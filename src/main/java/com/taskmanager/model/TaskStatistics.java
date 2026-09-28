package com.taskmanager.model;

import java.time.LocalDateTime;

/**
 * TaskStatistics Data Transfer Object (DTO)
 *
 * Encapsulates aggregated metrics computed asynchronously by background threads.
 * Keeps calculation decoupled from JavaFX presentation.
 */
public class TaskStatistics {

    private final int totalTasks;
    private final int pendingCount;
    private final int inProgressCount;
    private final int completedCount;
    private final int overdueCount;

    private final double completedPercentage;
    private final double pendingPercentage;
    private final double inProgressPercentage;

    private final int lowPriorityCount;
    private final int mediumPriorityCount;
    private final int highPriorityCount;

    private final LocalDateTime timestamp;

    public TaskStatistics(int totalTasks, int pendingCount, int inProgressCount, int completedCount,
                          int overdueCount, int lowPriorityCount, int mediumPriorityCount,
                          int highPriorityCount) {
        this.totalTasks = totalTasks;
        this.pendingCount = pendingCount;
        this.inProgressCount = inProgressCount;
        this.completedCount = completedCount;
        this.overdueCount = overdueCount;
        this.lowPriorityCount = lowPriorityCount;
        this.mediumPriorityCount = mediumPriorityCount;
        this.highPriorityCount = highPriorityCount;

        if (totalTasks > 0) {
            this.completedPercentage = (completedCount * 100.0) / totalTasks;
            this.pendingPercentage = (pendingCount * 100.0) / totalTasks;
            this.inProgressPercentage = (inProgressCount * 100.0) / totalTasks;
        } else {
            this.completedPercentage = 0.0;
            this.pendingPercentage = 0.0;
            this.inProgressPercentage = 0.0;
        }

        this.timestamp = LocalDateTime.now();
    }

    public static TaskStatistics empty() {
        return new TaskStatistics(0, 0, 0, 0, 0, 0, 0, 0);
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    public int getPendingCount() {
        return pendingCount;
    }

    public int getInProgressCount() {
        return inProgressCount;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public int getOverdueCount() {
        return overdueCount;
    }

    public double getCompletedPercentage() {
        return completedPercentage;
    }

    public double getPendingPercentage() {
        return pendingPercentage;
    }

    public double getInProgressPercentage() {
        return inProgressPercentage;
    }

    public int getLowPriorityCount() {
        return lowPriorityCount;
    }

    public int getMediumPriorityCount() {
        return mediumPriorityCount;
    }

    public int getHighPriorityCount() {
        return highPriorityCount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format("TaskStatistics[total=%d, completed=%d (%.1f%%), pending=%d (%.1f%%), overdue=%d]",
                totalTasks, completedCount, completedPercentage, pendingCount, pendingPercentage, overdueCount);
    }
}
