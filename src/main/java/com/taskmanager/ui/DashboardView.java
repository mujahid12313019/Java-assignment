package com.taskmanager.ui;

import com.taskmanager.model.Task;
import com.taskmanager.model.TaskPriority;
import com.taskmanager.model.TaskStatistics;
import com.taskmanager.model.TaskStatus;
import com.taskmanager.service.StatisticsService;
import com.taskmanager.service.TaskService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * DashboardView
 * Provides an executive overview of all task activities and high-level KPIs.
 *
 * Demonstrates:
 * - JavaFX layout containers (VBox, HBox, Priority growth)
 * - Custom metric KPI cards
 * - Real-time synchronization with background statistics updates
 */
public class DashboardView extends VBox {

    private final TaskService taskService;
    private final StatisticsService statisticsService;
    private final Stage primaryStage;
    private final Runnable navigateToTasksCallback;

    // KPI Card Value Labels
    private final Label totalValueLabel = new Label("0");
    private final Label pendingValueLabel = new Label("0");
    private final Label inProgressValueLabel = new Label("0");
    private final Label completedValueLabel = new Label("0");
    private final Label overdueValueLabel = new Label("0");

    // Urgent Tasks Table
    private final TableView<Task> urgentTable = new TableView<>();

    public DashboardView(TaskService taskService, StatisticsService statisticsService,
                         Stage primaryStage, Runnable navigateToTasksCallback) {
        this.taskService = taskService;
        this.statisticsService = statisticsService;
        this.primaryStage = primaryStage;
        this.navigateToTasksCallback = navigateToTasksCallback;

        getStyleClass().add("view-container");
        setSpacing(20);

        buildUI();
        refresh();
    }

    private void buildUI() {
        // 1. Top Section: 5 Metric Cards in HBox
        HBox kpiContainer = new HBox(14);
        kpiContainer.setAlignment(Pos.CENTER);

        VBox totalCard = createKpiCard("TOTAL TASKS", totalValueLabel, "metric-card-total", "All managed tasks");
        VBox pendingCard = createKpiCard("PENDING", pendingValueLabel, "metric-card-pending", "Awaiting start");
        VBox inProgressCard = createKpiCard("IN PROGRESS", inProgressValueLabel, "metric-card-inprogress", "Currently active");
        VBox completedCard = createKpiCard("COMPLETED", completedValueLabel, "metric-card-completed", "Successfully closed");
        VBox overdueCard = createKpiCard("OVERDUE", overdueValueLabel, "metric-card-overdue", "Passed deadline");

        HBox.setHgrow(totalCard, Priority.ALWAYS);
        HBox.setHgrow(pendingCard, Priority.ALWAYS);
        HBox.setHgrow(inProgressCard, Priority.ALWAYS);
        HBox.setHgrow(completedCard, Priority.ALWAYS);
        HBox.setHgrow(overdueCard, Priority.ALWAYS);

        kpiContainer.getChildren().addAll(totalCard, pendingCard, inProgressCard, completedCard, overdueCard);

        // 2. Middle Section: Overdue & High Priority Tasks
        VBox urgentSection = new VBox(10);
        urgentSection.getStyleClass().add("card");
        VBox.setVgrow(urgentSection, Priority.ALWAYS);

        HBox urgentHeader = new HBox(10);
        urgentHeader.setAlignment(Pos.CENTER_LEFT);
        Label sectionTitle = new Label("Urgent & Overdue Attention Required");
        sectionTitle.getStyleClass().add("card-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addTaskBtn = new Button("+ Add New Task");
        addTaskBtn.getStyleClass().add("btn-success");
        addTaskBtn.setOnAction(e -> handleAddTask());

        Button viewAllBtn = new Button("View All Tasks ->");
        viewAllBtn.getStyleClass().add("btn-secondary");
        viewAllBtn.setOnAction(e -> {
            if (navigateToTasksCallback != null) {
                navigateToTasksCallback.run();
            }
        });

        urgentHeader.getChildren().addAll(sectionTitle, spacer, addTaskBtn, viewAllBtn);

        setupUrgentTable();
        VBox.setVgrow(urgentTable, Priority.ALWAYS);

        urgentSection.getChildren().addAll(urgentHeader, urgentTable);

        getChildren().addAll(kpiContainer, urgentSection);
    }

    private VBox createKpiCard(String title, Label valueLabel, String styleClass, String subtitle) {
        VBox card = new VBox(6);
        card.getStyleClass().addAll("card", styleClass);
        card.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #64748b;");

        valueLabel.getStyleClass().add("card-value");

        Label subLabel = new Label(subtitle);
        subLabel.getStyleClass().add("card-subtitle");

        card.getChildren().addAll(titleLabel, valueLabel, subLabel);
        return card;
    }

    @SuppressWarnings("unchecked")
    private void setupUrgentTable() {
        urgentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        urgentTable.setPlaceholder(new Label("No urgent or overdue tasks! You're all caught up."));

        TableColumn<Task, String> idCol = new TableColumn<>("ID");
        idCol.setPrefWidth(90);
        idCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));

