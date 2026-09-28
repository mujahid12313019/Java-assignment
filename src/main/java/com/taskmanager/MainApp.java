package com.taskmanager;

import com.taskmanager.service.FileStorageService;
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
 * Primary JavaFX Application entry point managing application lifecycle.
 *
 * Demonstrates:
 * - Application Lifecycle (init, start, stop)
 * - Safe data loading at startup
 * - Reliable data persistence on application close
 * - Clean background thread termination
 */
public class MainApp extends Application {

    private static final Logger LOGGER = Logger.getLogger(MainApp.class.getName());

    private TaskService taskService;
    private StatisticsService statisticsService;
    private MainView mainView;

    @Override
    public void init() throws Exception {
        super.init();
        LOGGER.info("1. Initializing services and loading data...");

        // Initialize core services
        FileStorageService storageService = new FileStorageService("data/tasks.json");
        taskService = new TaskService(storageService);
        statisticsService = new StatisticsService();

        // 1. Load saved tasks from disk at startup
        try {
            taskService.load();
            LOGGER.info("Tasks loaded successfully. Total tasks: " + taskService.getTaskCount());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load existing tasks. Starting fresh.", e);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        LOGGER.info("2. Constructing JavaFX UI and launching background monitors...");

        // 2 & 3. Construct Main UI and initialize scheduled services
        mainView = new MainView(taskService, statisticsService, primaryStage);

        Scene scene = new Scene(mainView, 1080, 720);

        // Load CSS stylesheet if available (with graceful fallback)
        try {
            URL cssUrl = getClass().getResource("/css/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
                LOGGER.info("CSS stylesheet loaded successfully.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "CSS loading skipped or failed. Falling back to default styling.", e);
        }

        // Configure Primary Stage Window
        primaryStage.setTitle("Multithreaded Desktop Task Management System (JavaFX)");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);

        // Intercept close request to guarantee clean shutdown
        primaryStage.setOnCloseRequest(event -> {
            LOGGER.info("Window close requested. Initiating clean shutdown...");
        });

        // 4 & 5. Display dashboard and allow user interaction
        primaryStage.show();
        LOGGER.info("Application started successfully.");
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        LOGGER.info("Executing Application.stop() cleanup sequence...");

        // 1. Save current task data before exit
        if (taskService != null) {
            try {
                taskService.save();
                LOGGER.info("Tasks successfully saved to disk before exit.");
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to save tasks on exit.", e);
            }
        }

        // 2. Shut down all background threads and executors
        if (mainView != null) {
            mainView.shutdown();
            LOGGER.info("All background executors terminated.");
        }

        LOGGER.info("Clean shutdown complete.");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
