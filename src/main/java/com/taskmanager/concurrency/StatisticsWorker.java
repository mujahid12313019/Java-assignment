package com.taskmanager.concurrency;

import com.taskmanager.model.Task;
import com.taskmanager.model.TaskStatistics;
import com.taskmanager.service.StatisticsService;
import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * StatisticsWorker
 *
 * Demonstrates:
 * - Java Callable<T> and Runnable for asynchronous computation
 * - Offloading CPU aggregation from the JavaFX UI Application Thread
 * - Returning computed results back to the UI thread via Platform.runLater(...)
 *
 * WHY THIS BACKGROUND WORKER EXISTS:
 * When computing statistics over large task sets (e.g. iterating over hundreds
 * of tasks to calculate ratios, percentages, priority histograms, and date comparisons),
 * executing on the JavaFX Application Thread would drop UI animation frames and cause
 * button click latency. StatisticsWorker calculates the metrics in a worker thread and
 * safely renders the finished result on the UI.
 */
public class StatisticsWorker implements Callable<TaskStatistics>, Runnable {

    private static final Logger LOGGER = Logger.getLogger(StatisticsWorker.class.getName());

    private final List<Task> tasksSnapshot;
    private final StatisticsService statisticsService;
    private final Consumer<TaskStatistics> uiResultConsumer;

    public StatisticsWorker(List<Task> tasksSnapshot, StatisticsService statisticsService,
                            Consumer<TaskStatistics> uiResultConsumer) {
        this.tasksSnapshot = tasksSnapshot;
        this.statisticsService = statisticsService != null ? statisticsService : new StatisticsService();
        this.uiResultConsumer = uiResultConsumer;
    }

    public StatisticsWorker(List<Task> tasksSnapshot, StatisticsService statisticsService) {
        this(tasksSnapshot, statisticsService, null);
    }

    /**
     * Callable execution returning TaskStatistics directly.
     */
    @Override
    public TaskStatistics call() {
        String threadName = Thread.currentThread().getName();
        LOGGER.fine("[" + threadName + "] Computing statistics in background...");
        return statisticsService.calculateStatistics(tasksSnapshot);
    }

    /**
     * Runnable execution that computes and automatically dispatches to JavaFX UI.
     */
    @Override
    public void run() {
        TaskStatistics stats = call();
        if (uiResultConsumer != null) {
            Platform.runLater(() -> uiResultConsumer.accept(stats));
        }
    }
}
