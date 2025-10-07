package com.example.project.aloy.model;

import jakarta.persistence.*;

@Entity
@Table(name = "roommate_group_members")
public class RoommateGroupMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long groupMemberId;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private RoommateGroup group;

    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false)
    private User tenant;

    // Getters and setters
    public Long getGroupMemberId() { return groupMemberId; }
    public void setGroupMemberId(Long groupMemberId) { this.groupMemberId = groupMemberId; }
    public RoommateGroup getGroup() { return group; }
    public void setGroup(RoommateGroup group) { this.group = group; }
    public User getTenant() { return tenant; }
    public void setTenant(User tenant) { this.tenant = tenant; }
}