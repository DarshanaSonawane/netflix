package org.example.netflix.service;

import lombok.RequiredArgsConstructor;
import org.example.netflix.Dto.MovieRequest;
import org.example.netflix.Dto.MovieResponse;
import org.example.netflix.Entity.Genre;
import org.example.netflix.Entity.Movie;
import org.example.netflix.Entity.VideoStatus;
import org.example.netflix.Repository.MovieRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ContentService {

    private final MovieRepository movieRepository;

    @Transactional
    public MovieResponse addMovie(MovieRequest request) {
        // status defaults to PENDING and createdAt is set automatically on save
        Movie movie = Movie.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .genre(request.getGenre())
                .director(request.getDirector())
                .cast(request.getCast())
                .releaseYear(request.getReleaseYear())
                .rating(request.getRating())
                .thumbnailUrl(request.getThumbnailUrl())
                .durationMinutes(request.getDurationMinutes())
                .build();

        return MovieResponse.from(movieRepository.save(movie));
    }

    @Transactional(readOnly = true)
    public Page<MovieResponse> getAllMovies(Pageable pageable) {
        return movieRepository.findAll(pageable).map(MovieResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<MovieResponse> getMoviesByGenre(Genre genre, Pageable pageable) {
        return movieRepository.findByGenre(genre, pageable).map(MovieResponse::from);
    }

    @Transactional(readOnly = true)
    public MovieResponse getMovieById(Long id) {
        return MovieResponse.from(findMovieOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<MovieResponse> searchMovies(String keyword, Pageable pageable) {
        return movieRepository.findByTitleContainingIgnoreCase(keyword, pageable)
                .map(MovieResponse::from);
    }

    // Called by the server after the original video is uploaded to S3
    @Transactional
    public MovieResponse updateVideoKey(Long id, String videoKey) {
        Movie movie = findMovieOrThrow(id);
        movie.setVideoKey(videoKey);
        movie.setStatus(VideoStatus.ENCODING);
        return MovieResponse.from(movieRepository.save(movie));
    }

    // Called by the server after HLS encoding finishes; the movie is now playable
    @Transactional
    public MovieResponse updateHlsUrl(Long id, String hlsUrl) {
        Movie movie = findMovieOrThrow(id);
        movie.setHlsUrl(hlsUrl);
        movie.setStatus(VideoStatus.READY);
        return MovieResponse.from(movieRepository.save(movie));
    }

    private Movie findMovieOrThrow(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Movie not found with id: " + id));
    }
}
