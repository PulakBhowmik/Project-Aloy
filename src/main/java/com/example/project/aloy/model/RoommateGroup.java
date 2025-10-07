package com.example.project.aloy.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "roommate_group")
public class RoommateGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long groupId;

    @ManyToOne
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

    @Column(nullable = false)
    private Long creatorId; // User who created the group

    @Column(unique = true, nullable = false, length = 6)
    private String inviteCode; // 6-character invite code (e.g., ABC123)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupStatus status = GroupStatus.FORMING;

    private LocalDateTime createdAt;

    private LocalDateTime bookedAt; // When payment was made

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<RoommateGroupMember> members = new ArrayList<>();

    // Getters and setters
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    
    public Apartment getApartment() { return apartment; }
    public void setApartment(Apartment apartment) { this.apartment = apartment; }
    
    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }
    
    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
    
    public GroupStatus getStatus() { return status; }
    public void setStatus(GroupStatus status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getBookedAt() { return bookedAt; }
    public void setBookedAt(LocalDateTime bookedAt) { this.bookedAt = bookedAt; }
    
    public List<RoommateGroupMember> getMembers() { return members; }
    public void setMembers(List<RoommateGroupMember> members) { this.members = members; }
    
    // Helper method to get member count
    public int getMemberCount() {
        return members != null ? members.size() : 0;
    }
    
    // Helper method to check if group is full
    public boolean isFull() {
        return getMemberCount() >= 4;
    }
}