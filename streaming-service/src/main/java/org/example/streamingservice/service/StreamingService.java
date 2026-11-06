package org.example.streamingservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StreamingService {

    private static final Pattern QUALITY = Pattern.compile("^(360p|720p|1080p)$");
    private static final Duration URL_TTL = Duration.ofHours(6);    // how long presigned URLs work
    private static final Duration CACHE_TTL = Duration.ofHours(5);   // must be shorter than URL_TTL

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final StringRedisTemplate redis;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${streaming.base-url}")
    private String baseUrl;

    public String getStreamingUrl(Long movieId) {
        return baseUrl.replaceAll("/$", "") + "/api/stream/" + movieId + "/master.m3u8";
    }

    public String getMasterPlaylist(Long movieId) {
        return master(movieId);
    }

    public String getSignedPlaylist(Long movieId, String quality) {
        return rendition(movieId, quality);
    }

    public void warmCache(Long movieId) {
        String playlist = master(movieId);
        Matcher matcher = Pattern.compile("(?m)^\\s*(360p|720p|1080p)/index\\.m3u8\\s*$").matcher(playlist);
        while (matcher.find()) {
            rendition(movieId, matcher.group(1));
        }
    }

    // master playlist: variant lines are relative, so no presigning is needed
    public String master(Long movieId) {
        String cacheKey = "stream:" + movieId + ":master";
        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        String playlist = readText(prefix(movieId) + "master.m3u8");
        redis.opsForValue().set(cacheKey, playlist, CACHE_TTL);
        return playlist;
    }

    // rendition playlist: every segment line becomes a presigned S3 URL
    public String rendition(Long movieId, String quality) {
        if (!QUALITY.matcher(quality).matches()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
        }
        String cacheKey = "stream:" + movieId + ":" + quality;

        // 1. cached? return immediately
        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 2. not cached: read from S3 and presign every segment
        String keyPrefix = prefix(movieId) + quality + "/";
        String original = readText(keyPrefix + "index.m3u8");
        String rewritten = original.lines()
                .map(line -> line.trim().endsWith(".ts")
                        ? presign(keyPrefix + line.trim())
                        : line)
                .collect(Collectors.joining("\n")) + "\n";

        // 3. cache it
        redis.opsForValue().set(cacheKey, rewritten, CACHE_TTL);
        return rewritten;
    }

    // call this if a movie is ever re-encoded
    public void evict(Long movieId) {
        redis.delete("stream:" + movieId + ":master");
        for (String q : new String[]{"360p", "720p", "1080p"}) {
            redis.delete("stream:" + movieId + ":" + q);
        }
    }

    private String presign(String key) {
        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                .signatureDuration(URL_TTL)
                .getObjectRequest(g -> g.bucket(bucketName).key(key))
                .build();
        return presigner.presignGetObject(request).url().toString();
    }

    private String readText(String key) {
        try {
            return s3Client.getObjectAsBytes(
                            GetObjectRequest.builder().bucket(bucketName).key(key).build())
                    .asUtf8String();
        } catch (NoSuchKeyException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found or not ready");
        }
    }

    private String prefix(Long movieId) {
        return "videos/" + movieId + "/hls/";
    }
}
