package com.taskmanager.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Task Model Entity
 *
 * Demonstrates:
 * - Object-Oriented Programming (Encapsulation, State Validation)
 * - Modern Java Date/Time API (java.time.LocalDate, LocalDateTime)
 * - Defensive copying for thread safety
 * - JSON serialization annotations for persistence
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Task {

    private String id;
    private String title;
    private String description;
    private TaskPriority priority;
    private TaskStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdDateTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate deadline;

    private boolean completed;

    /**
     * Default No-Arg Constructor required by Jackson for JSON deserialization.
     */
    public Task() {
        this.createdDateTime = LocalDateTime.now();
        this.status = TaskStatus.PENDING;
        this.priority = TaskPriority.MEDIUM;
        this.completed = false;
    }

    /**
     * Full Parameterized Constructor.
     */
    public Task(String id, String title, String description, TaskPriority priority,
                TaskStatus status, LocalDateTime createdDateTime, LocalDate deadline, boolean completed) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = priority != null ? priority : TaskPriority.MEDIUM;
        this.status = status != null ? status : TaskStatus.PENDING;
        this.createdDateTime = createdDateTime != null ? createdDateTime : LocalDateTime.now();
        this.deadline = deadline;
        this.completed = completed || (this.status == TaskStatus.COMPLETED);
        if (this.completed) {
            this.status = TaskStatus.COMPLETED;
        }
    }

    /**
     * Copy Constructor used for defensive copying across threads.
     * Prevents shared-reference mutation issues during background serialization.
     */
    public Task(Task other) {
        if (other != null) {
            this.id = other.id;
            this.title = other.title;
            this.description = other.description;
            this.priority = other.priority;
            this.status = other.status;
            this.createdDateTime = other.createdDateTime;
            this.deadline = other.deadline;
            this.completed = other.completed;
        }
    }

    // ================= Business Helper Methods =================

    /**
     * Evaluates whether the task is overdue relative to a given date.
     * A task is overdue if it is not completed and its deadline has strictly passed.
     */
    public boolean isOverdue(LocalDate referenceDate) {
        if (this.completed || this.status == TaskStatus.COMPLETED || this.deadline == null) {
            return false;
        }
        return this.deadline.isBefore(referenceDate != null ? referenceDate : LocalDate.now());
    }

    /**
     * Convenience method checking overdue against current system date.
     */
    public boolean isOverdue() {
        return isOverdue(LocalDate.now());
    }

    /**
     * Marks the task as completed, synchronizing boolean flag and status enum.
     */
    public void markCompleted() {
        this.completed = true;
        this.status = TaskStatus.COMPLETED;
    }

    // ================= Getters and Setters (Encapsulation) =================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority != null ? priority : TaskPriority.MEDIUM;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status != null ? status : TaskStatus.PENDING;
        this.completed = (this.status == TaskStatus.COMPLETED);
    }

    public LocalDateTime getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(LocalDateTime createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
        if (completed) {
            this.status = TaskStatus.COMPLETED;
        } else if (this.status == TaskStatus.COMPLETED) {
            this.status = TaskStatus.IN_PROGRESS;
        }
    }

    // ================= Object Identity Methods =================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return Objects.equals(id, task.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", priority=" + priority +
                ", status=" + status +
                ", deadline=" + deadline +
                ", completed=" + completed +
                '}';
    }
}
