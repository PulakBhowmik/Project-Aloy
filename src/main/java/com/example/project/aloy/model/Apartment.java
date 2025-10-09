package com.example.project.aloy.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "apartments")
public class Apartment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long apartmentId;

    // Title/Name of the apartment
    private String title;

    // Address parts per ER diagram
    private String houseNo;
    private String street;
    private String district;

    // Full address (optional, derived or stored)
    private String address;

    private String description;

    @Column(name = "monthly_rent")
    private BigDecimal monthlyRate;

    // 'solo', 'group', or 'both'
    private String allowedFor;

    // AVAILABLE or BOOKED (standardized statuses)
    private String status = "AVAILABLE";

    private LocalDate availability;

    private Long ownerId;

    private boolean booked = false;

    public Apartment() {}

    public Long getApartmentId() {
        return apartmentId;
    }

    public void setApartmentId(Long apartmentId) {
        this.apartmentId = apartmentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHouseNo() {
        return houseNo;
    }

    public void setHouseNo(String houseNo) {
        this.houseNo = houseNo;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getMonthlyRate() {
        return monthlyRate;
    }

    public void setMonthlyRate(BigDecimal monthlyRate) {
        this.monthlyRate = monthlyRate;
    }

    public String getAllowedFor() {
        return allowedFor;
    }

    public void setAllowedFor(String allowedFor) {
        // Validate that only 'solo', 'group', or 'both' are allowed
        if (allowedFor != null && !allowedFor.isEmpty()) {
            AllowedForType.fromString(allowedFor); // Will throw exception if invalid
        }
        this.allowedFor = allowedFor;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getAvailability() {
        return availability;
    }

    public void setAvailability(LocalDate availability) {
        this.availability = availability;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public boolean isBooked() {
        return booked;
    }

    public void setBooked(boolean booked) {
        this.booked = booked;
    }
}