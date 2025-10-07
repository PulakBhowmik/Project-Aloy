package com.example.project.aloy.model;

public enum AllowedForType {
    SOLO("solo"),
    GROUP("group"),
    BOTH("both");

    private final String value;

    AllowedForType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static AllowedForType fromString(String text) {
        for (AllowedForType type : AllowedForType.values()) {
            if (type.value.equalsIgnoreCase(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid allowedFor value: " + text + ". Must be solo, group, or both.");
    }

    @Override
    public String toString() {
        return this.value;
    }
}
