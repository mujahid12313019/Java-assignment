package com.taskmanager.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * AboutView
 * Displays full project metadata, technology stack, and viva reference notes.
 *
 * Updated to reflect:
 * - SQLite database integration (JDBC, PreparedStatement, FK, CRUD)
 * - Real HTTP networking (HttpClient, JSON parsing, Jackson)
 * - Thread pool concurrency architecture
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

        // ══════════════════════════════════════════════════════════════
        // CARD 1: Project Identity
        // ══════════════════════════════════════════════════════════════
        VBox metaCard = new VBox(10);
        metaCard.getStyleClass().add("card");

        Label projTitle = new Label("Multithreaded Desktop Task Management System Using JavaFX");
        projTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e3c72;");

        Label projSub = new Label("Undergraduate Computer Science & Engineering Assignment");
        projSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        Separator sep1 = new Separator();

        GridPane studentGrid = new GridPane();
        studentGrid.setHgap(20); studentGrid.setVgap(8);
        studentGrid.setPadding(new Insets(6, 0, 6, 0));

        studentGrid.add(new Label("Student Name:"),   0, 0);
        Label nameVal = new Label("[Your Name Here]");
        nameVal.setStyle("-fx-font-weight: bold;");
        studentGrid.add(nameVal, 1, 0);

        studentGrid.add(new Label("Student ID:"),     0, 1);
        Label idVal = new Label("[Your Student ID]");
        idVal.setStyle("-fx-font-weight: bold;");
        studentGrid.add(idVal, 1, 1);

        studentGrid.add(new Label("Course:"),         2, 0);
        studentGrid.add(new Label("Advanced Object-Oriented Programming (Java)"), 3, 0);

        studentGrid.add(new Label("Submission:"),     2, 1);
        studentGrid.add(new Label("September 2026"), 3, 1);

        metaCard.getChildren().addAll(projTitle, projSub, sep1, studentGrid);

        // ══════════════════════════════════════════════════════════════
        // CARD 2: Technology Stack
        // ══════════════════════════════════════════════════════════════
        VBox techCard = new VBox(12);
        techCard.getStyleClass().add("card");

        Label techTitle = new Label("Technology Stack & Architecture");
        techTitle.getStyleClass().add("card-title");

        GridPane techGrid = new GridPane();
        techGrid.setHgap(24); techGrid.setVgap(10);

        techGrid.add(bold("Core Language:"),     0, 0);
        techGrid.add(new Label("Java 17 LTS (lambdas, streams, records, text blocks)"), 1, 0);

        techGrid.add(bold("GUI Framework:"),     0, 1);
        techGrid.add(new Label("JavaFX 17 (OpenJFX) — controls, layouts, TableView, data binding"), 1, 1);

        techGrid.add(bold("Database:"),          0, 2);
        techGrid.add(new Label("SQLite 3 via JDBC (sqlite-jdbc 3.45) — tables: users, tasks, categories"), 1, 2);

        techGrid.add(bold("Concurrency:"),       0, 3);
        techGrid.add(new Label("ExecutorService (fixed pool), ScheduledExecutorService, ReentrantReadWriteLock, Platform.runLater"), 1, 3);

        techGrid.add(bold("Networking:"),        0, 4);
        techGrid.add(new Label("java.net.http.HttpClient — HTTP GET to public JSON API"), 1, 4);

        techGrid.add(bold("JSON Parsing:"),      0, 5);
        techGrid.add(new Label("Jackson ObjectMapper — parses JSON to OnlineData Java object"), 1, 5);

        techGrid.add(bold("Build & VCS:"),       0, 6);
        techGrid.add(new Label("Apache Maven 3.9 & Git version control"), 1, 6);

        techCard.getChildren().addAll(techTitle, techGrid);

        // ══════════════════════════════════════════════════════════════
        // CARD 3: Database Design (SQLite + JDBC)
        // ══════════════════════════════════════════════════════════════
        VBox dbCard = new VBox(12);
        dbCard.getStyleClass().add("card");

        Label dbTitle = new Label("Database Design — SQLite (JDBC, PreparedStatement, FK)");
        dbTitle.getStyleClass().add("card-title");

        TextArea dbSchema = new TextArea(
            "ER Diagram:\n" +
            "\n" +
            "  users (1)\n" +
            "    |  1:N  (one user owns many tasks)\n" +
            "    ↓\n" +
            "  tasks  ──── N:1 ────> categories\n" +
            "\n" +
            "Schema:\n" +
            "  users      : id (PK), name, email UNIQUE\n" +
            "  categories : id (PK), name UNIQUE\n" +
            "  tasks      : id (PK), user_id (FK→users), category_id (FK→categories),\n" +
            "               title, description, priority, status, created_at, deadline, completed\n" +
            "\n" +
            "PRAGMA foreign_keys = ON;   ← enforces FK constraints in SQLite\n" +
            "\n" +
            "CRUD SQL:\n" +
            "  CREATE → INSERT INTO tasks (user_id, category_id, title, ...) VALUES (?, ?, ?, ...)\n" +
            "  READ   → SELECT t.*, u.name, c.name FROM tasks t\n" +
            "                JOIN users u ON t.user_id = u.id\n" +
            "                LEFT JOIN categories c ON t.category_id = c.id\n" +
            "  UPDATE → UPDATE tasks SET title=?, priority=?, status=?, ... WHERE id=?\n" +
            "  DELETE → DELETE FROM tasks WHERE id=?"
        );
        dbSchema.setEditable(false);
        dbSchema.setPrefRowCount(18);
        dbSchema.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11.5px;");

        dbCard.getChildren().addAll(dbTitle, dbSchema);

        // ══════════════════════════════════════════════════════════════
        // CARD 4: Concurrency Architecture
        // ══════════════════════════════════════════════════════════════
        VBox concCard = new VBox(12);
        concCard.getStyleClass().add("card");

        Label concTitle = new Label("Concurrency Architecture — Thread Pools & Multithreading");
        concTitle.getStyleClass().add("card-title");

        TextArea concText = new TextArea(
            "BackgroundTaskManager  (the single concurrency hub)\n" +
            "  ├── generalThreadPool   : Executors.newFixedThreadPool(4)\n" +
            "  │     └── Tasks: NetworkService HTTP requests, StatisticsWorker calculations\n" +
            "  │     └── Why pool? Avoids creating a new Thread per request (expensive OS calls)\n" +
            "  └── scheduledThreadPool : Executors.newScheduledThreadPool(2)\n" +
            "        └── AutoSaveTask   : every 30 s — saves tasks to disk (off UI thread)\n" +
            "        └── DeadlineMonitor: every 10 s — scans for overdue tasks\n" +
            "\n" +
            "Thread Safety in TaskService:\n" +
            "  ReentrantReadWriteLock  — multiple threads can READ concurrently\n" +
            "                          — only one thread WRITES at a time (exclusive)\n" +
            "  CopyOnWriteArrayList    — change listeners list is thread-safe\n" +
            "  AtomicInteger           — ID counter increment is atomic (no race condition)\n" +
            "\n" +
            "JavaFX Single-Threaded UI Rule:\n" +
            "  ALL JavaFX control updates MUST happen on the JavaFX Application Thread.\n" +
            "  Background callbacks use Platform.runLater(Runnable) to safely dispatch updates.\n" +
            "\n" +
            "Standalone Thread demonstration:\n" +
            "  BackgroundTaskManager.runStandaloneThread() — explicitly creates new Thread(runnable)\n" +
            "  Used for one-shot diagnostics, demonstrating raw Thread vs. pool difference."
        );
        concText.setEditable(false);
        concText.setPrefRowCount(18);
        concText.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11.5px;");

        concCard.getChildren().addAll(concTitle, concText);

        // ══════════════════════════════════════════════════════════════
        // CARD 5: JVM / JRE / JDK Concepts
        // ══════════════════════════════════════════════════════════════
        VBox jvmCard = new VBox(12);
        jvmCard.getStyleClass().add("card");
        Label jvmTitle = new Label("Core Concepts: JVM, JRE, JDK");
        jvmTitle.getStyleClass().add("card-title");

        Label jvmDesc = new Label(
            "JVM (Java Virtual Machine): Abstract execution engine that loads, verifies, and interprets/JIT-compiles bytecode (.class files) into native machine code. Platform-independent because bytecode is universal.\n\n" +
            "JRE (Java Runtime Environment): JVM + Java Class Libraries (java.base, java.desktop). Sufficient to RUN Java programs.\n\n" +
            "JDK (Java Development Kit): JRE + javac (compiler) + javadoc + javap + jdb + jar. Required to DEVELOP Java programs.\n\n" +
            "Compilation:\n" +
            "  C:    gcc hello.c -o hello.exe   → direct machine code (no JVM)\n" +
            "  Java: javac Hello.java → Hello.class (bytecode) → java Hello (JVM executes)"
        );
        jvmDesc.setWrapText(true);
        jvmDesc.setStyle("-fx-text-fill: #334155; -fx-font-size: 12px;");
        jvmCard.getChildren().addAll(jvmTitle, jvmDesc);

        // ══════════════════════════════════════════════════════════════
        // CARD 6: Demonstration Checklist (Viva)
        // ══════════════════════════════════════════════════════════════
        VBox demoCard = new VBox(12);
        demoCard.getStyleClass().add("card");
        Label demoTitle = new Label("Assignment Demonstration Checklist");
        demoTitle.getStyleClass().add("card-title");

        TextArea demoText = new TextArea(
            "DATABASE & CRUD:\n" +
            "  1. Open app → DB auto-initializes (data/taskmanager.db created)\n" +
            "  2. Go to Tasks tab → click '+ Add Task' → fill form → click Save\n" +
            "     → SQL: INSERT INTO tasks (...) VALUES (?, ?, ...) executes in TaskDAO\n" +
            "  3. Open DB Browser / sqlite3 CLI → SELECT * FROM tasks; → show inserted row\n" +
            "  4. Select a task → click 'Edit Task' → change title → Save\n" +
            "     → SQL: UPDATE tasks SET title=? WHERE id=?\n" +
            "  5. Select a task → click 'Delete Task' → confirm\n" +
            "     → SQL: DELETE FROM tasks WHERE id=?\n" +
            "  6. Show TaskDAO.java — highlight PreparedStatement, RETURN_GENERATED_KEYS, JOIN\n" +
            "\n" +
            "CONCURRENCY:\n" +
            "  7. Status bar at bottom: 'Auto-Save: ✓ ...' updates every 30 s (background thread)\n" +
            "  8. Status bar: 'Deadlines: ...' updates every 10 s (scheduled thread)\n" +
            "  9. Go to Statistics tab → click '⚠️ Run Race Condition' → show corrupted count\n" +
            " 10. Click '✅ Run Thread-Safe Test' → count = 1000 (locked safely)\n" +
            " 11. Show BackgroundTaskManager.java — highlight newFixedThreadPool(4)\n" +
            " 12. Show MainView.java → initBackgroundTasks() → scheduleWithFixedDelay()\n" +
            " 13. Explain: why Platform.runLater() — FX thread rule\n" +
            "\n" +
            "NETWORKING:\n" +
            " 14. Go to 'Online Data' tab → click 'Fetch Online Productivity Quote'\n" +
            " 15. Status shows: 'Fetching...' → then '✅ Data loaded successfully'\n" +
            " 16. Show the parsed quote, author, HTTP status, executing thread name\n" +
            " 17. Show raw JSON in the text area\n" +
            " 18. Explain: Worker Thread shown is DB-Network-Worker-N (pool thread, not FX thread)\n" +
            " 19. Show NetworkService.java — highlight HttpClient, HttpRequest, HttpResponse,\n" +
            "     Jackson ObjectMapper.readTree(), Platform.runLater()\n" +
            " 20. Disconnect internet → Fetch → error handled gracefully (no crash)\n" +
            "\n" +
            "GIT:\n" +
            " 21. git log --oneline → show commit history\n" +
            " 22. Show meaningful commit messages covering each feature"
        );
        demoText.setEditable(false);
        demoText.setPrefRowCount(32);
        demoText.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11.5px;");
        demoCard.getChildren().addAll(demoTitle, demoText);

        // ══════════════════════════════════════════════════════════════
        // CARD 7: Likely Viva Questions
        // ══════════════════════════════════════════════════════════════
        VBox vivaCard = new VBox(12);
        vivaCard.getStyleClass().add("card");
        Label vivaTitle = new Label("Likely Viva Questions & Answers");
        vivaTitle.getStyleClass().add("card-title");

        TextArea vivaText = new TextArea(
            "DATABASE:\n" +
            "Q: Why SQLite? A: Lightweight, serverless, file-based — ideal for desktop apps. No installation needed.\n" +
            "Q: What is JDBC? A: Java Database Connectivity — Java API for connecting to and querying databases.\n" +
            "Q: What is a primary key? A: Uniquely identifies each row in a table. Cannot be NULL or duplicate.\n" +
            "Q: What is a foreign key? A: A column in one table referencing the PK of another, enforcing referential integrity.\n" +
            "Q: What is a relationship? A: How tables relate. Here: users 1:N tasks, tasks N:1 categories.\n" +
            "Q: What is a JOIN? A: Combines rows from two tables based on a related column.\n" +
            "Q: Why PreparedStatement? A: Prevents SQL injection, improves performance via precompilation.\n" +
            "Q: Difference between DELETE and DROP? A: DELETE removes rows; DROP removes the entire table structure.\n" +
            "Q: Why enable foreign keys? A: SQLite disables FK checks by default — PRAGMA foreign_keys=ON enforces them.\n" +
            "\n" +
            "CONCURRENCY:\n" +
            "Q: What is multithreading? A: Running multiple threads concurrently within one process.\n" +
            "Q: What is a thread pool? A: A group of pre-created, reusable threads managed by ExecutorService.\n" +
            "Q: Why ExecutorService over new Thread()? A: Reuses threads, limits count, provides lifecycle control.\n" +
            "Q: What is ScheduledExecutorService? A: Runs tasks periodically at fixed intervals.\n" +
            "Q: What is a race condition? A: When multiple threads access shared data concurrently without sync, causing corruption.\n" +
            "Q: What is synchronization? A: Controlling thread access to shared resources using locks.\n" +
            "Q: Why can't a background thread update JavaFX controls? A: JavaFX has a single-threaded UI model — only the Application Thread may modify controls. Platform.runLater() queues the update safely.\n" +
            "\n" +
            "NETWORKING:\n" +
            "Q: What is HTTP? A: HyperText Transfer Protocol — request/response protocol for web communication.\n" +
            "Q: What is an HTTP GET? A: Retrieves data from a server without modifying it.\n" +
            "Q: What is JSON? A: JavaScript Object Notation — lightweight text-based data interchange format.\n" +
            "Q: How is JSON parsed? A: Jackson ObjectMapper.readTree() converts the JSON string to a JsonNode tree, then fields are extracted.\n" +
            "Q: What is HttpClient? A: java.net.http.HttpClient (Java 11+) — modern HTTP client API.\n" +
            "Q: Why must network requests be in a background thread? A: Network I/O blocks; blocking the FX thread freezes the UI.\n" +
            "Q: What if the API is unavailable? A: IOException is caught; error message dispatched to UI via Platform.runLater()."
        );
        vivaText.setEditable(false);
        vivaText.setPrefRowCount(32);
        vivaText.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px;");
        vivaCard.getChildren().addAll(vivaTitle, vivaText);

        root.getChildren().addAll(metaCard, techCard, dbCard, concCard, jvmCard, demoCard, vivaCard);
    }

    private Label bold(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");
        return lbl;
    }
}
