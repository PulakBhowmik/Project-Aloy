package com.example.project.aloy.model;


import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "reviews")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    private Integer rating; // 1-5 stars
    private String remarks;
    
    @Column(length = 500)
    private String goodSides;
    
    @Column(length = 500)
    private String badSides;
    
    private String tenantName; // Display name of reviewer
    
    private Long userId; // References User.userId
    private Long apartmentId; // References Apartment.apartmentId
    private LocalDate date;

    // Constructors
    public Review() {}

    // Getters and Setters
    public Long getReviewId() {
        return reviewId;
    }

    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getApartmentId() {
        return apartmentId;
    }

    public void setApartmentId(Long apartmentId) {
        this.apartmentId = apartmentId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getGoodSides() {
        return goodSides;
    }

    public void setGoodSides(String goodSides) {
        this.goodSides = goodSides;
    }

    public String getBadSides() {
        return badSides;
    }

    public void setBadSides(String badSides) {
        this.badSides = badSides;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }
}