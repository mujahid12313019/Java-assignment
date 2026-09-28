package com.taskmanager.ui;

import com.taskmanager.concurrency.ConcurrencyDemo;
import com.taskmanager.concurrency.StatisticsWorker;
import com.taskmanager.model.TaskStatistics;
import com.taskmanager.service.StatisticsService;
import com.taskmanager.service.TaskService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * StatisticsView
 * Visualizes comprehensive project analytics and provides interactive concurrency demonstrations.
 *
 * Demonstrates:
 * - Asynchronous background data computation via ExecutorService & StatisticsWorker
 * - Visual progress tracking (ProgressBar)
 * - Safe JavaFX UI updates via Platform.runLater
 * - In-app live execution of Concurrency and Race Condition experiments
 */
public class StatisticsView extends ScrollPane {

    private final TaskService taskService;
    private final StatisticsService statisticsService;
    private final ExecutorService backgroundPool = Executors.newCachedThreadPool();

    // Metric Summary Labels
    private final Label totalTasksLbl = new Label("0");
    private final Label completedPctLbl = new Label("0.0%");
    private final Label pendingPctLbl = new Label("0.0%");
    private final Label inProgressPctLbl = new Label("0.0%");
    private final Label overdueCountLbl = new Label("0");

    // Progress Bars
    private final ProgressBar completedProgress = new ProgressBar(0);
    private final ProgressBar pendingProgress = new ProgressBar(0);
    private final ProgressBar inProgressProgress = new ProgressBar(0);

    // Priority Distribution Labels & Bars
    private final Label lowPriorityLbl = new Label("0");
    private final Label medPriorityLbl = new Label("0");
    private final Label highPriorityLbl = new Label("0");

    private final ProgressBar lowProgressBar = new ProgressBar(0);
    private final ProgressBar medProgressBar = new ProgressBar(0);
    private final ProgressBar highProgressBar = new ProgressBar(0);

    private final Label lastCalculatedLbl = new Label("Ready");

    // Concurrency Demo Output Area
    private final TextArea concurrencyOutputArea = new TextArea();

    public StatisticsView(TaskService taskService, StatisticsService statisticsService) {
        this.taskService = taskService;
        this.statisticsService = statisticsService;

        setFitToWidth(true);
        setStyle("-fx-background-color: transparent;");

        VBox content = new VBox(20);
        content.getStyleClass().add("view-container");
        buildUI(content);
        setContent(content);

        refresh();
    }

