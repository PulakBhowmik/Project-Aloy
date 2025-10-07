package com.example.project.aloy.repository;

import com.example.project.aloy.model.RoommateGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoommateGroupMemberRepository extends JpaRepository<RoommateGroupMember, Long> {
}