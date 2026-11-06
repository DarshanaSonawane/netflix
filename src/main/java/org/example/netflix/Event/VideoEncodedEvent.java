package org.example.netflix.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VideoEncodedEvent {
    private Long movieId;
    private String hlsUrl;
}
