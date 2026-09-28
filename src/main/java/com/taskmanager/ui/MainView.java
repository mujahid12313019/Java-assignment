package com.taskmanager.ui;

import com.taskmanager.concurrency.AutoSaveTask;
import com.taskmanager.concurrency.BackgroundTaskManager;
import com.taskmanager.concurrency.DeadlineMonitor;
import com.taskmanager.service.NetworkService;
import com.taskmanager.service.StatisticsService;
import com.taskmanager.service.TaskService;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MainView
 * Assembles the master desktop layout: Header, TabPane (5 tabs), and Status Bar.
 *
 * Concurrency Architecture:
 * ─────────────────────────────────────────────────────────────────────
 *  BackgroundTaskManager.generalThreadPool  (fixed 4 threads)
 *    └── NetworkService.fetchOnlineDataAsync() — HTTP requests
 *    └── StatisticsWorker — async statistics calculation
 *
 *  BackgroundTaskManager.scheduledThreadPool  (2 scheduled threads)
 *    └── AutoSaveTask     — every 30 s, saves tasks to disk
 *    └── DeadlineMonitor  — every 10 s, scans for overdue tasks
 * ─────────────────────────────────────────────────────────────────────
 *
 * JavaFX Thread Safety:
 *  All background thread callbacks use Platform.runLater() to
 *  dispatch UI updates to the JavaFX Application Thread safely.
 */
public class MainView extends BorderPane {

    private static final Logger LOGGER = Logger.getLogger(MainView.class.getName());

    private final TaskService taskService;
    private final StatisticsService statisticsService;
    private final NetworkService networkService;
    private final BackgroundTaskManager backgroundTaskManager;   // SHARED thread pool
    private final Stage primaryStage;

    private final TabPane tabPane = new TabPane();
    private DashboardView dashboardView;
    private TaskView taskView;
    private StatisticsView statisticsView;
    private OnlineDataView onlineDataView;
    private AboutView aboutView;

    // Status Bar Labels (updated by background thread callbacks via Platform.runLater)
    private final Label autoSaveStatusLbl  = new Label("Auto-Save: Ready");
    private final Label deadlineStatusLbl  = new Label("Deadlines: Monitoring active");
    private final Label dbStatusLbl        = new Label();

    public MainView(TaskService taskService, StatisticsService statisticsService,
                    NetworkService networkService, BackgroundTaskManager backgroundTaskManager,
                    Stage primaryStage) {
        this.taskService = taskService;
        this.statisticsService = statisticsService;
        this.networkService = networkService;
        this.backgroundTaskManager = backgroundTaskManager;
        this.primaryStage = primaryStage;

        buildUI();
        initBackgroundTasks();
        bindChangeListeners();
    }

