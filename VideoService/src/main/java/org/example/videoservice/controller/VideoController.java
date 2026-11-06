package org.example.videoservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.videoservice.Service.VideoService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    // POST /api/videos/5/upload  (form-data, key = file)
    @PostMapping(value = "/{movieId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> upload(
            @PathVariable Long movieId,
            @RequestParam("file") MultipartFile file) {
        videoService.uploadVideo(movieId, file);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
