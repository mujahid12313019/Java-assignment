package com.taskmanager.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;

/**
 * AboutView
 * Displays academic project metadata, syllabus alignment, and architectural principles.
 *
 * Demonstrates:
 * - Clean informational layout
 * - Educational viva references embedded into the software interface
 */
public class AboutView extends ScrollPane {

    public AboutView() {
        setFitToWidth(true);
        setStyle("-fx-background-color: transparent;");

        VBox content = new VBox(16);
        content.getStyleClass().add("view-container");
        buildUI(content);
        setContent(content);
    }

    private void buildUI(VBox root) {
        // Card 1: Project Identity & Assignment Metadata
        VBox metaCard = new VBox(10);
        metaCard.getStyleClass().add("card");

        Label projTitle = new Label("Multithreaded Desktop Task Management System Using JavaFX");
        projTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e3c72;");

        Label projSub = new Label("Undergraduate Computer Science & Engineering Capstone Assignment");
        projSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        Separator sep1 = new Separator();

        GridPane studentGrid = new GridPane();
        studentGrid.setHgap(20);
        studentGrid.setVgap(8);
        studentGrid.setPadding(new Insets(6, 0, 6, 0));

        studentGrid.add(new Label("Student Name:"), 0, 0);
        Label nameVal = new Label("[Your Name Here]");
        nameVal.setStyle("-fx-font-weight: bold;");
        studentGrid.add(nameVal, 1, 0);

        studentGrid.add(new Label("Student ID:"), 0, 1);
        Label idVal = new Label("[Your Student ID]");
        idVal.setStyle("-fx-font-weight: bold;");
        studentGrid.add(idVal, 1, 1);

        studentGrid.add(new Label("Course:"), 2, 0);
        studentGrid.add(new Label("Advanced Object-Oriented Programming (Java)"), 3, 0);

        studentGrid.add(new Label("Submission Date:"), 2, 1);
        studentGrid.add(new Label("September 2026"), 3, 1);

        metaCard.getChildren().addAll(projTitle, projSub, sep1, studentGrid);

        // Card 2: Technologies & System Architecture
        VBox techCard = new VBox(12);
        techCard.getStyleClass().add("card");

        Label techTitle = new Label("Technologies & Frameworks");
        techTitle.getStyleClass().add("card-title");

        GridPane techGrid = new GridPane();
        techGrid.setHgap(24);
        techGrid.setVgap(10);

        techGrid.add(new Label("Core Language:"), 0, 0);
        techGrid.add(new Label("Java 17 LTS (Modern syntax, records, lambdas, streams)"), 1, 0);

        techGrid.add(new Label("GUI Framework:"), 0, 1);
        techGrid.add(new Label("JavaFX 17 (OpenJFX controls, responsive layouts, data binding)"), 1, 1);

        techGrid.add(new Label("Concurrency:"), 0, 2);
        techGrid.add(new Label("ScheduledExecutorService, ReentrantReadWriteLock, Platform.runLater"), 1, 2);

        techGrid.add(new Label("Persistence:"), 0, 3);
        techGrid.add(new Label("JSON file-based storage with Jackson Databind & JavaTime module"), 1, 3);

        techGrid.add(new Label("Build & VCS:"), 0, 4);
        techGrid.add(new Label("Apache Maven 3 & Git / GitHub version control"), 1, 4);

        techCard.getChildren().addAll(techTitle, techGrid);

        // Card 3: JVM, JRE, JDK Architectural Concepts (Viva Quick-Study)
        VBox conceptsCard = new VBox(12);
        conceptsCard.getStyleClass().add("card");

        Label concTitle = new Label("Core Architectural Concepts (JVM, JRE, JDK)");
        concTitle.getStyleClass().add("card-title");

        VBox jvmBox = new VBox(4);
        Label jvmHdr = new Label("1. Java Virtual Machine (JVM)");
        jvmHdr.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e3c72;");
        Label jvmDesc = new Label("The abstract execution engine that loads, verifies, and executes platform-independent bytecode (.class files). Uses an Interpreter and HotSpot JIT compiler to produce native machine code at runtime.");
        jvmDesc.setWrapText(true);
        jvmBox.getChildren().addAll(jvmHdr, jvmDesc);

        VBox jreBox = new VBox(4);
        Label jreHdr = new Label("2. Java Runtime Environment (JRE)");
        jreHdr.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e3c72;");
        Label jreDesc = new Label("The runtime package comprising the JVM, Java Core Class Libraries (java.base, java.desktop), and supporting runtime configurations required to execute compiled Java programs.");
        jreDesc.setWrapText(true);
        jreBox.getChildren().addAll(jreHdr, jreDesc);

        VBox jdkBox = new VBox(4);
        Label jdkHdr = new Label("3. Java Development Kit (JDK)");
        jdkHdr.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e3c72;");
        Label jdkDesc = new Label("The superset development suite providing everything in the JRE plus development tools including javac (compiler), javap (disassembler), javadoc, jdb (debugger), and jar archiver.");
        jdkDesc.setWrapText(true);
        jdkBox.getChildren().addAll(jdkHdr, jdkDesc);

        conceptsCard.getChildren().addAll(concTitle, jvmBox, jreBox, jdkBox);

        // Card 4: Multithreading Rules & Thread Safety
        VBox threadingCard = new VBox(10);
        threadingCard.getStyleClass().add("card");

        Label thrTitle = new Label("JavaFX UI Thread Rule & Concurrency Architecture");
        thrTitle.getStyleClass().add("card-title");

        Label thrDesc = new Label(
                "• JavaFX enforces a Single-Threaded UI Model: all GUI mutations must occur on the JavaFX Application Thread.\n" +
                "• Background tasks (AutoSaveTask, DeadlineMonitor, StatisticsWorker) run asynchronously using ScheduledExecutorService.\n" +
                "• When background threads finish or produce UI updates, they invoke Platform.runLater(Runnable) to safely dispatch changes.\n" +
                "• TaskService employs ReentrantReadWriteLock to allow multiple concurrent background readers while ensuring mutually exclusive writes."
        );
        thrDesc.setWrapText(true);
        thrDesc.setStyle("-fx-line-spacing: 4px;");

        threadingCard.getChildren().addAll(thrTitle, thrDesc);

        root.getChildren().addAll(metaCard, techCard, conceptsCard, threadingCard);
    }
}
