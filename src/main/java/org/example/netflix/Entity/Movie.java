package org.example.netflix.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.netflix.Entity.VideoStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "movies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Genre genre;

    private String director;

    // "cast" can clash with the SQL CAST keyword, so the column gets a safe name
    @Column(name = "cast_members", length = 500)
    private String cast;

    private Integer releaseYear;

    private Double rating;

    private String thumbnailUrl;

    private Integer durationMinutes;

    // HLS playlist (.m3u8) location, filled in after encoding finishes
    private String hlsUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // S3 object key of the original uploaded video, e.g. videos/12/original.mp4
    private String videoKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private VideoStatus status = VideoStatus.PENDING;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}