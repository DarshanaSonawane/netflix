package org.example.netflix.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.netflix.Dto.MovieRequest;
import org.example.netflix.Dto.MovieResponse;
import org.example.netflix.Entity.Genre;
import org.example.netflix.service.ContentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    // POST /api/movies
    @PostMapping
    public ResponseEntity<MovieResponse> addMovie(@Valid @RequestBody MovieRequest request) {
        MovieResponse created = contentService.addMovie(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // GET /api/movies?page=0&size=10
    @GetMapping
    public ResponseEntity<Page<MovieResponse>> getAllMovies(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(contentService.getAllMovies(pageable));
    }

    // GET /api/movies/5
    @GetMapping("/{id}")
    public ResponseEntity<MovieResponse> getMovieById(@PathVariable Long id) {
        return ResponseEntity.ok(contentService.getMovieById(id));
    }

    // GET /api/movies/genre/ACTION?page=0&size=10
    @GetMapping("/genre/{genre}")
    public ResponseEntity<Page<MovieResponse>> getMoviesByGenre(
            @PathVariable Genre genre,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(contentService.getMoviesByGenre(genre, pageable));
    }

    // GET /api/movies/search?keyword=incep&page=0&size=10
    @GetMapping("/search")
    public ResponseEntity<Page<MovieResponse>> searchMovies(
            @RequestParam String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(contentService.searchMovies(keyword, pageable));
    }
}