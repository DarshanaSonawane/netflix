package org.example.netflix.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.netflix.Entity.Genre;
import org.example.netflix.Entity.Movie;
import org.example.netflix.Entity.VideoStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieResponse {

    private Long id;
    private String title;
    private String description;
    private Genre genre;
    private String director;
    private String cast;
    private Integer releaseYear;
    private Double rating;
    private String thumbnailUrl;
    private Integer durationMinutes;
    private String hlsUrl;
    private VideoStatus status;
    private LocalDateTime createdAt;

    // Converts a Movie entity into a response. videoKey is deliberately left out
    // because it is an internal S3 path that clients should not see.
    public static MovieResponse from(Movie movie) {
        return MovieResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .description(movie.getDescription())
                .genre(movie.getGenre())
                .director(movie.getDirector())
                .cast(movie.getCast())
                .releaseYear(movie.getReleaseYear())
                .rating(movie.getRating())
                .thumbnailUrl(movie.getThumbnailUrl())
                .durationMinutes(movie.getDurationMinutes())
                .hlsUrl(movie.getHlsUrl())
                .status(movie.getStatus())
                .createdAt(movie.getCreatedAt())
                .build();
    }
}