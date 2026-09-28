package com.taskmanager.concurrency;

import com.taskmanager.model.Task;
import com.taskmanager.service.TaskService;
import javafx.application.Platform;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

/**
 * DeadlineMonitor
 *
 * Demonstrates:
 * - Background periodic polling using ScheduledExecutorService
 * - Non-blocking state analysis across shared data
 * - Strict adherence to JavaFX Thread Concurrency Rules (Platform.runLater)
 *
 * WHY THIS BACKGROUND THREAD EXISTS:
 * In a real-world task manager, tasks become overdue as real-world time advances,
 * even if the user is idle or interacting with other tabs. Polling time conditions
 * on the UI thread using UI timers can cause UI thread congestion. Running this
 * on a background scheduled thread decouples real-time monitoring from the GUI.
 */
public class DeadlineMonitor implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(DeadlineMonitor.class.getName());

    private final TaskService taskService;
    private final BiConsumer<Integer, List<Task>> deadlineAlertCallback;

    /**
     * @param taskService The task service to query.
     * @param deadlineAlertCallback Callback delivering (overdueCount, overdueTasksList).
     *                              Will be dispatched to JavaFX Application Thread.
     */
    public DeadlineMonitor(TaskService taskService, BiConsumer<Integer, List<Task>> deadlineAlertCallback) {
        this.taskService = taskService;
        this.deadlineAlertCallback = deadlineAlertCallback;
    }

    public DeadlineMonitor(TaskService taskService) {
        this(taskService, null);
    }

    @Override
    public void run() {
        String threadName = Thread.currentThread().getName();
        LOGGER.fine("[" + threadName + "] Running deadline monitor check...");

        try {
            LocalDate today = LocalDate.now();

            // Obtain thread-safe snapshot so iteration is free from concurrent modification
            List<Task> snapshot = taskService.getTasksSnapshot();

            List<Task> overdueList = new ArrayList<>();
            for (Task task : snapshot) {
                if (task.isOverdue(today)) {
                    overdueList.add(task);
                }
            }

            int overdueCount = overdueList.size();
            LOGGER.fine("[" + threadName + "] Found " + overdueCount + " overdue task(s).");

            // Dispatch update to UI safely on JavaFX Application Thread
            if (deadlineAlertCallback != null) {
                Platform.runLater(() -> deadlineAlertCallback.accept(overdueCount, overdueList));
            }

        } catch (Exception e) {
            LOGGER.severe("[" + threadName + "] Exception in DeadlineMonitor: " + e.getMessage());
        }
    }
}
