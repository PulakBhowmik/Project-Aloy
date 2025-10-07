package com.example.project.aloy.repository;

import com.example.project.aloy.model.ApartmentImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApartmentImageRepository extends JpaRepository<ApartmentImage, Long> {
}