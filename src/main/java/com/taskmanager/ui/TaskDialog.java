package com.taskmanager.ui;

import com.taskmanager.model.Task;
import com.taskmanager.model.TaskPriority;
import com.taskmanager.model.TaskStatus;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.Optional;

/**
 * TaskDialog
 * A reusable modal form for Adding and Editing tasks.
 *
 * Demonstrates:
 * - JavaFX Dialog API & Event Handling
 * - Input validation with user-friendly alerts
 * - Two-way data binding between UI inputs and the Task domain model
 */
public class TaskDialog extends Dialog<Task> {

    private final TextField titleField = new TextField();
    private final TextArea descriptionArea = new TextArea();
    private final ComboBox<TaskPriority> priorityComboBox = new ComboBox<>();
    private final ComboBox<TaskStatus> statusComboBox = new ComboBox<>();
    private final DatePicker deadlinePicker = new DatePicker();

    private final Task existingTask;

    /**
     * @param owner The parent Stage window.
     * @param taskToEdit The task being edited, or null if creating a new task.
     */
    public TaskDialog(Stage owner, Task taskToEdit) {
        this.existingTask = taskToEdit;

        initOwner(owner);
        initModality(Modality.WINDOW_MODAL);
        setTitle(taskToEdit == null ? "Add New Task" : "Edit Task: " + taskToEdit.getId());
        setHeaderText(taskToEdit == null ? "Create a new task with priority and deadline" : "Modify existing task details");

        // Set Dialog Buttons
        ButtonType saveButtonType = new ButtonType(taskToEdit == null ? "Create" : "Save Changes", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Populate ComboBox options
        priorityComboBox.getItems().setAll(TaskPriority.values());
        priorityComboBox.setValue(TaskPriority.MEDIUM);

        statusComboBox.getItems().setAll(TaskStatus.values());
        statusComboBox.setValue(TaskStatus.PENDING);

        descriptionArea.setPrefRowCount(3);
        descriptionArea.setWrapText(true);

        // Layout Form in a GridPane
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);
        grid.setPadding(new Insets(20, 20, 10, 20));

        grid.add(new Label("Title *:"), 0, 0);
        grid.add(titleField, 1, 0);
        titleField.setPromptText("Enter task title (e.g. Finish lab report)");

        grid.add(new Label("Description:"), 0, 1);
        grid.add(descriptionArea, 1, 1);
        descriptionArea.setPromptText("Enter task details or instructions");

        grid.add(new Label("Priority:"), 0, 2);
        grid.add(priorityComboBox, 1, 2);

        grid.add(new Label("Status:"), 0, 3);
        grid.add(statusComboBox, 1, 3);

        grid.add(new Label("Deadline:"), 0, 4);
        grid.add(deadlinePicker, 1, 4);
        deadlinePicker.setPromptText("Select target deadline");

        getDialogPane().setContent(grid);

        // If editing, populate current values
        if (taskToEdit != null) {
            titleField.setText(taskToEdit.getTitle());
            descriptionArea.setText(taskToEdit.getDescription());
            priorityComboBox.setValue(taskToEdit.getPriority());
            statusComboBox.setValue(taskToEdit.getStatus());
            deadlinePicker.setValue(taskToEdit.getDeadline());
        }

        // Apply custom stylesheet
        if (owner != null && owner.getScene() != null) {
            getDialogPane().getStylesheets().addAll(owner.getScene().getStylesheets());
        }

        // Validation on Save button click
        final Button saveBtn = (Button) getDialogPane().lookupButton(saveButtonType);
        saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String title = titleField.getText();
            if (title == null || title.trim().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.initOwner(owner);
                alert.setTitle("Validation Warning");
                alert.setHeaderText("Required Field Missing");
                alert.setContentText("Task title cannot be empty. Please enter a meaningful title.");
                alert.showAndWait();
                event.consume(); // Prevent dialog from closing
            }
        });

        // Convert UI inputs into a Task entity
        setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String title = titleField.getText().trim();
                String desc = descriptionArea.getText() != null ? descriptionArea.getText().trim() : "";
                TaskPriority priority = priorityComboBox.getValue();
                TaskStatus status = statusComboBox.getValue();
                LocalDate deadline = deadlinePicker.getValue();

                if (existingTask != null) {
                    existingTask.setTitle(title);
                    existingTask.setDescription(desc);
                    existingTask.setPriority(priority);
                    existingTask.setStatus(status);
                    existingTask.setDeadline(deadline);
                    return existingTask;
                } else {
                    return new Task(null, title, desc, priority, status, null, deadline, status == TaskStatus.COMPLETED);
                }
            }
            return null;
        });
    }

    public static Optional<Task> showDialog(Stage owner, Task taskToEdit) {
        TaskDialog dialog = new TaskDialog(owner, taskToEdit);
        return dialog.showAndWait();
    }
}
