package com.example.project.aloy.repository;

import com.example.project.aloy.model.RoommateGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoommateGroupRepository extends JpaRepository<RoommateGroup, Long> {
}