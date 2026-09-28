package com.taskmanager.service;

import com.taskmanager.dao.TaskDAO;
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
 * Central business logic layer managing in-memory task cache AND SQLite persistence via TaskDAO.
 *
 * Demonstrates:
 * - Thread-Safe State Management via ReentrantReadWriteLock
 *   (Permits concurrent background readers while ensuring exclusive write access)
 * - Defensive Copying to prevent ConcurrentModificationException
 * - Java Streams API for functional searching, filtering, and sorting
 * - Observer Pattern via change listeners for reactive UI synchronization
 * - SQLite CRUD operations delegated to TaskDAO (JDBC + PreparedStatement)
 */
public class TaskService {

    private static final Logger LOGGER = Logger.getLogger(TaskService.class.getName());

    // In-memory task cache (Keyed by Task ID for O(1) lookups)
    private final Map<String, Task> taskMap = new LinkedHashMap<>();

    // CONCURRENCY: Read-Write Lock allows concurrent reads from background workers
    // while writes are mutually exclusive (no race conditions on task data)
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();

    // SQLite DAO for real JDBC database operations
    private final TaskDAO taskDAO;

    // Legacy JSON file storage (kept for backward-compatibility / export)
    private final FileStorageService storageService;

    // Thread-safe change listeners for notifying UI controllers via Platform.runLater
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    // Fallback ID counter (used only when DB ID is not available)
    private final AtomicInteger idCounter = new AtomicInteger(1000);

    // Flag: whether SQLite is available and in use
    private boolean dbAvailable = false;

    public TaskService(TaskDAO taskDAO, FileStorageService storageService) {
        this.taskDAO = taskDAO;
        this.storageService = storageService != null ? storageService : new FileStorageService();
        this.dbAvailable = (taskDAO != null);
    }

    public TaskService(FileStorageService storageService) {
        this(null, storageService);
    }

    /** Constructor used in unit tests (in-memory only, no DAO, no file) */
    public TaskService() {
        this(null, new FileStorageService());
    }

    // ================= Change Listener Registration =================

    public void addChangeListener(Runnable listener) {
        if (listener != null) changeListeners.add(listener);
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
     * Loads tasks from SQLite database into the in-memory cache.
     * Falls back to JSON file if SQLite is unavailable.
     */
    public void load() throws IOException {
        writeLock.lock();
        try {
            taskMap.clear();
            if (dbAvailable) {
                // PRIMARY: Load from SQLite database via TaskDAO (real JDBC SELECT with JOIN)
                List<Task> loaded = taskDAO.getAllTasks();
                for (Task t : loaded) {
                    taskMap.put(t.getId(), t);
                }
                LOGGER.info("TaskService loaded " + taskMap.size() + " tasks from SQLite database.");
            } else {
                // FALLBACK: Load from JSON file storage
                List<Task> loaded = storageService.loadTasks();
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
                LOGGER.info("TaskService loaded " + taskMap.size() + " tasks from JSON file (DB unavailable).");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load tasks", e);
            throw new IOException("Load failed: " + e.getMessage(), e);
        } finally {
            writeLock.unlock();
        }
        notifyChangeListeners();
    }

    /**
     * Saves current in-memory tasks to disk (used by AutoSaveTask background thread).
     * When SQLite is in use, in-memory cache IS the DB mirror — export to JSON as backup.
     */
    public void save() throws IOException {
        if (!dbAvailable) {
            // JSON persistence when no DB
            List<Task> snapshot = getTasksSnapshot();
            storageService.saveTasks(snapshot);
            LOGGER.fine("TaskService saved " + snapshot.size() + " tasks to JSON file.");
        } else {
            // SQLite is auto-persistent; auto-save just logs a heartbeat
            LOGGER.fine("SQLite auto-save heartbeat: " + getTaskCount() + " tasks live in DB.");
        }
    }

    // ================= CRUD Operations (wired to SQLite) =================

    /**
     * CREATE: Adds a new task. Inserts into SQLite and updates in-memory cache.
     * SQL: INSERT INTO tasks (...) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
     */
    public Task addTask(String title, String description, TaskPriority priority,
                        TaskStatus status, LocalDate deadline) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Task title cannot be empty.");
        }

        Task task = new Task(
                null, // ID will be assigned by DB (AUTOINCREMENT) or counter
                title.trim(),
                description != null ? description.trim() : "",
                priority != null ? priority : TaskPriority.MEDIUM,
                status != null ? status : TaskStatus.PENDING,
                LocalDateTime.now(),
                deadline,
                status == TaskStatus.COMPLETED
        );
        task.setUserId(1);
        task.setCategoryId(1);
        task.setUserName("Default Student");
        task.setCategoryName("Academic / Coursework");

        if (dbAvailable) {
            try {
                // SQL CREATE: INSERT into SQLite via PreparedStatement
                taskDAO.createTask(task, 1, 1);
                LOGGER.info("SQL CREATE: Inserted task id=" + task.getId() + " into SQLite.");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "SQL CREATE failed, falling back to in-memory only", e);
                task.setId("TSK-" + idCounter.getAndIncrement());
            }
        } else {
            task.setId("TSK-" + idCounter.getAndIncrement());
        }

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
     * UPDATE: Modifies an existing task. Updates SQLite record and in-memory cache.
     * SQL: UPDATE tasks SET title=?, description=?, priority=?, status=?, deadline=?, completed=? WHERE id=?
     */
    public boolean updateTask(String id, String title, String description,
                              TaskPriority priority, TaskStatus status, LocalDate deadline) {
        if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("Task ID cannot be null.");
        if (title == null || title.trim().isEmpty()) throw new IllegalArgumentException("Task title cannot be empty.");

        writeLock.lock();
        try {
            Task existing = taskMap.get(id);
            if (existing == null) return false;

            existing.setTitle(title.trim());
            existing.setDescription(description != null ? description.trim() : "");
            existing.setPriority(priority != null ? priority : TaskPriority.MEDIUM);
            existing.setStatus(status != null ? status : TaskStatus.PENDING);
            existing.setDeadline(deadline);

            if (dbAvailable) {
                try {
                    // SQL UPDATE: Update SQLite record via PreparedStatement
                    taskDAO.updateTask(existing);
                    LOGGER.info("SQL UPDATE: Updated task id=" + id + " in SQLite.");
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "SQL UPDATE failed for id=" + id, e);
                }
            }
        } finally {
            writeLock.unlock();
        }

