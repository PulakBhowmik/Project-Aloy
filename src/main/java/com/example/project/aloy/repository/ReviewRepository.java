package com.example.project.aloy.repository;

import com.example.project.aloy.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    List<Review> findByApartmentIdOrderByDateDesc(Long apartmentId);
    
    boolean existsByUserIdAndApartmentId(Long userId, Long apartmentId);
    
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.apartmentId = :apartmentId")
    Double findAverageRatingByApartmentId(Long apartmentId);
    
    @Query("SELECT COUNT(r) FROM Review r WHERE r.apartmentId = :apartmentId")
    Long countByApartmentId(Long apartmentId);
}