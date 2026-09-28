package com.taskmanager.service;

import com.taskmanager.model.Task;
import com.taskmanager.model.TaskPriority;
import com.taskmanager.model.TaskStatistics;
import com.taskmanager.model.TaskStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * StatisticsService
 * Pure computational service calculating aggregated task statistics.
 *
 * Demonstrates:
 * - Functional programming with Java Streams
 * - Stateless, thread-safe service design
 */
public class StatisticsService {

    /**
     * Computes statistics from an arbitrary list of tasks (typically an immutable snapshot).
     */
    public TaskStatistics calculateStatistics(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return TaskStatistics.empty();
        }

        LocalDate today = LocalDate.now();

        int total = tasks.size();
        int pending = 0;
        int inProgress = 0;
        int completed = 0;
        int overdue = 0;

        int low = 0;
        int medium = 0;
        int high = 0;

        for (Task task : tasks) {
            // Status breakdown
            if (task.getStatus() == TaskStatus.PENDING) {
                pending++;
            } else if (task.getStatus() == TaskStatus.IN_PROGRESS) {
                inProgress++;
            } else if (task.getStatus() == TaskStatus.COMPLETED || task.isCompleted()) {
                completed++;
            }

            // Overdue check
            if (task.isOverdue(today)) {
                overdue++;
            }

            // Priority distribution
            if (task.getPriority() == TaskPriority.LOW) {
                low++;
            } else if (task.getPriority() == TaskPriority.MEDIUM) {
                medium++;
            } else if (task.getPriority() == TaskPriority.HIGH) {
                high++;
            }
        }

        return new TaskStatistics(total, pending, inProgress, completed, overdue, low, medium, high);
    }
}
