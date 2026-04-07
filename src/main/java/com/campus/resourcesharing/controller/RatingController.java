package com.campus.resourcesharing.controller;

import com.campus.resourcesharing.dto.ApiResponse;
import com.campus.resourcesharing.model.Rating;
import com.campus.resourcesharing.service.RatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ratings")
@CrossOrigin(origins = "*")
public class RatingController {
    
    @Autowired
    private RatingService ratingService;
    
    @PostMapping
    public ResponseEntity<ApiResponse> submitRating(
            @RequestParam Long requestId,
            @RequestParam Long raterId,
            @RequestParam Long ratedUserId,
            @RequestParam Integer score,
            @RequestParam(required = false) String comment) {
        try {
            Rating rating = ratingService.submitRating(requestId, raterId, ratedUserId, score, 
                    comment != null ? comment : "");
            return ResponseEntity.ok(new ApiResponse(true, "Rating submitted successfully", 
                    convertToMap(rating)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse> getRatingsForUser(@PathVariable Long userId) {
        try {
            List<Rating> ratings = ratingService.getRatingsForUser(userId);
            List<Map<String, Object>> ratingList = ratings.stream()
                    .map(this::convertToMap)
                    .toList();
            return ResponseEntity.ok(new ApiResponse(true, "Ratings retrieved successfully", ratingList));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }
    
    @GetMapping("/check")
    public ResponseEntity<ApiResponse> checkIfRated(
            @RequestParam Long requestId,
            @RequestParam Long raterId) {
        try {
            boolean hasRated = ratingService.hasUserRated(requestId, raterId);
            Map<String, Object> result = new HashMap<>();
            result.put("hasRated", hasRated);
            return ResponseEntity.ok(new ApiResponse(true, "Check completed", result));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }
    
    private Map<String, Object> convertToMap(Rating rating) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", rating.getId());
        map.put("score", rating.getScore());
        map.put("comment", rating.getComment());
        map.put("createdAt", rating.getCreatedAt());
        
        Map<String, Object> raterMap = new HashMap<>();
        raterMap.put("id", rating.getRater().getId());
        raterMap.put("username", rating.getRater().getUsername());
        raterMap.put("fullName", rating.getRater().getFullName());
        map.put("rater", raterMap);
        
        Map<String, Object> ratedUserMap = new HashMap<>();
        ratedUserMap.put("id", rating.getRatedUser().getId());
        ratedUserMap.put("username", rating.getRatedUser().getUsername());
        ratedUserMap.put("fullName", rating.getRatedUser().getFullName());
        map.put("ratedUser", ratedUserMap);
        
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("id", rating.getRequest().getId());
        requestMap.put("resourceTitle", rating.getRequest().getResource().getTitle());
        map.put("request", requestMap);
        
        return map;
    }
}
