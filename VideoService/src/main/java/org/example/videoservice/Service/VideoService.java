package org.example.videoservice.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.videoservice.event.VideoUploadedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {

    private final S3Client s3Client;
    private final KafkaTemplate<String, VideoUploadedEvent> kafkaTemplate;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${kafka.topic.video-uploaded}")
    private String topic;

    public void uploadVideo(Long movieId, MultipartFile file) {
        if (movieId == null || movieId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Movie id must be positive");
        }
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must be a video");
        }

        String originalFileName = file.getOriginalFilename() == null ? "video.mp4" : file.getOriginalFilename();
        String videoKey = "videos/" + movieId + "/original/" + UUID.randomUUID() + extensionOf(originalFileName);

        try (InputStream in = file.getInputStream()) {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(videoKey)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromInputStream(in, file.getSize()));
        } catch (IOException | RuntimeException e) {
            log.error("S3 upload failed for movie {}", movieId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Video upload failed");
        }

        VideoUploadedEvent event = new VideoUploadedEvent(
                movieId, videoKey, bucketName, originalFileName, file.getSize());

        // movieId as key keeps all events for one movie in order on the same partition
        try {
            kafkaTemplate.send(topic, String.valueOf(movieId), event).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while publishing upload event for movie {}", movieId, e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Video upload event could not be confirmed");
        } catch (ExecutionException | RuntimeException e) {
            log.error("Failed to publish upload event for movie {}", movieId, e);
            removeOrphanedUpload(movieId, videoKey);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Video upload event could not be published");
        }
    }

    private void removeOrphanedUpload(Long movieId, String videoKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(videoKey).build());
        } catch (RuntimeException cleanupError) {
            log.error("Failed to remove orphaned S3 upload for movie {}", movieId, cleanupError);
        }
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot) : ".mp4";
    }
}
