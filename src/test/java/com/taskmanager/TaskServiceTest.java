package com.taskmanager;

import com.taskmanager.model.Task;
import com.taskmanager.model.TaskPriority;
import com.taskmanager.model.TaskStatus;
import com.taskmanager.service.FileStorageService;
import com.taskmanager.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskServiceTest
 * Automated unit tests verifying domain logic, CRUD operations, filtering, and concurrency safety.
 */
public class TaskServiceTest {

    private TaskService taskService;
    private File testStorageFile;

    @BeforeEach
    public void setUp() {
        testStorageFile = new File("target/test-data-" + System.currentTimeMillis() + ".json");
        FileStorageService storageService = new FileStorageService(testStorageFile.getPath());
        taskService = new TaskService(storageService);
    }

    @Test
    @DisplayName("Should add task successfully with generated ID")
    public void testAddTask() {
        Task task = taskService.addTask("Study JVM Internals", "Read JVM Specification Chapter 2",
                TaskPriority.HIGH, TaskStatus.IN_PROGRESS, LocalDate.now().plusDays(2));

        assertNotNull(task.getId(), "Task ID should be generated");
        assertTrue(task.getId().startsWith("TSK-"), "Task ID should start with TSK- prefix");
        assertEquals("Study JVM Internals", task.getTitle());
        assertEquals(TaskPriority.HIGH, task.getPriority());
        assertEquals(1, taskService.getTaskCount());
    }

    @Test
    @DisplayName("Should reject blank task title with IllegalArgumentException")
    public void testAddTaskBlankTitle() {
        assertThrows(IllegalArgumentException.class, () ->
                taskService.addTask("   ", "Empty title test", TaskPriority.LOW, TaskStatus.PENDING, null));
    }

    @Test
    @DisplayName("Should update existing task attributes")
    public void testUpdateTask() {
        Task task = taskService.addTask("Initial Title", "Desc", TaskPriority.LOW, TaskStatus.PENDING, null);
        boolean updated = taskService.updateTask(task.getId(), "Updated Title", "Updated Desc",
                TaskPriority.HIGH, TaskStatus.IN_PROGRESS, LocalDate.now().plusDays(5));

        assertTrue(updated);
        Task found = taskService.getTaskById(task.getId()).orElseThrow();
        assertEquals("Updated Title", found.getTitle());
        assertEquals(TaskPriority.HIGH, found.getPriority());
        assertEquals(TaskStatus.IN_PROGRESS, found.getStatus());
    }

    @Test
    @DisplayName("Should delete task by ID")
    public void testDeleteTask() {
        Task task = taskService.addTask("Task To Delete", "Desc", TaskPriority.LOW, TaskStatus.PENDING, null);
        assertEquals(1, taskService.getTaskCount());

        boolean deleted = taskService.deleteTask(task.getId());
        assertTrue(deleted);
        assertEquals(0, taskService.getTaskCount());
        assertTrue(taskService.getTaskById(task.getId()).isEmpty());
    }

    @Test
    @DisplayName("Should mark task as completed")
    public void testMarkTaskCompleted() {
        Task task = taskService.addTask("Task To Complete", "Desc", TaskPriority.MEDIUM, TaskStatus.PENDING, null);
        assertFalse(task.isCompleted());

        taskService.markTaskCompleted(task.getId());

        Task updated = taskService.getTaskById(task.getId()).orElseThrow();
        assertTrue(updated.isCompleted());
        assertEquals(TaskStatus.COMPLETED, updated.getStatus());
    }

    @Test
    @DisplayName("Should filter and search tasks by keyword, status, and priority")
    public void testSearchAndFilter() {
        taskService.addTask("Write Java Parser", "Compiler coursework", TaskPriority.HIGH, TaskStatus.PENDING, null);
        taskService.addTask("Read OS Book", "Operating systems", TaskPriority.MEDIUM, TaskStatus.IN_PROGRESS, null);
        taskService.addTask("Fix C Bug", "GCC linking issue", TaskPriority.HIGH, TaskStatus.COMPLETED, null);

        // Search by keyword
        List<Task> searchResults = taskService.filterAndSearch("Compiler", null, null);
        assertEquals(1, searchResults.size());
        assertEquals("Write Java Parser", searchResults.get(0).getTitle());

        // Filter by Priority
        List<Task> highPriority = taskService.filterAndSearch(null, null, TaskPriority.HIGH);
        assertEquals(2, highPriority.size());

        // Filter by Status
        List<Task> completedTasks = taskService.filterAndSearch(null, TaskStatus.COMPLETED, null);
        assertEquals(1, completedTasks.size());
        assertEquals("Fix C Bug", completedTasks.get(0).getTitle());
    }

    @Test
    @DisplayName("Should detect overdue tasks correctly")
    public void testOverdueDetection() {
        // Deadline passed yesterday (overdue)
        taskService.addTask("Overdue Task", "Should be detected", TaskPriority.HIGH,
                TaskStatus.PENDING, LocalDate.now().minusDays(1));

        // Deadline tomorrow (not overdue)
        taskService.addTask("Upcoming Task", "Not overdue", TaskPriority.MEDIUM,
                TaskStatus.PENDING, LocalDate.now().plusDays(1));

        List<Task> overdue = taskService.getOverdueTasks();
        assertEquals(1, overdue.size());
        assertEquals("Overdue Task", overdue.get(0).getTitle());
    }

    @Test
    @DisplayName("Should save and load tasks to disk safely")
    public void testSaveAndLoad() throws IOException {
        taskService.addTask("Persistence Test 1", "Desc 1", TaskPriority.LOW, TaskStatus.PENDING, LocalDate.now());
        taskService.addTask("Persistence Test 2", "Desc 2", TaskPriority.HIGH, TaskStatus.COMPLETED, LocalDate.now().plusDays(3));

        taskService.save();
        assertTrue(testStorageFile.exists(), "Storage file should exist on disk");

        // Create new service loading from the same file
        FileStorageService storage2 = new FileStorageService(testStorageFile.getPath());
        TaskService loadedService = new TaskService(storage2);
        loadedService.load();

        assertEquals(2, loadedService.getTaskCount());
        assertNotNull(loadedService.getAllTasks().get(0).getId());

        // Clean up
        if (testStorageFile.exists()) {
            testStorageFile.delete();
        }
    }

    @Test
    @DisplayName("Should handle 20 concurrent threads adding tasks without data loss (Thread Safety)")
    public void testConcurrentAddsAreThreadSafe() throws InterruptedException {
        int threadCount = 20;
        int tasksPerThread = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int tId = i;
            pool.submit(() -> {
                try {
                    for (int j = 0; j < tasksPerThread; j++) {
                        taskService.addTask("Concurrent " + tId + "-" + j, "Desc",
                                TaskPriority.MEDIUM, TaskStatus.PENDING, null);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertTrue(finished, "All threads should complete within timeout");
        assertEquals(threadCount * tasksPerThread, taskService.getTaskCount(),
                "All concurrent tasks must be inserted safely without race condition loss");
    }
}
