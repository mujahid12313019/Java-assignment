# Multithreaded Desktop Task Management System Using JavaFX

An enterprise-grade, concurrent desktop application engineered for task management, demonstrating core concepts of Java Object-Oriented Programming, JavaFX graphical user interfaces, multithreading, thread-safe concurrency, native vs. bytecode compilation pipelines, and Git version control.

---

## Table of Contents
1. [Description](#description)
2. [Features](#features)
3. [Technologies Used](#technologies)
4. [Project Architecture](#project-architecture)
5. [Java Compilation vs. C Compilation](#java-compilation-vs-c-compilation)
6. [JVM vs. JRE vs. JDK](#jvm-vs-jre-vs-jdk)
7. [Multithreading and Concurrency](#multithreading-and-concurrency)
8. [JavaFX GUI Architecture](#javafx-gui-architecture)
9. [Git and Version Control Workflow](#git-and-version-control)
10. [How to Build and Run](#how-to-run)
11. [Screenshots](#screenshots)
12. [Testing Suite](#testing-suite)
13. [Learning Outcomes](#learning-outcomes)
14. [Viva Examination Guide](#viva-examination-guide)

---

## Description

The **Multithreaded Desktop Task Management System** is a standalone desktop application built in Java 17 and JavaFX. It allows individuals and teams to track project deliverables, assign priorities, manage lifecycle states, search and filter through tasks, monitor upcoming deadlines, and inspect real-time productivity statistics.

Unlike rudimentary single-threaded demo apps, this system incorporates realistic background worker threads (using `ScheduledExecutorService` and `ExecutorService`) that run parallel to the JavaFX Application Thread. It addresses and resolves classic concurrency dilemmas such as race conditions, thread starvation, and `ConcurrentModificationException` using atomic references, deep copying, and `ReentrantReadWriteLock`.

---

## Features

### Core Task Management (CRUD)
- **Add Tasks**: Form modal with title validation, detailed description, priority selector, initial status, and calendar deadline.
- **Edit Tasks**: Instant modification of task properties with validation.
- **Delete Tasks**: Safe removal with user confirmation dialogs.
- **Mark Completed**: One-click completion status toggling.
- **Dynamic Search**: Instant full-text search across Title, Description, and ID.
- **Multi-Factor Filtering**: Combine status filters (Pending, In Progress, Completed) and priority filters (Low, Medium, High).
- **Sorting**: Native column sorting across IDs, Deadlines, Priorities, and Dates.

### Advanced Multithreading Capabilities
- **Background Auto-Save**: Automatically serializes in-memory task data to disk every 30 seconds without causing GUI stutter.
- **Deadline Monitoring**: Daemon thread scans deadlines every 10 seconds, alerting users of overdue deliverables.
- **Asynchronous Statistics Worker**: Aggregates metrics and distributions in a worker pool without freezing animations.
- **Live In-App Concurrency Experiment**: Side-by-side execution demonstrating unsynchronized race conditions vs. `ReentrantReadWriteLock` safety.

---

## Technologies

- **Language**: Java 17 LTS
- **GUI Framework**: JavaFX 17 (`openjfx`)
- **Build System**: Apache Maven 3.9+
- **Persistence**: File-based JSON via Jackson Databind 2.16 + JavaTime Module
- **Testing**: JUnit 5 (Jupiter 5.10.2)
- **Version Control**: Git / GitHub

---

## Project Architecture

The application adopts a clean, layered architectural pattern:

```
d:/project/
├── pom.xml                                 # Maven project object model
├── .gitignore                              # Git exclusion rules
├── README.md                               # Project documentation
├── build.bat                               # Windows one-click build script
├── run.bat                                 # Windows one-click launcher
├── test.bat                                # Windows one-click test runner
├── data/
│   └── tasks.json                          # JSON persistence store
├── docs/
│   ├── assignment-report.md                # 18-section academic project report
│   └── compilation/
│       ├── hello.c                         # C native compilation sample
│       ├── Hello.java                      # Java bytecode compilation sample
│       └── compilation.md                  # Comprehensive compilation breakdown
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/taskmanager/
│   │   │       ├── Main.java               # Standard non-modular launcher
│   │   │       ├── MainApp.java            # JavaFX Application lifecycle manager
│   │   │       ├── model/
│   │   │       │   ├── Task.java           # Domain entity with defensive copying
│   │   │       │   ├── TaskPriority.java   # Priority enum (LOW, MEDIUM, HIGH)
│   │   │       │   ├── TaskStatus.java     # Status enum (PENDING, IN_PROGRESS, COMPLETED)
│   │   │       │   └── TaskStatistics.java # Immutable metrics DTO
│   │   │       ├── service/
│   │   │       │   ├── FileStorageService.java  # Atomic JSON persistence
│   │   │       │   ├── TaskService.java         # Thread-safe repository (RWLock)
│   │   │       │   └── StatisticsService.java   # Stream-based metrics calculator
│   │   │       ├── concurrency/
│   │   │       │   ├── AutoSaveTask.java        # Periodic background save
│   │   │       │   ├── DeadlineMonitor.java     # Real-time deadline watcher
│   │   │       │   ├── StatisticsWorker.java    # Offloaded metrics worker
│   │   │       │   └── ConcurrencyDemo.java     # Pedagogical race condition demo
│   │   │       └── ui/
│   │   │           ├── UITheme.java             # Pure Java styling helper
│   │   │           ├── TaskDialog.java          # Modal Add/Edit dialog
│   │   │           ├── DashboardView.java       # KPI cards and urgent table
│   │   │           ├── TaskView.java            # CRUD TableView, search & filters
│   │   │           ├── StatisticsView.java      # Visual analytics and lab runner
│   │   │           ├── AboutView.java           # Academic metadata & viva notes
│   │   │           └── MainView.java            # Master layout & thread manager
│   │   └── resources/
│   │       └── css/
│   │           └── styles.css               # Modern desktop styling
│   └── test/
│       └── java/
│           └── com/taskmanager/
│               ├── TaskServiceTest.java         # Unit & concurrency tests
│               └── StatisticsServiceTest.java   # Metric calculations tests
```

---

## Java Compilation vs. C Compilation

| Dimension | C Compilation Pipeline | Java Compilation Pipeline |
| :--- | :--- | :--- |
| **Model** | Direct static compilation to native binary | Hybrid: Compile to Bytecode, Interpret/JIT at runtime |
| **Compiler** | `gcc` (GNU Compiler Collection) | `javac` (Java Compiler) |
| **Intermediate** | Assembly (`.s`), Relocatable Object (`.o`) | Platform-independent Bytecode (`.class`) |
| **Target Output** | Native binary (`.exe` on Win, ELF on Linux) | Portable bytecode executed by the JVM |
| **Portability** | Source-code portable ("Recompile anywhere") | Binary portable ("Write Once, Run Anywhere - WORA") |
| **Memory** | Manual memory allocation (`malloc`/`free`) | Automatic memory management (JVM Garbage Collection) |

### Terminal Verification
Refer to [docs/compilation/compilation.md](file:///d:/project/docs/compilation/compilation.md) for full commands:
```bash
# C Compilation:
gcc docs/compilation/hello.c -o hello.exe
.\hello.exe

# Java Compilation:
javac docs/compilation/Hello.java
java -cp docs/compilation Hello
```

---

## JVM vs. JRE vs. JDK

- **JVM (Java Virtual Machine)**: The abstract runtime computing engine that loads `.class` bytecode, verifies bytecode integrity via the Bytecode Verifier, and executes instructions using an interpreter and the HotSpot Just-In-Time (JIT) compiler.
- **JRE (Java Runtime Environment)**: A bundle containing the JVM plus the core Java Class Libraries (e.g. `java.base`, `java.desktop`) and configuration files required to *run* compiled Java software.
- **JDK (Java Development Kit)**: The complete software development environment containing the JRE plus command-line tools for development (`javac`, `jar`, `javap`, `jdb`, `javadoc`).

---

## Multithreading and Concurrency

### Why and Where Multiple Threads Are Used:

1. **AutoSaveTask (`ScheduledExecutorService`)**:
   - *Problem*: Disk writing involves file system I/O latencies. If executed on the UI thread, user keystrokes and button clicks will stutter.
   - *Solution*: A scheduled background thread executes every 30 seconds, taking an immutable snapshot of tasks and atomically writing to `data/tasks.json`.

2. **DeadlineMonitor (`ScheduledExecutorService`)**:
   - *Problem*: System time changes continuously. Checking deadlines synchronously inside the UI event loop causes overhead.
   - *Solution*: A daemon thread executes every 10 seconds, identifies overdue tasks, and dispatches UI warnings via `Platform.runLater()`.

3. **StatisticsWorker (`ExecutorService` / `Callable<TaskStatistics>`)**:
   - *Problem*: Iterating over tasks to compute completion percentages, category ratios, and overdue metrics consumes CPU time.
   - *Solution*: Statistics are calculated in worker threads and delivered asynchronously to JavaFX components.

### Solving Concurrency & Race Conditions:
- **`ReentrantReadWriteLock` in `TaskService`**: Allows unlimited concurrent reader threads (`AutoSaveTask`, `DeadlineMonitor`, `StatisticsWorker`) without mutual blocking, while acquiring an exclusive write lock when the user adds, edits, or deletes tasks.
- **Defensive Copying**: `taskService.getTasksSnapshot()` returns independent clones of `Task` objects, eliminating `ConcurrentModificationException`.
- **JavaFX Single-Threaded Rule**: Direct calls to JavaFX components from worker threads violate JavaFX threading constraints. All UI updates are channeled through `Platform.runLater(Runnable)`.

---

## JavaFX GUI Architecture

- **Root Structure**: `BorderPane` with Header Banner at `Top`, `TabPane` at `Center`, and Status Bar at `Bottom`.
- **Dashboard View**: 5 KPI cards displaying Total, Pending, In Progress, Completed, and Overdue tasks, plus an urgent tasks table.
- **Tasks View**: Full CRUD interface featuring real-time search, multi-field filtering (Status + Priority), and a responsive `TableView` with custom badge cell renderers.
- **Statistics View**: Dynamic progress bars, priority histograms, asynchronous computation triggers, and an interactive Concurrency Lab.
- **About View**: Academic project details, student metadata placeholder, and viva revision cards.

---

## Git and Version Control

### Recommended Git Workflow
```bash
# 1. Initialize local repository
git init

# 2. Add files and make initial commit
git add .
git commit -m "feat: Initial commit with Maven configuration and project skeleton"

# 3. Create feature branches
git branch feature/task-model
git checkout feature/task-model
# ... implement domain model ...
git commit -am "feat: Add Task entity, TaskPriority, and TaskStatus enums"

# 4. Implement Concurrency branch
git checkout -b feature/concurrency
# ... implement AutoSaveTask, DeadlineMonitor, and RWLock ...
git commit -am "feat: Implement multithreaded auto-save, deadline watcher, and ReentrantReadWriteLock"

# 5. Merge back to main branch
git checkout main
git merge feature/concurrency
```

### Essential Commands Summary
- `git init`: Initializes a new Git repository.
- `git add <files>`: Stages modified files for commit.
- `git commit -m "<msg>"`: Records staged snapshot to repository history.
- `git branch`: Lists, creates, or deletes branches.
- `git switch <branch>` / `git checkout <branch>`: Swaps active branch.
- `git merge <branch>`: Combines histories of specified branch into current branch.
- `git status`: Displays status of working directory and staging area.
- `git log --oneline --graph`: Shows commit visualization tree.

---

## How to Build and Run

### Prerequisites
- JDK 17 (Eclipse Temurin or OpenJDK 17)
- Apache Maven 3.8+

### Execution Commands

```bash
# 1. Clean and compile the project:
mvn clean compile

# 2. Run automated test suite:
mvn test

# 3. Launch the JavaFX Application:
mvn javafx:run

# 4. Package as an executable JAR:
mvn clean package
java -jar target/taskmanager-desktop-1.0.0.jar
```

*Note: On Windows, you can simply double-click `build.bat`, `test.bat`, or `run.bat`.*

---

## Screenshots

> *Placeholder: Add application screenshots below for submission report.*

1. **Dashboard Overview**:
   `[SCREENSHOT_PLACEHOLDER: dashboard_view.png]`
2. **Tasks TableView with Filtering**:
   `[SCREENSHOT_PLACEHOLDER: tasks_crud_view.png]`
3. **Add/Edit Task Dialog Form**:
   `[SCREENSHOT_PLACEHOLDER: task_dialog_modal.png]`
4. **Statistics & Concurrency Experimentation**:
   `[SCREENSHOT_PLACEHOLDER: statistics_concurrency_lab.png]`

---

## Testing Suite

The project includes thorough JUnit 5 tests in `src/test/java/com/taskmanager/`:
- `TaskServiceTest.java`:
  - `testAddTask()`: Validates auto-incrementing ID generation and assignment.
  - `testAddTaskBlankTitle()`: Tests exception handling on invalid inputs.
  - `testUpdateTask()` & `testDeleteTask()`: Validates mutation and deletion operations.
  - `testSearchAndFilter()`: Tests Java 8 Stream keyword and status/priority queries.
  - `testOverdueDetection()`: Tests date comparison logic.
  - `testSaveAndLoad()`: Tests JSON disk persistence and roundtrip recovery.
  - `testConcurrentAddsAreThreadSafe()`: Spawns 20 threads simultaneously writing tasks to prove absence of race conditions.
- `StatisticsServiceTest.java`:
  - `testEmptyList()`: Validates edge cases for zero tasks.
  - `testStatisticsAggregations()`: Tests percentage calculations and category distributions.

---

## Learning Outcomes

1. **Java Syntax & OOP**: Encapsulation, inheritance, polymorphism, records/enums, functional interfaces, lambda expressions, and Java 8 Streams.
2. **Compilation Differences**: GCC native machine translation vs. `javac` intermediate bytecode compilation.
3. **JVM Architecture**: ClassLoader mechanics, memory areas (Heap, Metaspace, Stack), and JIT compilation.
4. **Desktop GUI Engineering**: JavaFX stage/scene lifecycle, layout containers, TableView bindings, custom cell factories, and modal dialogs.
5. **Multithreading**: `Runnable`, `Callable`, `ExecutorService`, and `ScheduledExecutorService` for responsive desktop applications.
6. **Thread Safety**: Eliminating race conditions using `ReentrantReadWriteLock`, deep copying, and `Platform.runLater()`.
7. **Version Control**: Git staging, committing, branching strategies, and `.gitignore` hygiene.

---

## Viva Examination Guide

Review [docs/assignment-report.md](file:///d:/project/docs/assignment-report.md) for full viva sample questions and detailed answers.
