package com.example.project.aloy.service;

import com.example.project.aloy.model.*;
import com.example.project.aloy.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class RoommateGroupService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private RoommateGroupRepository groupRepository;

    @Autowired
    private RoommateGroupMemberRepository memberRepository;

    @Autowired
    private ApartmentRepository apartmentRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Create a new roommate group for an apartment
     */
    @Transactional
    public RoommateGroup createGroup(Long apartmentId, Long creatorId) {
        // Validate apartment exists and allows group booking
        Apartment apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new RuntimeException("Apartment not found"));

        if (!"group".equals(apartment.getAllowedFor()) && !"both".equals(apartment.getAllowedFor())) {
            throw new RuntimeException("This apartment does not allow group bookings");
        }

        if (apartment.isBooked()) {
            throw new RuntimeException("Apartment is already booked");
        }

        // Check if creator is a tenant
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!"TENANT".equalsIgnoreCase(creator.getRole())) {
            throw new RuntimeException("Only tenants can create groups");
        }

        // Check if tenant is already in an active group
        List<RoommateGroupMember> existingMemberships = memberRepository.findActiveMembershipsByTenantId(creatorId);
        if (!existingMemberships.isEmpty()) {
            throw new RuntimeException("You are already in an active group");
        }

        // Create group
        RoommateGroup group = new RoommateGroup();
        group.setApartment(apartment);
        group.setCreatorId(creatorId);
        group.setInviteCode(generateInviteCode());
        group.setStatus(GroupStatus.FORMING);
        group.setCreatedAt(LocalDateTime.now());

        group = groupRepository.save(group);

        // Add creator as first member
        RoommateGroupMember member = new RoommateGroupMember();
        member.setGroup(group);
        member.setTenant(creator);
        member.setJoinedAt(LocalDateTime.now());
        memberRepository.save(member);

        return group;
    }

    /**
     * Join an existing group using invite code
     */
    @Transactional
    public RoommateGroup joinGroup(String inviteCode, Long tenantId) {
        // Find group by invite code
        RoommateGroup group = groupRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new RuntimeException("Invalid invite code"));

        // Check if group is still forming
        if (group.getStatus() != GroupStatus.FORMING) {
            throw new RuntimeException("This group is no longer accepting members");
        }

        // Check if group is full
        if (group.isFull()) {
            throw new RuntimeException("This group is already full (4/4 members)");
        }

        // Check if tenant exists
        User tenant = userRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!"TENANT".equalsIgnoreCase(tenant.getRole())) {
            throw new RuntimeException("Only tenants can join groups");
        }

        // Check if tenant is already in this group
        Optional<RoommateGroupMember> existing = memberRepository.findByGroup_GroupIdAndTenant_UserId(group.getGroupId(), tenantId);
        if (existing.isPresent()) {
            throw new RuntimeException("You are already in this group");
        }

        // Check if tenant is in any other active group
        List<RoommateGroupMember> existingMemberships = memberRepository.findActiveMembershipsByTenantId(tenantId);
        if (!existingMemberships.isEmpty()) {
            throw new RuntimeException("You are already in another active group");
        }

        // Add member to group
        RoommateGroupMember member = new RoommateGroupMember();
        member.setGroup(group);
        member.setTenant(tenant);
        member.setJoinedAt(LocalDateTime.now());
        memberRepository.save(member);
        memberRepository.flush(); // Ensure the save is committed

        // Check if group is now full (4 members) - refetch to get updated member list
        group = groupRepository.findById(group.getGroupId()).get();
        int currentMemberCount = memberRepository.findByGroup_GroupId(group.getGroupId()).size();
        
        if (currentMemberCount >= 4) {
            group.setStatus(GroupStatus.READY);
            groupRepository.save(group);
            groupRepository.flush(); // Ensure status update is committed
        }

        return group;
    }

    /**
     * Leave a group (only if not yet booked)
     */
    @Transactional
    public void leaveGroup(Long groupId, Long tenantId) {
        // Fetch group with fresh data
        RoommateGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (group.getStatus() == GroupStatus.BOOKED) {
            throw new RuntimeException("Cannot leave a group that has already booked the apartment");
        }

        // Check if member exists
        RoommateGroupMember member = memberRepository.findByGroup_GroupIdAndTenant_UserId(groupId, tenantId)
                .orElseThrow(() -> new RuntimeException("You are not a member of this group"));

        // Remove member from group's member list (for JPA cascade)
        group.getMembers().remove(member);
        
        // Delete the member entity
        memberRepository.delete(member);
        
        // Save group changes and flush to database
        groupRepository.save(group);
        groupRepository.flush();
        
        // Clear EntityManager to force fresh queries
        entityManager.clear();

        // Count remaining members by querying database directly
        List<RoommateGroupMember> remainingMembersList = memberRepository.findByGroup_GroupId(groupId);
        int remainingMembers = remainingMembersList.size();

        if (remainingMembers == 0) {
            // No members left, delete the group entirely
            groupRepository.deleteById(groupId);
            groupRepository.flush();
        } else {
            // Re-fetch group for status update
            RoommateGroup freshGroup = groupRepository.findById(groupId)
                    .orElse(null);
            
            if (freshGroup != null && freshGroup.getStatus() == GroupStatus.READY && remainingMembers < 4) {
                // Group was ready but now not full, set back to forming
                freshGroup.setStatus(GroupStatus.FORMING);
                groupRepository.save(freshGroup);
                groupRepository.flush();
            }
        }
        
        // Final cache clear to ensure next API call gets fresh data
        entityManager.clear();
    }

    /**
     * Book apartment (payment) - only if group has 4 members
     */
    @Transactional
    public RoommateGroup bookApartment(Long groupId, Long payingTenantId) {
        RoommateGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Verify paying tenant is in the group
        memberRepository.findByGroup_GroupIdAndTenant_UserId(groupId, payingTenantId)
                .orElseThrow(() -> new RuntimeException("You are not a member of this group"));

        // Check if group has exactly 4 members
        if (!group.isFull()) {
            throw new RuntimeException("Group must have exactly 4 members to book apartment");
        }

        // Check if group status is READY
        if (group.getStatus() != GroupStatus.READY) {
            throw new RuntimeException("Group is not ready for booking");
        }

        // Check if apartment is still available
        Apartment apartment = group.getApartment();
        if (apartment.isBooked()) {
            // Another group booked it first
            group.setStatus(GroupStatus.CANCELLED);
            groupRepository.save(group);
            throw new RuntimeException("Apartment has already been booked by another group");
        }

        // Mark apartment as booked
        apartment.setBooked(true);
        apartment.setStatus("RENTED");
        apartmentRepository.save(apartment);

        // Update group status
        group.setStatus(GroupStatus.BOOKED);
        group.setBookedAt(LocalDateTime.now());
        groupRepository.save(group);

        // Cancel other groups for this apartment
        List<RoommateGroup> otherGroups = groupRepository.findByApartment_ApartmentId(apartment.getApartmentId());
        for (RoommateGroup otherGroup : otherGroups) {
            if (!otherGroup.getGroupId().equals(groupId) && 
                (otherGroup.getStatus() == GroupStatus.FORMING || otherGroup.getStatus() == GroupStatus.READY)) {
                otherGroup.setStatus(GroupStatus.CANCELLED);
                groupRepository.save(otherGroup);
            }
        }

        return group;
    }

    /**
     * Get all forming groups for an apartment
     */
    public List<RoommateGroup> getFormingGroups(Long apartmentId) {
        return groupRepository.findByApartment_ApartmentIdAndStatus(apartmentId, GroupStatus.FORMING);
    }

    /**
     * Get all ready groups for an apartment
     */
    public List<RoommateGroup> getReadyGroups(Long apartmentId) {
        return groupRepository.findByApartment_ApartmentIdAndStatus(apartmentId, GroupStatus.READY);
    }

    /**
     * Get group by invite code
     */
    public Optional<RoommateGroup> getGroupByInviteCode(String inviteCode) {
        return groupRepository.findByInviteCode(inviteCode);
    }

    /**
     * Get group by ID
     */
    public Optional<RoommateGroup> getGroupById(Long groupId) {
        return groupRepository.findById(groupId);
    }

    /**
     * Get tenant's current group status (if in any active group)
     */
    public java.util.Map<String, Object> getTenantGroupStatus(Long tenantId) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        
        // Check if tenant is in any active group (FORMING or READY)
        List<RoommateGroupMember> memberships = memberRepository.findActiveMembershipsByTenantId(tenantId);
        
        if (memberships.isEmpty()) {
            response.put("inGroup", false);
            return response;
        }
        
        // Get the group details
        RoommateGroupMember membership = memberships.get(0);
        RoommateGroup group = membership.getGroup();
        
        // Count members
        int memberCount = memberRepository.findByGroup_GroupId(group.getGroupId()).size();
        
        response.put("inGroup", true);
        response.put("groupId", group.getGroupId());
        response.put("apartmentTitle", group.getApartment().getTitle());
        response.put("apartmentId", group.getApartment().getApartmentId());
        response.put("inviteCode", group.getInviteCode());
        response.put("status", group.getStatus().toString());
        response.put("memberCount", memberCount);
        response.put("maxMembers", 4);
        response.put("isFull", memberCount >= 4);
        response.put("isCreator", group.getCreatorId().equals(tenantId));
        response.put("createdAt", group.getCreatedAt());
        
        // Get member names
        List<RoommateGroupMember> allMembers = memberRepository.findByGroup_GroupId(group.getGroupId());
        List<String> memberNames = allMembers.stream()
                .map(m -> m.getTenant().getName())
                .toList();
        response.put("memberNames", memberNames);
        
        return response;
    }

    /**
     * Generate a unique 6-character invite code
     */
    private String generateInviteCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        String code;
        
        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            code = sb.toString();
        } while (groupRepository.findByInviteCode(code).isPresent());
        
        return code;
    }
}
