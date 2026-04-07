package com.campus.resourcesharing.service;

import com.campus.resourcesharing.model.Rating;
import com.campus.resourcesharing.model.Request;
import com.campus.resourcesharing.model.User;
import com.campus.resourcesharing.repository.RatingRepository;
import com.campus.resourcesharing.repository.RequestRepository;
import com.campus.resourcesharing.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RatingService {
    
    @Autowired
    private RatingRepository ratingRepository;
    
    @Autowired
    private RequestRepository requestRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Transactional
    public Rating submitRating(Long requestId, Long raterId, Long ratedUserId, Integer score, String comment) {
        // Validate score
        if (score < 1 || score > 5) {
            throw new RuntimeException("Rating score must be between 1 and 5");
        }
        
        // Get the request
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        
        // Check if request is fully verified
        if (!request.isFullyVerified()) {
            throw new RuntimeException("Cannot rate until the transaction is fully verified");
        }
        
        // Get the rater and rated user
        User rater = userRepository.findById(raterId)
                .orElseThrow(() -> new RuntimeException("Rater not found"));
        
        User ratedUser = userRepository.findById(ratedUserId)
                .orElseThrow(() -> new RuntimeException("User to rate not found"));
        
        // Verify rater is part of the transaction
        boolean isOwner = request.getOwner().getId().equals(raterId);
        boolean isRequester = request.getRequester().getId().equals(raterId);
        
        if (!isOwner && !isRequester) {
            throw new RuntimeException("Only participants of this transaction can submit ratings");
        }
        
        // Verify rated user is the other party
        if (isOwner && !ratedUserId.equals(request.getRequester().getId())) {
            throw new RuntimeException("Owner can only rate the requester");
        }
        if (isRequester && !ratedUserId.equals(request.getOwner().getId())) {
            throw new RuntimeException("Requester can only rate the owner");
        }
        
        // Check if already rated
        if (ratingRepository.existsByRequestIdAndRaterId(requestId, raterId)) {
            throw new RuntimeException("You have already rated this transaction");
        }
        
        // Create rating
        Rating rating = new Rating(rater, ratedUser, request, score, comment);
        Rating savedRating = ratingRepository.save(rating);
        
        // Update user's average rating
        updateUserRating(ratedUserId);
        
        // Force initialization of lazy-loaded relationships
        savedRating.getRater().getFullName();
        savedRating.getRatedUser().getFullName();
        savedRating.getRequest().getId();
        savedRating.getRequest().getResource().getTitle();
        
        return savedRating;
    }
    
    @Transactional
    public void updateUserRating(Long userId) {
        Double avgRating = ratingRepository.calculateAverageRating(userId);
        Integer totalRatings = ratingRepository.countRatingsByUserId(userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setAverageRating(avgRating != null ? avgRating : 0.0);
        user.setTotalRatings(totalRatings != null ? totalRatings : 0);
        
        userRepository.save(user);
    }
    
    @Transactional(readOnly = true)
    public List<Rating> getRatingsForUser(Long userId) {
        List<Rating> ratings = ratingRepository.findByRatedUserIdOrderByCreatedAtDesc(userId);
        // Force initialization of lazy-loaded relationships
        for (Rating rating : ratings) {
            rating.getRater().getFullName();
            rating.getRatedUser().getFullName();
            rating.getRequest().getId();
            rating.getRequest().getResource().getTitle();
        }
        return ratings;
    }
    
    public List<Rating> getRatingsByUser(Long userId) {
        return ratingRepository.findByRaterIdOrderByCreatedAtDesc(userId);
    }
    
    public boolean hasUserRated(Long requestId, Long raterId) {
        return ratingRepository.existsByRequestIdAndRaterId(requestId, raterId);
    }
    
    public Rating getRatingForRequest(Long requestId, Long raterId) {
        return ratingRepository.findByRequestIdAndRaterId(requestId, raterId).orElse(null);
    }
}
