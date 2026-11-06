package org.example.netflix.Dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.netflix.Entity.Genre;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must be at most 255 characters")
    private String title;

    @Size(max = 5000, message = "Description is too long")
    private String description;

    @NotNull(message = "Genre is required")
    private Genre genre;

    private String director;

    private String cast;

    @Min(value = 1888, message = "Release year is not valid")
    @Max(value = 2100, message = "Release year is not valid")
    private Integer releaseYear;

    @DecimalMin(value = "0.0", message = "Rating must be at least 0")
    @DecimalMax(value = "10.0", message = "Rating must be at most 10")
    private Double rating;

    private String thumbnailUrl;

    @Positive(message = "Duration must be greater than 0")
    private Integer durationMinutes;
}