package com.taskmanager.service;

import com.taskmanager.model.Task;
import com.taskmanager.model.TaskPriority;
import com.taskmanager.model.TaskStatus;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * TaskService
 * Central business logic layer managing in-memory tasks and persistence coordination.
 *
 * Demonstrates:
 * - Thread-Safe State Management via ReentrantReadWriteLock
 *   (Permits concurrent background readers while ensuring exclusive write access)
 * - Defensive Copying to prevent ConcurrentModificationException
 * - Java Streams API for functional searching, filtering, and sorting
 * - Observer Pattern via change listeners for reactive UI synchronization
 */
public class TaskService {

    private static final Logger LOGGER = Logger.getLogger(TaskService.class.getName());

    // In-memory task repository (Keyed by Task ID for O(1) lookups)
    private final Map<String, Task> taskMap = new LinkedHashMap<>();

    // Concurrency Lock: Read-Write Lock allows concurrent reads from background workers
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();

    // File storage persistence service
    private final FileStorageService storageService;

    // Thread-safe change listeners for notifying controllers
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    // ID counter generator
    private final AtomicInteger idCounter = new AtomicInteger(1000);

    public TaskService(FileStorageService storageService) {
        this.storageService = storageService != null ? storageService : new FileStorageService();
    }

    public TaskService() {
        this(new FileStorageService());
    }

    // ================= Change Listener Registration =================

    public void addChangeListener(Runnable listener) {
        if (listener != null) {
            changeListeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    private void notifyChangeListeners() {
        for (Runnable listener : changeListeners) {
            try {
                listener.run();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error running change listener", e);
            }
        }
    }

    // ================= Persistence Operations =================

    /**
     * Loads tasks from disk into memory.
     */
    public void load() throws IOException {
        writeLock.lock();
        try {
            List<Task> loaded = storageService.loadTasks();
            taskMap.clear();
            int maxIdNum = 1000;
            for (Task t : loaded) {
                taskMap.put(t.getId(), t);
                if (t.getId() != null && t.getId().startsWith("TSK-")) {
                    try {
                        int num = Integer.parseInt(t.getId().substring(4));
                        if (num > maxIdNum) maxIdNum = num;
                    } catch (NumberFormatException ignored) {}
                }
            }
            idCounter.set(maxIdNum + 1);
            LOGGER.info("TaskService loaded " + taskMap.size() + " tasks.");
        } finally {
            writeLock.unlock();
        }
        notifyChangeListeners();
    }

    /**
     * Saves current in-memory tasks to disk.
     */
    public void save() throws IOException {
        List<Task> snapshot = getTasksSnapshot();
        storageService.saveTasks(snapshot);
        LOGGER.info("TaskService saved " + snapshot.size() + " tasks to disk.");
    }

    // ================= CRUD Operations =================

    /**
     * Adds a new task with validation and auto-generated ID if missing.
     */
    public Task addTask(String title, String description, TaskPriority priority,
                        TaskStatus status, LocalDate deadline) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Task title cannot be empty.");
        }

        String id = "TSK-" + idCounter.getAndIncrement();
        Task task = new Task(
                id,
                title.trim(),
                description != null ? description.trim() : "",
                priority != null ? priority : TaskPriority.MEDIUM,
                status != null ? status : TaskStatus.PENDING,
                LocalDateTime.now(),
                deadline,
                status == TaskStatus.COMPLETED
        );

        writeLock.lock();
        try {
            taskMap.put(task.getId(), task);
        } finally {
            writeLock.unlock();
        }

        notifyChangeListeners();
        return task;
    }

    /**
     * Updates an existing task with validation.
     */
    public boolean updateTask(String id, String title, String description,
                              TaskPriority priority, TaskStatus status, LocalDate deadline) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Task ID cannot be null.");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Task title cannot be empty.");
        }

        writeLock.lock();
        try {
            Task existing = taskMap.get(id);
            if (existing == null) {
                return false;
            }
            existing.setTitle(title.trim());
            existing.setDescription(description != null ? description.trim() : "");
            existing.setPriority(priority != null ? priority : TaskPriority.MEDIUM);
            existing.setStatus(status != null ? status : TaskStatus.PENDING);
            existing.setDeadline(deadline);
        } finally {
            writeLock.unlock();
        }

        notifyChangeListeners();
        return true;
    }

    /**
     * Deletes a task by ID.
     */
    public boolean deleteTask(String id) {
        if (id == null) return false;
        boolean removed;
        writeLock.lock();
        try {
            removed = (taskMap.remove(id) != null);
        } finally {
            writeLock.unlock();
        }

        if (removed) {
            notifyChangeListeners();
        }
        return removed;
    }

    /**
     * Marks a task as completed.
     */
    public boolean markTaskCompleted(String id) {
        if (id == null) return false;
        boolean updated = false;
        writeLock.lock();
        try {
            Task existing = taskMap.get(id);
            if (existing != null) {
                existing.markCompleted();
                updated = true;
            }
        } finally {
            writeLock.unlock();
        }

        if (updated) {
            notifyChangeListeners();
        }
        return updated;
    }

    /**
     * Retrieves a single task by ID.
     */
    public Optional<Task> getTaskById(String id) {
        readLock.lock();
        try {
            Task t = taskMap.get(id);
            return t != null ? Optional.of(new Task(t)) : Optional.empty();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Returns a defensive shallow copy of all tasks for display.
     */
    public List<Task> getAllTasks() {
        readLock.lock();
        try {
            return new ArrayList<>(taskMap.values());
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Returns an independent deep copy of all tasks.
     * This snapshot is essential for background workers (auto-save, statistics)
     * so they can operate freely without thread interference or ConcurrentModificationException.
     */
    public List<Task> getTasksSnapshot() {
        readLock.lock();
        try {
            List<Task> snapshot = new ArrayList<>(taskMap.size());
            for (Task t : taskMap.values()) {
                snapshot.add(new Task(t)); // Deep copy using copy constructor
            }
            return snapshot;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Searches and filters tasks using Java Streams.
     */
    public List<Task> filterAndSearch(String searchKeyword, TaskStatus statusFilter, TaskPriority priorityFilter) {
        readLock.lock();
        try {
            return taskMap.values().stream()
                    .filter(task -> {
                        // Keyword match on Title or Description
                        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                            String lower = searchKeyword.toLowerCase();
                            boolean titleMatch = task.getTitle() != null && task.getTitle().toLowerCase().contains(lower);
                            boolean descMatch = task.getDescription() != null && task.getDescription().toLowerCase().contains(lower);
                            boolean idMatch = task.getId() != null && task.getId().toLowerCase().contains(lower);
                            if (!titleMatch && !descMatch && !idMatch) {
                                return false;
                            }
                        }
                        // Status filter
                        if (statusFilter != null && task.getStatus() != statusFilter) {
                            return false;
                        }
                        // Priority filter
                        if (priorityFilter != null && task.getPriority() != priorityFilter) {
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Retrieves all overdue tasks relative to today.
     */
    public List<Task> getOverdueTasks() {
        LocalDate today = LocalDate.now();
        readLock.lock();
        try {
            return taskMap.values().stream()
                    .filter(t -> t.isOverdue(today))
                    .collect(Collectors.toList());
        } finally {
            readLock.unlock();
        }
    }

    public int getTaskCount() {
        readLock.lock();
        try {
            return taskMap.size();
        } finally {
            readLock.unlock();
        }
    }
}
