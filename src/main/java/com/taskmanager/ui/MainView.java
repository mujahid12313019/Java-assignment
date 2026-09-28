package com.taskmanager.ui;

import com.taskmanager.concurrency.AutoSaveTask;
import com.taskmanager.concurrency.DeadlineMonitor;
import com.taskmanager.model.Task;
import com.taskmanager.service.StatisticsService;
import com.taskmanager.service.TaskService;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MainView
 * Assembles the master desktop layout containing Header, TabPane, Views, and Status Bar.
 *
 * Demonstrates:
 * - Complex JavaFX desktop application composition (BorderPane, TabPane)
 * - Background daemon thread lifecycle management (ScheduledExecutorService)
 * - Real-time cross-tab synchronization and status bar notifications
 */
public class MainView extends BorderPane {

    private static final Logger LOGGER = Logger.getLogger(MainView.class.getName());

    private final TaskService taskService;
    private final StatisticsService statisticsService;
    private final Stage primaryStage;

    private final TabPane tabPane = new TabPane();
    private DashboardView dashboardView;
    private TaskView taskView;
    private StatisticsView statisticsView;
    private AboutView aboutView;

    // Status Bar Labels
    private final Label autoSaveStatusLbl = new Label("Auto-Save: Ready");
    private final Label deadlineStatusLbl = new Label("Deadlines: Monitoring active");
    private final Label threadCountLbl = new Label("Background Threads: 2 active");

    // Scheduled background service executor
    private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true); // Daemon threads allow clean JVM shutdown when GUI closes
        t.setName("TaskManager-ScheduledWorker-" + t.getId());
        return t;
    });

    public MainView(TaskService taskService, StatisticsService statisticsService, Stage primaryStage) {
        this.taskService = taskService;
        this.statisticsService = statisticsService;
        this.primaryStage = primaryStage;

        buildUI();
        initBackgroundThreads();
        bindChangeListeners();
    }

    private void buildUI() {
        // 1. Top Header Banner
        VBox topContainer = new VBox();

        HBox header = new HBox(16);
        header.getStyleClass().add("app-header");
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("Multithreaded Task Management System");
        title.getStyleClass().add("app-title");
        Label subtitle = new Label("JavaFX Desktop Architecture • Concurrency • Thread-Safe Data Layer");
        subtitle.getStyleClass().add("app-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button manualSaveBtn = new Button("💾 Save Now");
        manualSaveBtn.getStyleClass().add("btn-secondary");
        manualSaveBtn.setOnAction(e -> handleManualSave());

        header.getChildren().addAll(titleBox, spacer, manualSaveBtn);

        // 2. Navigation Tabs
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        dashboardView = new DashboardView(taskService, statisticsService, primaryStage, () -> tabPane.getSelectionModel().select(1));
        Tab dashTab = new Tab("📊 Dashboard", dashboardView);

        taskView = new TaskView(taskService, primaryStage);
        Tab tasksTab = new Tab("📋 Tasks", taskView);

        statisticsView = new StatisticsView(taskService, statisticsService);
        Tab statsTab = new Tab("📈 Statistics & Lab", statisticsView);

        aboutView = new AboutView();
        Tab aboutTab = new Tab("ℹ️ About & Viva Notes", aboutView);

        tabPane.getTabs().addAll(dashTab, tasksTab, statsTab, aboutTab);

        // When switching tabs, refresh target view
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == dashTab) dashboardView.refresh();
            else if (newTab == tasksTab) taskView.refresh();
            else if (newTab == statsTab) statisticsView.refresh();
        });

        topContainer.getChildren().addAll(header, tabPane);
        setTop(topContainer);

        // 3. Bottom Status / Footer Bar
        HBox statusBar = new HBox(20);
        statusBar.getStyleClass().add("status-bar");
        statusBar.setAlignment(Pos.CENTER_LEFT);

        autoSaveStatusLbl.getStyleClass().add("status-label");
        deadlineStatusLbl.getStyleClass().add("status-label");
        threadCountLbl.getStyleClass().add("status-label");

        Region botSpacer = new Region();
        HBox.setHgrow(botSpacer, Priority.ALWAYS);

        Label jvmInfoLbl = new Label("Java " + System.getProperty("java.version") + " • HotSpot JVM");
        jvmInfoLbl.getStyleClass().add("status-label");

        statusBar.getChildren().addAll(autoSaveStatusLbl, new Separator(javafx.geometry.Orientation.VERTICAL),
                deadlineStatusLbl, new Separator(javafx.geometry.Orientation.VERTICAL),
                threadCountLbl, botSpacer, jvmInfoLbl);

        setBottom(statusBar);
    }

    /**
     * Initializes background threads:
     * 1. AutoSaveTask: Runs every 30 seconds to persist data without UI hitching.
     * 2. DeadlineMonitor: Runs every 10 seconds to scan for overdue items.
     */
    private void initBackgroundThreads() {
        // Auto-Save Task (period = 30 seconds)
        AutoSaveTask autoSaveRunnable = new AutoSaveTask(taskService, (success, message) -> {
            // Callback invoked on JavaFX Application Thread via Platform.runLater inside AutoSaveTask
            autoSaveStatusLbl.setText("Auto-Save: " + (success ? "✓ " : "✗ ") + message);
        });
        scheduledExecutor.scheduleWithFixedDelay(autoSaveRunnable, 15, 30, TimeUnit.SECONDS);

        // Deadline Monitor Task (period = 10 seconds)
        DeadlineMonitor deadlineRunnable = new DeadlineMonitor(taskService, (overdueCount, overdueList) -> {
            // Callback invoked on JavaFX Application Thread via Platform.runLater inside DeadlineMonitor
            if (overdueCount > 0) {
                deadlineStatusLbl.setText("⚠️ Deadlines: " + overdueCount + " overdue task(s) require action!");
                deadlineStatusLbl.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
            } else {
                deadlineStatusLbl.setText("✓ Deadlines: All tasks on schedule");
                deadlineStatusLbl.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
            }
        });
        scheduledExecutor.scheduleWithFixedDelay(deadlineRunnable, 2, 10, TimeUnit.SECONDS);

        LOGGER.info("Background ScheduledExecutorService initialized with AutoSave and DeadlineMonitor.");
    }

    private void bindChangeListeners() {
        // When tasks change, notify active views safely on the FX Application Thread
        taskService.addChangeListener(() -> Platform.runLater(() -> {
            dashboardView.refresh();
            taskView.refresh();
            statisticsView.refresh();
        }));
    }

    private void handleManualSave() {
        try {
            taskService.save();
            autoSaveStatusLbl.setText("Manual Save: ✓ Successfully saved to disk");
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(primaryStage);
            alert.setTitle("Save Error");
            alert.setHeaderText("Unable to save tasks to disk");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Gracefully shuts down all background executor services.
     */
    public void shutdown() {
        LOGGER.info("Shutting down background ScheduledExecutorService...");
        scheduledExecutor.shutdown();
        try {
            if (!scheduledExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        if (statisticsView != null) {
            statisticsView.shutdown();
        }
    }
}
