package com.example.project.aloy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "roommate_group_members")
public class RoommateGroupMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long groupMemberId;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    @JsonIgnore  // Prevent circular reference when serializing
    private RoommateGroup group;

    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false)
    private User tenant;

    private LocalDateTime joinedAt;

    // Getters and setters
    public Long getGroupMemberId() { return groupMemberId; }
    public void setGroupMemberId(Long groupMemberId) { this.groupMemberId = groupMemberId; }
    
    public RoommateGroup getGroup() { return group; }
    public void setGroup(RoommateGroup group) { this.group = group; }
    
    public User getTenant() { return tenant; }
    public void setTenant(User tenant) { this.tenant = tenant; }
    
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
}