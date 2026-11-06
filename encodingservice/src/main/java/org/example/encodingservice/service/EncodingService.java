package org.example.encodingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.encodingservice.event.VideoEncodedEvent;
import org.example.encodingservice.event.VideoUploadedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class EncodingService {

    private record Quality(String name, int height, int videoKbps, int audioKbps) {}

    private record Rendition(Quality quality, int width) {}

    private static final List<Quality> QUALITIES = List.of(
            new Quality("360p", 360, 800, 96),
            new Quality("720p", 720, 2800, 128),
            new Quality("1080p", 1080, 5000, 192));

    private final S3Client s3Client;
    private final KafkaTemplate<String, VideoEncodedEvent> kafkaTemplate;

    @Value("${aws.region}")
    private String region;

    @Value("${kafka.topic.video-encoded}")
    private String encodedTopic;

    public void encode(VideoUploadedEvent event) throws IOException, InterruptedException {
        Path workDir = Files.createTempDirectory("encode-" + event.getMovieId() + "-");
        try {
            // 1. download the raw video from S3
            Path input = downloadOriginal(event.getBucketName(), event.getVideoKey(), workDir);

            // 2. find the source size, so we never upscale
            int[] size = probeSize(input);
            int srcWidth = size[0];
            int srcHeight = size[1];

            // 3. encode each quality into its own HLS folder
            Path outDir = Files.createDirectory(workDir.resolve("hls"));
            List<Rendition> renditions = new ArrayList<>();
            for (Quality q : QUALITIES) {
                if (q.height() > srcHeight && !renditions.isEmpty()) {
                    continue; // skip qualities larger than the source (always keep 360p)
                }
                int width = evenWidth(q.height(), srcWidth, srcHeight);
                encodeRendition(input, outDir.resolve(q.name()), q, width);
                renditions.add(new Rendition(q, width));
                log.info("Movie {}: finished {}", event.getMovieId(), q.name());
            }

            // 4. write the master playlist
            writeMasterPlaylist(outDir.resolve("master.m3u8"), renditions);

            // 5. upload everything to S3
            String hlsPrefix = "videos/" + event.getMovieId() + "/hls/";
            uploadDirectoryToS3(outDir, event.getBucketName(), hlsPrefix);

            // 6. publish the event; hlsUrl points to the master playlist
            String hlsUrl = "https://" + event.getBucketName() + ".s3." + region
                    + ".amazonaws.com/" + hlsPrefix + "master.m3u8";
            publishEncoded(event.getMovieId(), hlsUrl);
        } finally {
            deleteRecursively(workDir);
        }
    }

    // ---------- download ----------

    private Path downloadOriginal(String bucket, String key, Path workDir) {
        Path input = workDir.resolve("input");
        s3Client.getObject(
                GetObjectRequest.builder().bucket(bucket).key(key).build(),
                input);
        return input;
    }

    // ---------- ffprobe / ffmpeg ----------

    // returns {width, height} of the first video stream
    private int[] probeSize(Path input) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "ffprobe", "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=width,height",
                "-of", "csv=p=0",
                input.toString());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (process.waitFor() != 0) {
            throw new IOException("ffprobe failed: " + out);
        }
        String[] parts = out.split(",");
        return new int[]{Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())};
    }

    // keeps the aspect ratio; width must be even for h264
    private int evenWidth(int targetHeight, int srcWidth, int srcHeight) {
        double w = targetHeight * (double) srcWidth / srcHeight;
        return (int) (Math.round(w / 2.0) * 2);
    }

    private void encodeRendition(Path input, Path dir, Quality q, int width)
            throws IOException, InterruptedException {
        Files.createDirectories(dir);

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-y",
                "-i", input.toString(),
                "-map", "0:v:0",
                "-map", "0:a:0?",
                "-vf", "scale=" + width + ":" + q.height(),
                "-c:v", "libx264",
                "-preset", "veryfast",
                "-b:v", q.videoKbps() + "k",
                "-maxrate", q.videoKbps() + "k",
                "-bufsize", (q.videoKbps() * 2) + "k",
                "-force_key_frames", "expr:gte(t,n_forced*6)",
                "-c:a", "aac",
                "-b:a", q.audioKbps() + "k",
                "-f", "hls",
                "-hls_time", "6",
                "-hls_playlist_type", "vod",
                "-hls_segment_filename", dir.resolve("seg_%03d.ts").toString(),
                dir.resolve("index.m3u8").toString());
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);

        int exit = pb.start().waitFor();
        if (exit != 0) {
            throw new IOException("ffmpeg failed for " + q.name() + " with exit code " + exit);
        }
    }

    // ---------- master playlist ----------

    private void writeMasterPlaylist(Path file, List<Rendition> renditions) throws IOException {
        StringBuilder sb = new StringBuilder("#EXTM3U\n#EXT-X-VERSION:3\n");
        for (Rendition r : renditions) {
            Quality q = r.quality();
            int bandwidth = (q.videoKbps() + q.audioKbps()) * 1000;
            sb.append("#EXT-X-STREAM-INF:BANDWIDTH=").append(bandwidth)
                    .append(",RESOLUTION=").append(r.width()).append("x").append(q.height())
                    .append("\n")
                    .append(q.name()).append("/index.m3u8\n");
        }
        Files.writeString(file, sb.toString());
    }

    // ---------- upload ----------

    // Uploads every file under a local directory (including subfolders) to S3
    private void uploadDirectoryToS3(Path dir, String bucket, String prefix) throws IOException {
        List<Path> files;
        try (Stream<Path> s = Files.walk(dir)) {
            files = s.filter(Files::isRegularFile).toList();
        }
        for (Path file : files) {
            String relative = dir.relativize(file).toString().replace('\\', '/');
            uploadFileToS3(file, bucket, prefix + relative);
        }
    }

    private void uploadFileToS3(Path file, String bucket, String key) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentTypeOf(file.getFileName().toString()))
                        .build(),
                RequestBody.fromFile(file));
    }

    private String contentTypeOf(String name) {
        if (name.endsWith(".m3u8")) return "application/vnd.apple.mpegurl";
        if (name.endsWith(".ts")) return "video/mp2t";
        return "application/octet-stream";
    }

    // ---------- kafka ----------

    private void publishEncoded(Long movieId, String hlsUrl) {
        kafkaTemplate.send(encodedTopic, String.valueOf(movieId),
                new VideoEncodedEvent(movieId, hlsUrl));
        log.info("Encoded movie {} -> {}", movieId, hlsUrl);
    }

    // ---------- cleanup ----------

    private void deleteRecursively(Path dir) {
        try (Stream<Path> s = Files.walk(dir)) {
            s.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    log.warn("Could not delete {}: {}", p, e.getMessage());
                }
            });
        } catch (IOException e) {
            log.warn("Could not clean temp dir {}", dir, e);
        }
    }
}