package com.taskmanager;

import com.taskmanager.model.Task;
import com.taskmanager.model.TaskPriority;
import com.taskmanager.model.TaskStatistics;
import com.taskmanager.model.TaskStatus;
import com.taskmanager.service.StatisticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * StatisticsServiceTest
 * Verifies accuracy of metric aggregations and percentage computations.
 */
public class StatisticsServiceTest {

    private final StatisticsService statisticsService = new StatisticsService();

    @Test
    @DisplayName("Should return zero counts for empty task list")
    public void testEmptyList() {
        TaskStatistics stats = statisticsService.calculateStatistics(new ArrayList<>());
        assertEquals(0, stats.getTotalTasks());
        assertEquals(0.0, stats.getCompletedPercentage());
        assertEquals(0.0, stats.getPendingPercentage());
        assertEquals(0, stats.getOverdueCount());
    }

    @Test
    @DisplayName("Should correctly calculate metrics, percentages, and priority distributions")
    public void testStatisticsAggregations() {
        List<Task> tasks = new ArrayList<>();

        // 1. Completed High Priority (not overdue)
        tasks.add(new Task("T1", "Task 1", "Desc", TaskPriority.HIGH, TaskStatus.COMPLETED,
                LocalDateTime.now(), LocalDate.now().minusDays(2), true));

        // 2. Pending Medium Priority (overdue)
        tasks.add(new Task("T2", "Task 2", "Desc", TaskPriority.MEDIUM, TaskStatus.PENDING,
                LocalDateTime.now(), LocalDate.now().minusDays(1), false));

        // 3. In Progress Low Priority (upcoming deadline)
        tasks.add(new Task("T3", "Task 3", "Desc", TaskPriority.LOW, TaskStatus.IN_PROGRESS,
                LocalDateTime.now(), LocalDate.now().plusDays(3), false));

        // 4. Pending High Priority (upcoming deadline)
        tasks.add(new Task("T4", "Task 4", "Desc", TaskPriority.HIGH, TaskStatus.PENDING,
                LocalDateTime.now(), LocalDate.now().plusDays(5), false));

        TaskStatistics stats = statisticsService.calculateStatistics(tasks);

        assertEquals(4, stats.getTotalTasks());
        assertEquals(1, stats.getCompletedCount());
        assertEquals(2, stats.getPendingCount());
        assertEquals(1, stats.getInProgressCount());
        assertEquals(1, stats.getOverdueCount()); // T2 only (T1 is completed, so not overdue)

        assertEquals(25.0, stats.getCompletedPercentage(), 0.001);
        assertEquals(50.0, stats.getPendingPercentage(), 0.001);
        assertEquals(25.0, stats.getInProgressPercentage(), 0.001);

        assertEquals(1, stats.getLowPriorityCount());
        assertEquals(1, stats.getMediumPriorityCount());
        assertEquals(2, stats.getHighPriorityCount());
    }
}
