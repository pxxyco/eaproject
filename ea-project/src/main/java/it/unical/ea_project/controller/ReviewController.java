package it.unical.ea_project.controller;

import it.unical.ea_project.dto.ReviewDTO;
import it.unical.ea_project.security.AuthUser;
import it.unical.ea_project.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/mine")
    public ResponseEntity<List<ReviewDTO>> getMyReviews(@AuthenticationPrincipal AuthUser user) {
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(reviewService.findByUserAsDto(user.id()));
    }

    @PostMapping
    public ResponseEntity<ReviewDTO> createReview(@RequestBody ReviewDTO review,
                                                  @AuthenticationPrincipal AuthUser user){
        if (user == null) return ResponseEntity.status(401).build();
        review.setUserId(user.id());
        return ResponseEntity.ok(reviewService.create(review));
    }

    @GetMapping("/trip/{tripId}")
    public ResponseEntity<List<ReviewDTO>> getReviewsByTrip(@PathVariable Long tripId) {
        return ResponseEntity.ok(reviewService.findByTrip(tripId));
    }

    @GetMapping("/activity/{activityId}")
    public ResponseEntity<List<ReviewDTO>> getReviewsByActivity(@PathVariable Long activityId) {
        return ResponseEntity.ok(reviewService.findByActivity(activityId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReviewDTO> updateReview(@PathVariable Long id,
                                                  @RequestBody ReviewDTO review,
                                                  @AuthenticationPrincipal AuthUser user){
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(reviewService.update(id, review, user.id()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id, @AuthenticationPrincipal AuthUser user){
        if (user == null) return ResponseEntity.status(401).build();
        reviewService.delete(id, user.id());
        return ResponseEntity.noContent().build();
    }

}
