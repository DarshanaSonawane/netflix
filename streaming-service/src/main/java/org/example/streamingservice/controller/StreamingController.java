package org.example.streamingservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.streamingservice.service.StreamingService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/stream")
@RequiredArgsConstructor
public class StreamingController {

    private static final MediaType HLS_PLAYLIST = MediaType.parseMediaType("application/vnd.apple.mpegurl");

    private final StreamingService streamingService;

    // GET /api/stream/5/url
    @GetMapping("/{movieId}/url")
    public ResponseEntity<Map<String, String>> getStreamingUrl(@PathVariable Long movieId) {
        return ResponseEntity.ok(Map.of("streamUrl", streamingService.getStreamingUrl(movieId)));
    }

    // GET /api/stream/5/master.m3u8
    @GetMapping("/{movieId}/master.m3u8")
    public ResponseEntity<String> master(@PathVariable Long movieId) {
        return playlist(streamingService.getMasterPlaylist(movieId));
    }

    // GET /api/stream/5/720p/index.m3u8
    @GetMapping("/{movieId}/{quality}/index.m3u8")
    public ResponseEntity<String> quality(@PathVariable Long movieId, @PathVariable String quality) {
        return playlist(streamingService.getSignedPlaylist(movieId, quality));
    }

    private ResponseEntity<String> playlist(String body) {
        return ResponseEntity.ok()
                .contentType(HLS_PLAYLIST)
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)))
                .body(body);
    }
}