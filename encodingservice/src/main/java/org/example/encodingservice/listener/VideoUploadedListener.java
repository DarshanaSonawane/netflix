package org.example.encodingservice.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.encodingservice.event.VideoUploadedEvent;
import org.example.encodingservice.service.EncodingService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoUploadedListener {

    private final EncodingService encodingService;

    @KafkaListener(topics = "${kafka.topic.video-uploaded}", groupId = "encoding-service")
    public void onVideoUploaded(VideoUploadedEvent event) {
        log.info("Encoding movie {} from {}", event.getMovieId(), event.getVideoKey());
        try {
            encodingService.encode(event);
        } catch (Exception e) {
            log.error("Encoding failed for movie {}", event.getMovieId(), e);
        }
    }
}