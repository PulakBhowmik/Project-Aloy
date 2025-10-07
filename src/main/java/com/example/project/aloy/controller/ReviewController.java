package com.example.project.aloy.controller;

import com.example.project.aloy.model.Review;
import com.example.project.aloy.model.User;
import com.example.project.aloy.repository.ReviewRepository;
import com.example.project.aloy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    
    @Autowired
    private ReviewRepository reviewRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    // Get all reviews for an apartment
    @GetMapping("/apartment/{apartmentId}")
    public ResponseEntity<?> getApartmentReviews(@PathVariable Long apartmentId) {
        try {
            List<Review> reviews = reviewRepository.findByApartmentIdOrderByDateDesc(apartmentId);
            Double avgRating = reviewRepository.findAverageRatingByApartmentId(apartmentId);
            Long totalReviews = reviewRepository.countByApartmentId(apartmentId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("reviews", reviews);
            response.put("averageRating", avgRating != null ? avgRating : 0.0);
            response.put("totalReviews", totalReviews);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // Submit a review
    @PostMapping("/submit")
    public ResponseEntity<?> submitReview(@RequestBody Map<String, Object> reviewData) {
        try {
            Long userId = Long.valueOf(reviewData.get("userId").toString());
            Long apartmentId = Long.valueOf(reviewData.get("apartmentId").toString());
            Integer rating = Integer.valueOf(reviewData.get("rating").toString());
            String goodSides = reviewData.get("goodSides").toString();
            String badSides = reviewData.get("badSides").toString();
            
            // Check if user already reviewed this apartment
            if (reviewRepository.existsByUserIdAndApartmentId(userId, apartmentId)) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "You have already submitted a review for this apartment"));
            }
            
            // Get tenant name
            User tenant = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Create review
            Review review = new Review();
            review.setUserId(userId);
            review.setApartmentId(apartmentId);
            review.setRating(rating);
            review.setGoodSides(goodSides);
            review.setBadSides(badSides);
            review.setTenantName(tenant.getName());
            review.setDate(LocalDate.now());
            
            reviewRepository.save(review);
            
            return ResponseEntity.ok(Map.of(
                "message", "Review submitted successfully!",
                "review", review
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // Check if user can review (has vacated the apartment)
    @GetMapping("/can-review/{userId}/{apartmentId}")
    public ResponseEntity<?> canUserReview(@PathVariable Long userId, @PathVariable Long apartmentId) {
        try {
            boolean alreadyReviewed = reviewRepository.existsByUserIdAndApartmentId(userId, apartmentId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("canReview", !alreadyReviewed);
            response.put("alreadyReviewed", alreadyReviewed);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
