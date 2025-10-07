package com.example.project.aloy.model;

public enum GroupStatus {
    FORMING,    // Group is being formed (less than 4 members)
    READY,      // Group has 4 members, ready for payment
    BOOKED,     // Payment made, apartment booked
    CANCELLED   // Group dissolved or apartment taken by another group
}
