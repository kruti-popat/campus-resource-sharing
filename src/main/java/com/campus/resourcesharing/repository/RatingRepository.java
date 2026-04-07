package com.campus.resourcesharing.repository;

import com.campus.resourcesharing.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    
    List<Rating> findByRatedUserIdOrderByCreatedAtDesc(Long ratedUserId);
    
    List<Rating> findByRaterIdOrderByCreatedAtDesc(Long raterId);
    
    Optional<Rating> findByRequestIdAndRaterId(Long requestId, Long raterId);
    
    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.ratedUser.id = :userId")
    Double calculateAverageRating(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(r) FROM Rating r WHERE r.ratedUser.id = :userId")
    Integer countRatingsByUserId(@Param("userId") Long userId);
    
    boolean existsByRequestIdAndRaterId(Long requestId, Long raterId);
}