    private void buildUI() {
        // ── 1. Top Header ──
        HBox header = new HBox(16);
        header.getStyleClass().add("app-header");
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("Multithreaded Task Management System");
        title.getStyleClass().add("app-title");
        Label subtitle = new Label(
            "JavaFX 17 • SQLite JDBC • Thread Pools • HTTP Networking • JSON Parsing"
        );
        subtitle.getStyleClass().add("app-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button manualSaveBtn = new Button("💾 Save Now");
        manualSaveBtn.getStyleClass().add("btn-secondary");
        manualSaveBtn.setOnAction(e -> handleManualSave());

        header.getChildren().addAll(titleBox, spacer, manualSaveBtn);

        // ── 2. Five Navigation Tabs ──
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Tab 1: Dashboard
        dashboardView = new DashboardView(taskService, statisticsService, primaryStage,
                () -> tabPane.getSelectionModel().select(1));
        Tab dashTab = new Tab("📊 Dashboard", dashboardView);

        // Tab 2: Tasks (CRUD — wired to SQLite)
        taskView = new TaskView(taskService, primaryStage);
        Tab tasksTab = new Tab("📋 Tasks (CRUD)", taskView);

        // Tab 3: Statistics & Concurrency Lab
        statisticsView = new StatisticsView(taskService, statisticsService);
        Tab statsTab = new Tab("📈 Statistics & Lab", statisticsView);

        // Tab 4: Online Data (Networking + JSON Parsing)
        onlineDataView = new OnlineDataView(networkService, backgroundTaskManager);
        Tab onlineTab = new Tab("🌐 Online Data", onlineDataView);

        // Tab 5: About & Viva Notes
        aboutView = new AboutView();
        Tab aboutTab = new Tab("ℹ️ About & Viva", aboutView);

        tabPane.getTabs().addAll(dashTab, tasksTab, statsTab, onlineTab, aboutTab);

        // Refresh view on tab switch
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == dashTab)    dashboardView.refresh();
            else if (newTab == tasksTab)  taskView.refresh();
            else if (newTab == statsTab) statisticsView.refresh();
        });

        VBox topContainer = new VBox();
        topContainer.getChildren().addAll(header, tabPane);
        setTop(topContainer);

        // ── 3. Bottom Status Bar ──
        HBox statusBar = new HBox(16);
        statusBar.getStyleClass().add("status-bar");
        statusBar.setAlignment(Pos.CENTER_LEFT);

        autoSaveStatusLbl.getStyleClass().add("status-label");
        deadlineStatusLbl.getStyleClass().add("status-label");

        String dbMode = taskService.isDbAvailable() ? "✅ SQLite DB" : "📄 JSON File";
        dbStatusLbl.setText("Persistence: " + dbMode);
        dbStatusLbl.setStyle(taskService.isDbAvailable()
                ? "-fx-text-fill: #059669; -fx-font-size: 11px; -fx-font-weight: bold;"
                : "-fx-text-fill: #d97706; -fx-font-size: 11px; -fx-font-weight: bold;");

        Region botSpacer = new Region();
        HBox.setHgrow(botSpacer, Priority.ALWAYS);

        Label jvmLbl = new Label("Java " + System.getProperty("java.version") + " • HotSpot JVM");
        jvmLbl.getStyleClass().add("status-label");

        statusBar.getChildren().addAll(
            autoSaveStatusLbl,
            new Separator(Orientation.VERTICAL),
            deadlineStatusLbl,
            new Separator(Orientation.VERTICAL),
            dbStatusLbl,
            botSpacer,
            jvmLbl
        );
        setBottom(statusBar);
    }

    /**
     * Initializes periodic background tasks using the SHARED ScheduledExecutorService.
     *
     * SCHEDULED THREAD POOL: BackgroundTaskManager.scheduledThreadPool handles:
     *   1. AutoSaveTask  — runs every 30 s to persist task data
     *   2. DeadlineMonitor — runs every 10 s to detect overdue tasks
     *
     * WHY SCHEDULED POOL INSTEAD OF PLAIN THREADS?
     *   A plain Thread.sleep() loop wastes a thread sitting idle between ticks.
     *   ScheduledExecutorService recycles its worker threads between scheduled runs,
     *   is more precise, and provides clean shutdown via awaitTermination().
     */
    private void initBackgroundTasks() {
        // ── SCHEDULED THREAD POOL: Auto-Save every 30 seconds ──
        // AutoSaveTask is a Runnable executed on a scheduled pool thread.
        AutoSaveTask autoSaveTask = new AutoSaveTask(taskService, (success, msg) ->
            // Platform.runLater: callback dispatched safely to JavaFX Application Thread
            autoSaveStatusLbl.setText("Auto-Save: " + (success ? "✓ " : "✗ ") + msg)
        );
        backgroundTaskManager.scheduleWithFixedDelay(autoSaveTask, 15, 30, TimeUnit.SECONDS);

        // ── SCHEDULED THREAD POOL: Deadline Monitor every 10 seconds ──
        DeadlineMonitor deadlineMonitor = new DeadlineMonitor(taskService, (count, list) -> {
            // Platform.runLater: callback dispatched safely to JavaFX Application Thread
            if (count > 0) {
                deadlineStatusLbl.setText("⚠️ Deadlines: " + count + " overdue!");
                deadlineStatusLbl.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
            } else {
                deadlineStatusLbl.setText("✓ Deadlines: All on schedule");
                deadlineStatusLbl.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
            }
        });
        backgroundTaskManager.scheduleWithFixedDelay(deadlineMonitor, 2, 10, TimeUnit.SECONDS);

        LOGGER.info("Background scheduled tasks initialized (AutoSave 30s, DeadlineMonitor 10s).");
    }

    /**
     * Registers a change listener on TaskService so all views refresh on any CRUD change.
     * The listener uses Platform.runLater() to ensure UI updates happen on the FX thread.
     */
    private void bindChangeListeners() {
        taskService.addChangeListener(() -> Platform.runLater(() -> {
            dashboardView.refresh();
            taskView.refresh();
            statisticsView.refresh();
        }));
    }

    private void handleManualSave() {
        try {
            taskService.save();
            autoSaveStatusLbl.setText("Manual Save: ✓ Saved at "
                    + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(primaryStage);
            alert.setTitle("Save Error");
            alert.setHeaderText("Unable to save tasks");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Gracefully shuts down StatisticsView's cached thread pool.
     * The BackgroundTaskManager is shut down in MainApp.stop().
     */
    public void shutdown() {
        LOGGER.info("MainView shutdown: terminating StatisticsView background pool.");
        if (statisticsView != null) statisticsView.shutdown();
    }
}