    private void buildUI(VBox root) {
        // Section 1: Completion Status Breakdown Card
        VBox statusCard = new VBox(14);
        statusCard.getStyleClass().add("card");

        HBox cardHeader = new HBox(10);
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Overall Completion & Lifecycle Breakdown");
        title.getStyleClass().add("card-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button asyncRecalcBtn = new Button("🔄 Async Recalculate (Background Worker)");
        asyncRecalcBtn.getStyleClass().add("btn-secondary");
        asyncRecalcBtn.setOnAction(e -> calculateAsync());

        cardHeader.getChildren().addAll(title, spacer, asyncRecalcBtn);

        GridPane statusGrid = new GridPane();
        statusGrid.setHgap(20);
        statusGrid.setVgap(12);
        statusGrid.setPadding(new Insets(10, 0, 10, 0));

        completedProgress.setPrefWidth(260);
        completedProgress.getStyleClass().add("progress-bar-completed");
        pendingProgress.setPrefWidth(260);
        pendingProgress.getStyleClass().add("progress-bar-pending");
        inProgressProgress.setPrefWidth(260);

        statusGrid.add(new Label("Total Managed Tasks:"), 0, 0);
        statusGrid.add(totalTasksLbl, 1, 0);

        statusGrid.add(new Label("Completed Tasks:"), 0, 1);
        statusGrid.add(completedProgress, 1, 1);
        statusGrid.add(completedPctLbl, 2, 1);

        statusGrid.add(new Label("In Progress Tasks:"), 0, 2);
        statusGrid.add(inProgressProgress, 1, 2);
        statusGrid.add(inProgressPctLbl, 2, 2);

        statusGrid.add(new Label("Pending Tasks:"), 0, 3);
        statusGrid.add(pendingProgress, 1, 3);
        statusGrid.add(pendingPctLbl, 2, 3);

        statusGrid.add(new Label("Overdue Tasks:"), 0, 4);
        statusGrid.add(overdueCountLbl, 1, 4);

        statusCard.getChildren().addAll(cardHeader, statusGrid, lastCalculatedLbl);

        // Section 2: Priority Distribution Card
        VBox priorityCard = new VBox(14);
        priorityCard.getStyleClass().add("card");

        Label priorityTitle = new Label("Task Priority Distribution");
        priorityTitle.getStyleClass().add("card-title");

        GridPane priorityGrid = new GridPane();
        priorityGrid.setHgap(20);
        priorityGrid.setVgap(12);

        lowProgressBar.setPrefWidth(260);
        medProgressBar.setPrefWidth(260);
        highProgressBar.setPrefWidth(260);

        priorityGrid.add(new Label("Low Priority:"), 0, 0);
        priorityGrid.add(lowProgressBar, 1, 0);
        priorityGrid.add(lowPriorityLbl, 2, 0);

        priorityGrid.add(new Label("Medium Priority:"), 0, 1);
        priorityGrid.add(medProgressBar, 1, 1);
        priorityGrid.add(medPriorityLbl, 2, 1);

        priorityGrid.add(new Label("High Priority (Urgent):"), 0, 2);
        priorityGrid.add(highProgressBar, 1, 2);
        priorityGrid.add(highPriorityLbl, 2, 2);

        priorityCard.getChildren().addAll(priorityTitle, priorityGrid);

        // Section 3: Concurrency & Race Condition Experimentation Panel
        VBox concurrencyCard = new VBox(12);
        concurrencyCard.getStyleClass().add("card");

        Label concTitle = new Label("Multithreading & Concurrency Laboratory (Viva Demonstrator)");
        concTitle.getStyleClass().add("card-title");

        Label concDesc = new Label("Execute live concurrency experiments on background worker threads to compare unsynchronized race conditions against thread-safe synchronization.");
        concDesc.setWrapText(true);
        concDesc.setStyle("-fx-text-fill: #64748b;");

        HBox btnBox = new HBox(12);
        Button runConflictBtn = new Button("⚠️ Run Race Condition (Unsynchronized)");
        runConflictBtn.getStyleClass().add("btn-danger");
        runConflictBtn.setOnAction(e -> runUnsynchronizedDemo());

        Button runSafeBtn = new Button("✅ Run Thread-Safe Test (ReentrantReadWriteLock)");
        runSafeBtn.getStyleClass().add("btn-success");
        runSafeBtn.setOnAction(e -> runSynchronizedDemo());

        btnBox.getChildren().addAll(runConflictBtn, runSafeBtn);

        concurrencyOutputArea.setEditable(false);
        concurrencyOutputArea.setPrefRowCount(7);
        concurrencyOutputArea.setWrapText(true);
        concurrencyOutputArea.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px;");
        concurrencyOutputArea.setText("Ready for concurrency experimentation.\nClick one of the buttons above to execute worker threads in real time.");

        concurrencyCard.getChildren().addAll(concTitle, concDesc, btnBox, concurrencyOutputArea);

        root.getChildren().addAll(statusCard, priorityCard, concurrencyCard);
    }

    /**
     * Executes asynchronous calculation via StatisticsWorker on a background thread.
     */
    public void calculateAsync() {
        lastCalculatedLbl.setText("Calculating in background worker thread...");
        long startTime = System.currentTimeMillis();

        StatisticsWorker worker = new StatisticsWorker(
                taskService.getTasksSnapshot(),
                statisticsService,
                stats -> {
                    long duration = System.currentTimeMillis() - startTime;
                    applyStatistics(stats);
                    lastCalculatedLbl.setText(String.format("Calculated on background thread in %d ms (Updated at %s)",
                            duration, stats.getTimestamp().toLocalTime().toString()));
                }
        );

        backgroundPool.execute(worker);
    }

    public void refresh() {
        calculateAsync();
    }

    private void applyStatistics(TaskStatistics stats) {
        totalTasksLbl.setText(String.valueOf(stats.getTotalTasks()));
        completedPctLbl.setText(String.format("%.1f%% (%d)", stats.getCompletedPercentage(), stats.getCompletedCount()));
        pendingPctLbl.setText(String.format("%.1f%% (%d)", stats.getPendingPercentage(), stats.getPendingCount()));
        inProgressPctLbl.setText(String.format("%.1f%% (%d)", stats.getInProgressPercentage(), stats.getInProgressCount()));
        overdueCountLbl.setText(String.valueOf(stats.getOverdueCount()));

        completedProgress.setProgress(stats.getCompletedPercentage() / 100.0);
        pendingProgress.setProgress(stats.getPendingPercentage() / 100.0);
        inProgressProgress.setProgress(stats.getInProgressPercentage() / 100.0);

        int total = stats.getTotalTasks();
        if (total > 0) {
            lowProgressBar.setProgress((double) stats.getLowPriorityCount() / total);
            medProgressBar.setProgress((double) stats.getMediumPriorityCount() / total);
            highProgressBar.setProgress((double) stats.getHighPriorityCount() / total);
        } else {
            lowProgressBar.setProgress(0);
            medProgressBar.setProgress(0);
            highProgressBar.setProgress(0);
        }

        lowPriorityLbl.setText(stats.getLowPriorityCount() + " (" + (total > 0 ? (stats.getLowPriorityCount() * 100 / total) : 0) + "%)");
        medPriorityLbl.setText(stats.getMediumPriorityCount() + " (" + (total > 0 ? (stats.getMediumPriorityCount() * 100 / total) : 0) + "%)");
        highPriorityLbl.setText(stats.getHighPriorityCount() + " (" + (total > 0 ? (stats.getHighPriorityCount() * 100 / total) : 0) + "%)");
    }

    private void runUnsynchronizedDemo() {
        concurrencyOutputArea.setText("Executing 10 concurrent threads without synchronization on a background pool...");
        backgroundPool.submit(() -> {
            String log = ConcurrencyDemo.demonstrateUnsynchronizedConflict();
            Platform.runLater(() -> concurrencyOutputArea.setText(log));
        });
    }

    private void runSynchronizedDemo() {
        concurrencyOutputArea.setText("Executing 10 concurrent threads with ReentrantReadWriteLock synchronization...");
        backgroundPool.submit(() -> {
            String log = ConcurrencyDemo.demonstrateSynchronizedSafety();
            Platform.runLater(() -> concurrencyOutputArea.setText(log));
        });
    }

    public void shutdown() {
        backgroundPool.shutdown();
    }
}