        TableColumn<Task, String> titleCol = new TableColumn<>("Title");
        titleCol.setPrefWidth(260);
        titleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));

        TableColumn<Task, String> priorityCol = new TableColumn<>("Priority");
        priorityCol.setPrefWidth(110);
        priorityCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPriority().getDisplayName()));
        priorityCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().addAll("badge", "badge-" + item.toLowerCase());
                    setGraphic(badge);
                }
            }
        });

        TableColumn<Task, String> deadlineCol = new TableColumn<>("Deadline");
        deadlineCol.setPrefWidth(120);
        deadlineCol.setCellValueFactory(data -> {
            LocalDate d = data.getValue().getDeadline();
            return new SimpleStringProperty(d != null ? d.format(DateTimeFormatter.ISO_LOCAL_DATE) : "None");
        });

        TableColumn<Task, String> alertCol = new TableColumn<>("Urgency Status");
        alertCol.setPrefWidth(140);
        alertCol.setCellValueFactory(data -> {
            Task t = data.getValue();
            if (t.isOverdue()) {
                return new SimpleStringProperty("OVERDUE");
            } else if (t.getDeadline() != null && t.getDeadline().equals(LocalDate.now())) {
                return new SimpleStringProperty("DUE TODAY");
            } else {
                return new SimpleStringProperty("HIGH PRIORITY");
            }
        });
        alertCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().addAll("badge", item.contains("OVERDUE") ? "badge-overdue" : "badge-high");
                    setGraphic(badge);
                }
            }
        });

        urgentTable.getColumns().addAll(idCol, titleCol, priorityCol, deadlineCol, alertCol);
    }

    private void handleAddTask() {
        Optional<Task> result = TaskDialog.showDialog(primaryStage, null);
        result.ifPresent(task -> {
            taskService.addTask(task.getTitle(), task.getDescription(), task.getPriority(),
                    task.getStatus(), task.getDeadline());
            refresh();
        });
    }

    /**
     * Refreshes KPIs and urgent task table.
     */
    public void refresh() {
        List<Task> tasks = taskService.getAllTasks();
        TaskStatistics stats = statisticsService.calculateStatistics(tasks);

        updateStatistics(stats);

        // Filter urgent tasks: overdue OR (high priority AND not completed)
        List<Task> urgentList = tasks.stream()
                .filter(t -> !t.isCompleted() && (t.isOverdue() || t.getPriority() == TaskPriority.HIGH))
                .toList();

        urgentTable.setItems(FXCollections.observableArrayList(urgentList));
    }

    public void updateStatistics(TaskStatistics stats) {
        if (stats == null) return;
        totalValueLabel.setText(String.valueOf(stats.getTotalTasks()));
        pendingValueLabel.setText(String.valueOf(stats.getPendingCount()));
        inProgressValueLabel.setText(String.valueOf(stats.getInProgressCount()));
        completedValueLabel.setText(String.valueOf(stats.getCompletedCount()));
        overdueValueLabel.setText(String.valueOf(stats.getOverdueCount()));
    }
}
