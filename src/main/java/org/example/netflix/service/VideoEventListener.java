package org.example.netflix.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.netflix.event.VideoEncodedEvent;
import org.example.netflix.event.VideoUploadedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoEventListener {

    private final ContentService contentService;

    @KafkaListener(topics = "${kafka.topic.video-uploaded}", containerFactory = "uploadedEventListenerContainerFactory")
    public void onVideoUploaded(VideoUploadedEvent event) {
        contentService.updateVideoKey(event.getMovieId(), event.getVideoKey());
    }

    @KafkaListener(topics = "${kafka.topic.video-encoded}", containerFactory = "encodedEventListenerContainerFactory")
    public void onVideoEncoded(VideoEncodedEvent event) {
        contentService.updateHlsUrl(event.getMovieId(), event.getHlsUrl());
    }
}
