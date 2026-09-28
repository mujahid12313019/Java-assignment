package com.taskmanager.model;

/**
 * TaskStatus Enum
 * Represents the lifecycle state of a task.
 */
public enum TaskStatus {
    PENDING("Pending", "#95a5a6"),         // Gray badge
    IN_PROGRESS("In Progress", "#3498db"), // Blue badge
    COMPLETED("Completed", "#27ae60");     // Emerald Green badge

    private final String displayName;
    private final String colorHex;

    TaskStatus(String displayName, String colorHex) {
        this.displayName = displayName;
        this.colorHex = colorHex;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorHex() {
        return colorHex;
    }

    public boolean isTerminal() {
        return this == COMPLETED;
    }

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Case-insensitive factory parser with safe default fallback.
     */
    public static TaskStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return PENDING;
        }
        for (TaskStatus s : values()) {
            if (s.name().equalsIgnoreCase(value.trim()) || s.displayName.equalsIgnoreCase(value.trim())) {
                return s;
            }
        }
        return PENDING;
    }
}
