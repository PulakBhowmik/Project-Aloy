package com.example.project.aloy.repository;

import com.example.project.aloy.model.GroupStatus;
import com.example.project.aloy.model.RoommateGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoommateGroupRepository extends JpaRepository<RoommateGroup, Long> {
    // Find group by invite code
    Optional<RoommateGroup> findByInviteCode(String inviteCode);
    
    // Find all groups for a specific apartment with given status
    List<RoommateGroup> findByApartment_ApartmentIdAndStatus(Long apartmentId, GroupStatus status);
    
    // Find all groups for an apartment (regardless of status)
    List<RoommateGroup> findByApartment_ApartmentId(Long apartmentId);
}