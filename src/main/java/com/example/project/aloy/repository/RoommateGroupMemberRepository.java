package com.example.project.aloy.repository;

import com.example.project.aloy.model.RoommateGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoommateGroupMemberRepository extends JpaRepository<RoommateGroupMember, Long> {
    // Check if a tenant is already in a specific group
    Optional<RoommateGroupMember> findByGroup_GroupIdAndTenant_UserId(Long groupId, Long tenantId);
    
    // Find all members of a group
    List<RoommateGroupMember> findByGroup_GroupId(Long groupId);
    
    // Check if tenant is in any FORMING or READY group
    @Query("SELECT m FROM RoommateGroupMember m WHERE m.tenant.userId = :tenantId " +
           "AND m.group.status IN ('FORMING', 'READY')")
    List<RoommateGroupMember> findActiveMembershipsByTenantId(@Param("tenantId") Long tenantId);
    
    // Delete member from group
    void deleteByGroup_GroupIdAndTenant_UserId(Long groupId, Long tenantId);
}