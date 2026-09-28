package com.taskmanager;

import com.taskmanager.concurrency.BackgroundTaskManager;
import com.taskmanager.dao.TaskDAO;
import com.taskmanager.database.DatabaseConnection;
import com.taskmanager.database.DatabaseInitializer;
import com.taskmanager.service.FileStorageService;
import com.taskmanager.service.NetworkService;
import com.taskmanager.service.StatisticsService;
import com.taskmanager.service.TaskService;
import com.taskmanager.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MainApp
 * Primary JavaFX Application entry point managing the full application lifecycle.
 *
 * Lifecycle:
 *   init()  → Initialize SQLite DB, DAOs, Services, Thread Pool
 *   start() → Build JavaFX Scene, wire up UI
 *   stop()  → Save data, shutdown thread pools gracefully
 *
 * Demonstrates:
 * - Application Lifecycle (init, start, stop)
 * - SQLite database initialization and JDBC wiring
 * - BackgroundTaskManager (thread pool) creation and shutdown
 * - Dependency injection of services into UI layer
 */
public class MainApp extends Application {

    private static final Logger LOGGER = Logger.getLogger(MainApp.class.getName());

    private TaskService taskService;
    private StatisticsService statisticsService;
    private NetworkService networkService;
    private BackgroundTaskManager backgroundTaskManager;
    private MainView mainView;

    @Override
    public void init() throws Exception {
        super.init();
        LOGGER.info("=== Application init() — Initializing Services and Database ===");

        // ─── 1. Initialize SQLite Database (creates file + schema if not exists) ───
        DatabaseConnection dbConnection = new DatabaseConnection(); // data/taskmanager.db
        DatabaseInitializer dbInitializer = new DatabaseInitializer(dbConnection);
        try {
            dbInitializer.initialize();
            LOGGER.info("SQLite database initialized: " + dbConnection.getDbUrl());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "SQLite init failed — will fall back to JSON storage", e);
        }

        // ─── 2. Create TaskDAO wired to SQLite ───
        TaskDAO taskDAO = new TaskDAO(dbConnection);

        // ─── 3. Initialize Services ───
        FileStorageService fileStorage = new FileStorageService("data/tasks.json");
        taskService = new TaskService(taskDAO, fileStorage);
        statisticsService = new StatisticsService();
        networkService = new NetworkService();

        // ─── 4. THREAD POOL: Create BackgroundTaskManager (fixed pool + scheduled pool) ───
        // This single manager owns ALL background concurrency in the application.
        backgroundTaskManager = new BackgroundTaskManager();

        // ─── 5. Load tasks from SQLite into in-memory cache ───
        try {
            taskService.load();
            LOGGER.info("Tasks loaded successfully. Total: " + taskService.getTaskCount()
                    + " | Source: " + (taskService.isDbAvailable() ? "SQLite DB" : "JSON File"));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load tasks. Starting with empty list.", e);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        LOGGER.info("=== Application start() — Constructing JavaFX Scene ===");

        // Build Main UI with all injected services and the shared thread pool manager
        mainView = new MainView(taskService, statisticsService, networkService,
                backgroundTaskManager, primaryStage);

        Scene scene = new Scene(mainView, 1100, 740);

        // Load CSS stylesheet
        try {
            URL cssUrl = getClass().getResource("/css/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
                LOGGER.info("CSS loaded: " + cssUrl);
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "CSS loading skipped.", e);
        }

        primaryStage.setTitle("Multithreaded Desktop Task Management System — JavaFX + SQLite");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(620);

        primaryStage.setOnCloseRequest(event ->
                LOGGER.info("Close requested — stop() will execute cleanup."));

        primaryStage.show();
        LOGGER.info("Application window displayed.");
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        LOGGER.info("=== Application stop() — Clean Shutdown Sequence ===");

        // 1. Save tasks (JSON backup; SQLite already persistent)
        if (taskService != null) {
            try {
                taskService.save();
                LOGGER.info("Task persistence completed on shutdown.");
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to save tasks on exit.", e);
            }
        }

        // 2. Shutdown JavaFX UI background threads (ScheduledExecutorService in MainView)
        if (mainView != null) {
            mainView.shutdown();
            LOGGER.info("MainView background executors terminated.");
        }

        // 3. Shutdown shared BackgroundTaskManager thread pools
        if (backgroundTaskManager != null) {
            backgroundTaskManager.shutdown();
            LOGGER.info("BackgroundTaskManager thread pools terminated.");
        }

        LOGGER.info("=== Clean shutdown complete ===");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
