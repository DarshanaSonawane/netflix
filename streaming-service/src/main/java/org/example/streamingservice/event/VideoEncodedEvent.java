package org.example.streamingservice.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class VideoEncodedEvent {
    private Long movieId;
    private String hlsUrl;
}