        notifyChangeListeners();
        return true;
    }

    /**
     * DELETE: Removes a task from SQLite and in-memory cache.
     * SQL: DELETE FROM tasks WHERE id = ?
     */
    public boolean deleteTask(String id) {
        if (id == null) return false;
        boolean removed = false;

        writeLock.lock();
        try {
            Task task = taskMap.remove(id);
            removed = (task != null);

            if (removed && dbAvailable) {
                try {
                    // SQL DELETE: Remove from SQLite via PreparedStatement
                    int numericId = parseNumericId(id);
                    if (numericId > 0) {
                        taskDAO.deleteTask(numericId);
                        LOGGER.info("SQL DELETE: Deleted task id=" + id + " from SQLite.");
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "SQL DELETE failed for id=" + id, e);
                }
            }
        } finally {
            writeLock.unlock();
        }

        if (removed) notifyChangeListeners();
        return removed;
    }

    /**
     * Marks a task as completed in both SQLite and in-memory cache.
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

                if (dbAvailable) {
                    try {
                        int numericId = parseNumericId(id);
                        if (numericId > 0) {
                            taskDAO.markTaskCompleted(numericId);
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "SQL UPDATE (markCompleted) failed for id=" + id, e);
                    }
                }
            }
        } finally {
            writeLock.unlock();
        }

        if (updated) notifyChangeListeners();
        return updated;
    }

    // ================= READ Operations =================

    /**
     * READ: Returns a defensive copy of a single task from the in-memory cache.
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
     * READ: Returns a defensive shallow copy of all tasks for display.
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
     * Returns an independent deep copy (snapshot) for background workers.
     * Background threads (AutoSave, Statistics) use this to avoid ConcurrentModificationException.
     */
    public List<Task> getTasksSnapshot() {
        readLock.lock();
        try {
            List<Task> snapshot = new ArrayList<>(taskMap.size());
            for (Task t : taskMap.values()) {
                snapshot.add(new Task(t)); // Deep copy via copy constructor
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
                        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                            String lower = searchKeyword.toLowerCase();
                            boolean titleMatch = task.getTitle() != null && task.getTitle().toLowerCase().contains(lower);
                            boolean descMatch = task.getDescription() != null && task.getDescription().toLowerCase().contains(lower);
                            boolean idMatch = task.getId() != null && task.getId().toLowerCase().contains(lower);
                            if (!titleMatch && !descMatch && !idMatch) return false;
                        }
                        if (statusFilter != null && task.getStatus() != statusFilter) return false;
                        if (priorityFilter != null && task.getPriority() != priorityFilter) return false;
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

    public boolean isDbAvailable() {
        return dbAvailable;
    }

    // ================= Private Helpers =================

    private int parseNumericId(String id) {
        if (id == null) return 0;
        try {
            return Integer.parseInt(id.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
