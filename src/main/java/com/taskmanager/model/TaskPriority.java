package com.taskmanager.model;

/**
 * TaskPriority Enum
 * Demonstrates Java Enum types with custom properties, constructors, and methods.
 * Used to categorize the urgency and importance of a task.
 */
public enum TaskPriority {
    LOW("Low", "#2ecc71"),       // Green badge
    MEDIUM("Medium", "#f39c12"), // Amber/Orange badge
    HIGH("High", "#e74c3c");     // Red badge

    private final String displayName;
    private final String colorHex;

    TaskPriority(String displayName, String colorHex) {
        this.displayName = displayName;
        this.colorHex = colorHex;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorHex() {
        return colorHex;
    }

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Case-insensitive factory parser demonstrating robust error handling with fallback.
     */
    public static TaskPriority fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return MEDIUM;
        }
        for (TaskPriority p : values()) {
            if (p.name().equalsIgnoreCase(value.trim()) || p.displayName.equalsIgnoreCase(value.trim())) {
                return p;
            }
        }
        return MEDIUM;
    }
}
