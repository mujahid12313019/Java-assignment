package com.taskmanager.concurrency;

import com.taskmanager.service.TaskService;
import javafx.application.Platform;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AutoSaveTask
 *
 * Demonstrates:
 * - Java Multithreading using Runnable and ScheduledExecutorService
 * - Offloading Disk I/O from the JavaFX UI Application Thread
 * - Safe UI feedback dispatch via Platform.runLater(...)
 *
 * WHY THIS BACKGROUND THREAD EXISTS:
 * Disk write operations (JSON file serialization and atomic file replacement)
 * are blocking I/O calls. If performed on the JavaFX Application Thread,
 * the entire user interface would stutter, freeze, or drop frames.
 * By running AutoSaveTask as a periodic daemon background thread, data durability
 * is guaranteed without degrading user experience.
 */
public class AutoSaveTask implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(AutoSaveTask.class.getName());
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final TaskService taskService;
    private final BiConsumer<Boolean, String> uiStatusCallback;

    /**
     * @param taskService The service managing task state and persistence.
     * @param uiStatusCallback Optional callback to notify UI of save status.
     *                         Dispatched safely via Platform.runLater.
     */
    public AutoSaveTask(TaskService taskService, BiConsumer<Boolean, String> uiStatusCallback) {
        this.taskService = taskService;
        this.uiStatusCallback = uiStatusCallback;
    }

    public AutoSaveTask(TaskService taskService) {
        this(taskService, null);
    }

    @Override
    public void run() {
        String threadName = Thread.currentThread().getName();
        LOGGER.fine("[" + threadName + "] Executing periodic background auto-save...");

        try {
            // Save in-memory tasks to disk (disk I/O occurs on this background thread)
            taskService.save();

            String timeStamp = LocalTime.now().format(TIME_FORMAT);
            String message = "Auto-saved successfully at " + timeStamp;
            LOGGER.fine("[" + threadName + "] " + message);

            // Notify UI safely using Platform.runLater
            if (uiStatusCallback != null) {
                Platform.runLater(() -> uiStatusCallback.accept(true, message));
            }
        } catch (IOException e) {
            String errorMsg = "Auto-save failed: " + e.getMessage();
            LOGGER.log(Level.SEVERE, "[" + threadName + "] " + errorMsg, e);

            // Notify UI of error on the JavaFX Application Thread
            if (uiStatusCallback != null) {
                Platform.runLater(() -> uiStatusCallback.accept(false, errorMsg));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[" + threadName + "] Unexpected error during auto-save", e);
        }
    }
}
