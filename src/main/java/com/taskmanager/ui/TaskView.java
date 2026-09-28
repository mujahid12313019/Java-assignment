package com.taskmanager.ui;

import com.taskmanager.model.Task;
import com.taskmanager.model.TaskPriority;
import com.taskmanager.model.TaskStatus;
import com.taskmanager.service.TaskService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * TaskView
 * Core task management interface supporting searching, filtering, and full CRUD operations.
 *
 * Demonstrates:
 * - TableView with custom cell factories and styled status badges
 * - Real-time stream-based search and multi-criteria filtering
 * - User interaction handling (Edit, Complete, Delete confirmation)
 * - Safe JavaFX UI state synchronization
 */
public class TaskView extends VBox {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final TaskService taskService;
    private final Stage primaryStage;

    private final TableView<Task> taskTable = new TableView<>();
    private final ObservableList<Task> tableData = FXCollections.observableArrayList();

    // Filter Controls
    private final TextField searchField = new TextField();
    private final ComboBox<String> statusFilterBox = new ComboBox<>();
    private final ComboBox<String> priorityFilterBox = new ComboBox<>();

    // Action Buttons
    private final Button editBtn = new Button("Edit Task");
    private final Button completeBtn = new Button("Mark Completed");
    private final Button deleteBtn = new Button("Delete Task");

    public TaskView(TaskService taskService, Stage primaryStage) {
        this.taskService = taskService;
        this.primaryStage = primaryStage;

        getStyleClass().add("view-container");
        setSpacing(14);

        buildUI();
        refresh();
    }

