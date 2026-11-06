package org.example.streamingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.streamingservice.event.VideoEncodedEvent;
import org.example.streamingservice.service.StreamingService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoEncodedEventConsumer {

    private final StreamingService streamingService;

    @KafkaListener(topics = "${kafka.topic.video-encoded}", groupId = "streaming-service")
    public void onVideoEncoded(VideoEncodedEvent event) {
        log.info("Video encoded for movie {}, warming the playlist cache", event.getMovieId());
        try {
            streamingService.warmCache(event.getMovieId());
        } catch (Exception e) {
            // not fatal: the cache fills on demand when the first viewer asks
            log.warn("Cache warm-up failed for movie {}", event.getMovieId(), e);
        }
    }
}