    private void buildUI() {
        // 1. Top Search and Filter Bar
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.getStyleClass().addAll("card", "filter-bar");

        searchField.setPromptText("🔍 Search by title, description, or ID...");
        searchField.setPrefWidth(280);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        Label statusLbl = new Label("Status:");
        statusFilterBox.getItems().setAll("ALL", "PENDING", "IN_PROGRESS", "COMPLETED");
        statusFilterBox.setValue("ALL");
        statusFilterBox.setOnAction(e -> applyFilters());

        Label priorityLbl = new Label("Priority:");
        priorityFilterBox.getItems().setAll("ALL", "LOW", "MEDIUM", "HIGH");
        priorityFilterBox.setValue("ALL");
        priorityFilterBox.setOnAction(e -> applyFilters());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBtn = new Button("+ Add Task");
        addBtn.getStyleClass().add("btn-success");
        addBtn.setOnAction(e -> handleAddTask());

        filterBar.getChildren().addAll(searchField, statusLbl, statusFilterBox, priorityLbl, priorityFilterBox, spacer, addBtn);

        // 2. Center TableView
        setupTaskTable();
        VBox.setVgrow(taskTable, Priority.ALWAYS);

        // 3. Bottom Action Bar
        HBox actionBar = new HBox(12);
        actionBar.setAlignment(Pos.CENTER_LEFT);

        editBtn.getStyleClass().add("btn-secondary");
        editBtn.setDisable(true);
        editBtn.setOnAction(e -> handleEditTask());

        completeBtn.getStyleClass().add("btn-success");
        completeBtn.setDisable(true);
        completeBtn.setOnAction(e -> handleCompleteTask());

        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setDisable(true);
        deleteBtn.setOnAction(e -> handleDeleteTask());

        Region botSpacer = new Region();
        HBox.setHgrow(botSpacer, Priority.ALWAYS);

        Label countLabel = new Label();
        countLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
        tableData.addListener((javafx.collections.ListChangeListener<Task>) c ->
                countLabel.setText("Showing " + tableData.size() + " task(s)"));

        actionBar.getChildren().addAll(editBtn, completeBtn, deleteBtn, botSpacer, countLabel);

        // Selection Listener to enable/disable action buttons
        taskTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            boolean hasSelection = newSel != null;
            editBtn.setDisable(!hasSelection);
            deleteBtn.setDisable(!hasSelection);
            completeBtn.setDisable(!hasSelection || newSel.isCompleted());
        });

        // Double-click row to edit
        taskTable.setRowFactory(tv -> {
            TableRow<Task> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    handleEditTask();
                }
            });
            return row;
        });

        getChildren().addAll(filterBar, taskTable, actionBar);
    }

    @SuppressWarnings("unchecked")
    private void setupTaskTable() {
        taskTable.setItems(tableData);
        taskTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        taskTable.setPlaceholder(new Label("No tasks found matching your filter criteria."));

        TableColumn<Task, String> idCol = new TableColumn<>("ID");
        idCol.setPrefWidth(90);
        idCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));

        TableColumn<Task, String> titleCol = new TableColumn<>("Title");
        titleCol.setPrefWidth(200);
        titleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));

        TableColumn<Task, String> descCol = new TableColumn<>("Description");
        descCol.setPrefWidth(220);
        descCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));

        TableColumn<Task, String> priorityCol = new TableColumn<>("Priority");
        priorityCol.setPrefWidth(95);
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

        TableColumn<Task, String> statusCol = new TableColumn<>("Status");
        statusCol.setPrefWidth(110);
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().getDisplayName()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    String style = switch (item.toLowerCase()) {
                        case "completed" -> "badge-completed";
                        case "in progress" -> "badge-inprogress";
                        default -> "badge-pending";
                    };
                    badge.getStyleClass().addAll("badge", style);
                    setGraphic(badge);
                }
            }
        });

        TableColumn<Task, String> deadlineCol = new TableColumn<>("Deadline");
        deadlineCol.setPrefWidth(125);
        deadlineCol.setCellValueFactory(data -> {
            LocalDate d = data.getValue().getDeadline();
            return new SimpleStringProperty(d != null ? d.format(DATE_FORMAT) : "No Deadline");
        });
        deadlineCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    Task task = getTableRow() != null ? getTableRow().getItem() : null;
                    if (task != null && task.isOverdue()) {
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #334155;");
                    }
                }
            }
        });

        TableColumn<Task, String> createdCol = new TableColumn<>("Created");
        createdCol.setPrefWidth(130);
        createdCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getCreatedDateTime() != null ? data.getValue().getCreatedDateTime().format(DATE_TIME_FORMAT) : ""));

        taskTable.getColumns().addAll(idCol, titleCol, descCol, priorityCol, statusCol, deadlineCol, createdCol);
    }

    private void handleAddTask() {
        Optional<Task> result = TaskDialog.showDialog(primaryStage, null);
        result.ifPresent(task -> {
            taskService.addTask(
                    task.getTitle(),
                    task.getDescription(),
                    task.getPriority(),
                    task.getStatus(),
                    task.getDeadline()
            );
            refresh();
        });
    }

    private void handleEditTask() {
        Task selected = taskTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Optional<Task> result = TaskDialog.showDialog(primaryStage, selected);
        result.ifPresent(task -> {
            taskService.updateTask(
                    task.getId(),
                    task.getTitle(),
                    task.getDescription(),
                    task.getPriority(),
                    task.getStatus(),
                    task.getDeadline()
            );
            refresh();
        });
    }

    private void handleCompleteTask() {
        Task selected = taskTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        taskService.markTaskCompleted(selected.getId());
        refresh();
    }

    private void handleDeleteTask() {
        Task selected = taskTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(primaryStage);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete Task " + selected.getId() + "?");
        alert.setContentText("Are you sure you want to permanently delete '" + selected.getTitle() + "'?");

        Optional<ButtonType> response = alert.showAndWait();
        if (response.isPresent() && response.get() == ButtonType.OK) {
            taskService.deleteTask(selected.getId());
            refresh();
        }
    }

    private void applyFilters() {
        String keyword = searchField.getText();

        String statusStr = statusFilterBox.getValue();
        TaskStatus statusFilter = ("ALL".equalsIgnoreCase(statusStr) || statusStr == null)
                ? null
                : TaskStatus.valueOf(statusStr);

        String priorityStr = priorityFilterBox.getValue();
        TaskPriority priorityFilter = ("ALL".equalsIgnoreCase(priorityStr) || priorityStr == null)
                ? null
                : TaskPriority.valueOf(priorityStr);

        List<Task> filtered = taskService.filterAndSearch(keyword, statusFilter, priorityFilter);
        tableData.setAll(filtered);
    }

    public void refresh() {
        applyFilters();
    }
}
