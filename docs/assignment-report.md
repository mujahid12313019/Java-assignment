# Academic Assignment Report: Multithreaded Desktop Task Management System Using JavaFX

**Course**: Advanced Object-Oriented Programming (Java)  
**Project Title**: Multithreaded Desktop Task Management System Using JavaFX  
**Date**: September 2026  
**Author / Student**: [Student Name Placeholder]  
**Student ID**: [Student ID Placeholder]  
**Department**: Department of Computer Science & Engineering  

---

## 1. Introduction

Desktop software engineering requires a synthesis of reliable domain modeling, responsive Graphical User Interfaces (GUI), safe multi-threaded concurrency, and dependable persistent storage. In high-performance software systems, operations such as disk I/O, heavy mathematical calculations, and continuous temporal monitoring must never monopolize the primary user-interface thread. 

This project delivers a complete, production-grade **Multithreaded Desktop Task Management System** developed in Java 17 and JavaFX. The application demonstrates core computer science competencies including Object-Oriented Analysis and Design (OOAD), Java thread concurrency with `ReentrantReadWriteLock` and `ScheduledExecutorService`, JavaFX reactive UI data binding, low-level compilation dynamics (comparing GCC for C versus `javac` for Java), and distributed version control using Git.

---

## 2. Objectives

The primary pedagogical and technical objectives of this project are:
1. **Object-Oriented Design**: Implement clean encapsulation, defensive copying, and immutable DTOs using modern Java 17 features.
2. **Responsive GUI Engineering**: Build a rich, non-blocking JavaFX desktop client that provides instantaneous feedback, sorting, searching, and filtering.
3. **Multithreaded Systems**: Deploy dedicated background threads (`AutoSaveTask`, `DeadlineMonitor`, `StatisticsWorker`) using `ScheduledExecutorService` and `ExecutorService`.
4. **Thread Safety and Concurrency**: Solve race conditions and `ConcurrentModificationException` using `ReentrantReadWriteLock` and thread-safe snapshots.
5. **UI Thread Discipline**: Strictly respect the JavaFX single-threaded rule by funneling UI mutations through `Platform.runLater()`.
6. **Compilation Systems Analysis**: Contrast the static, platform-dependent native compilation model of C with the portable, virtual-machine bytecode model of Java.
7. **Version Control Lifecycle**: Demonstrate clean Git repository management, feature branching, and `.gitignore` hygiene.

---

## 3. Problem Statement

Standard desktop task managers developed as academic prototypes often suffer from critical architectural flaws:
1. **UI Freezing (Hitching)**: Running disk file writes and statistics calculations directly on the JavaFX Application Thread causes noticeable stutter, dropped frames, and unresponsive buttons.
2. **Race Conditions & Inconsistent State**: When multiple background threads access shared data structures without synchronization, lost updates or `ConcurrentModificationException` occur.
3. **Threading Violations**: Naively updating JavaFX controls from background threads causes runtime `IllegalStateException: Not on FX application thread`.
4. **Lack of Theoretical Grounding**: Many developers fail to grasp what occurs beneath the code—specifically the distinctions between native machine compilation (C) and bytecode execution (JVM).

---

## 4. Proposed Solution

This project implements a decoupled, multi-tier architecture to address each problem:
1. **Background Scheduled Services**: A `ScheduledExecutorService` offloads background auto-saving (30-second intervals) and real-time deadline monitoring (10-second intervals) onto background daemon threads.
2. **Read-Write Locking**: `TaskService` uses `java.util.concurrent.locks.ReentrantReadWriteLock` to permit concurrent reader threads while guaranteeing mutual exclusion during mutations.
3. **Defensive Copying**: Background threads receive deep-copied snapshots (`getTasksSnapshot()`), ensuring that disk serialization and metrics aggregation never collide with user mutations.
4. **JavaFX Thread Confinement**: All worker thread outcomes are marshaled back to the JavaFX Application Thread via `Platform.runLater()`.
5. **Interactive Laboratory**: A built-in Concurrency Lab visualizes unsynchronized race conditions versus thread-safe synchronization directly in the user interface.

---

## 5. Technologies Used

- **Programming Language**: Java 17 LTS (Modern syntax, Lambdas, Streams, java.time API)
- **GUI Framework**: JavaFX 17 (`javafx-controls`, `javafx-fxml`)
- **Persistence Framework**: Jackson Databind 2.16.1 & Jackson Datatype JSR-310 (JSON persistence)
- **Concurrency Utilities**: `java.util.concurrent` (`ScheduledExecutorService`, `ExecutorService`, `ReentrantReadWriteLock`, `CountDownLatch`, `AtomicInteger`)
- **Build System**: Apache Maven 3.9+
- **Unit Testing**: JUnit 5 (Jupiter 5.10.2)
- **Version Control**: Git & GitHub

---

## 6. System Features

### A. Task Lifecycle and CRUD Operations
- **Task Creation**: Modal dialog validating title, description, priority (LOW, MEDIUM, HIGH), status (PENDING, IN_PROGRESS, COMPLETED), and deadline date.
- **Task Modification**: In-place editing pre-populated with existing attributes.
- **Task Deletion**: Contextual confirmation alerts to avoid accidental deletions.
- **Completion Toggling**: Immediate status and flag synchronization.

### B. Searching, Filtering, and Sorting
- **Real-Time Keyword Search**: Matches keywords across Title, Description, and unique ID using Java 8 Stream predicates.
- **Multi-Criteria Filtering**: Simultaneous filtering by Status and Priority.
- **Column Sorting**: Interactive sorting by ID, Title, Priority, Status, Deadline, and Creation Date.

### C. Dashboard & Analytics
- **Executive KPI Cards**: Visual metrics for Total, Pending, In Progress, Completed, and Overdue tasks.
- **Urgent Task Table**: Immediate identification of high-priority tasks and overdue items.
- **Statistics & Progress Bars**: Graphical completion, pending, and priority distribution metrics.

### D. Concurrency Features
- **Periodic Auto-Save**: Background persistence every 30 seconds.
- **Real-Time Deadline Monitor**: Polling every 10 seconds with dynamic status bar warnings.
- **Asynchronous Statistics Worker**: Background calculation with millisecond runtime tracking.
- **Live In-App Concurrency Demonstration**: Demonstrates race condition failures and synchronization remedies on demand.

---

## 7. System Architecture

The project follows a clean separation of concerns:

```
+-------------------------------------------------------------------------+
|                              PRESENTATION                               |
|   MainApp (Application Lifecycle) -> MainView (BorderPane & TabPane)    |
|   DashboardView | TaskView | StatisticsView | AboutView | TaskDialog    |
+-------------------------------------------------------------------------+
                                     │
                 (Safe Dispatch: Platform.runLater)
                                     ▼
+-------------------------------------------------------------------------+
|                           CONCURRENCY LAYER                             |
|   AutoSaveTask           DeadlineMonitor          StatisticsWorker      |
|   (Scheduled Thread)     (Scheduled Thread)       (Executor Thread)     |
+-------------------------------------------------------------------------+
                                     │
                     (ReadLock / Defensive Snapshot)
                                     ▼
+-------------------------------------------------------------------------+
|                              SERVICE LAYER                              |
|   TaskService (ReentrantReadWriteLock, CRUD, Streams, Change Listeners) |
|   StatisticsService (Aggregation, Percentages, Metric Calculations)     |
+-------------------------------------------------------------------------+
                                     │
                        (File I/O / JSON Mapping)
                                     ▼
+-------------------------------------------------------------------------+
|                            PERSISTENCE LAYER                            |
|   FileStorageService (Jackson ObjectMapper, Atomic Temp-File Moves)     |
|   data/tasks.json (Persistent Store)                                    |
+-------------------------------------------------------------------------+
```

---

## 8. Java Compilation Process

Java uses a **two-phase hybrid compilation model**:
1. **Compilation Phase (`javac`)**: The Java compiler translates human-readable source code (`.java`) into an intermediate, platform-neutral binary format called **Bytecode** (`.class` files).
2. **Execution Phase (`java` / JVM)**:
   - **ClassLoader Subsystem**: Loads `.class` files into memory, links dependencies, and initializes static structures.
   - **Bytecode Verifier**: Inspects instructions for memory safety, stack underflow/overflow, and illegal type casting.
   - **Execution Engine**:
     - *Interpreter*: Rapidly executes bytecode instructions sequentially.
     - *Just-In-Time (JIT) HotSpot Compiler*: Analyzes running code, detects frequently invoked loops and methods ("hot spots"), and compiles them directly into native machine code for maximum execution speed.

```
[Task.java] ──(javac)──> [Task.class] ──(JVM: ClassLoader + JIT)──> Native Machine Code
```

---

## 9. C Compilation Process

C employs a **static, platform-dependent compilation process** that produces machine code directly targeting the host CPU architecture:

1. **Preprocessing (`cpp`)**: Resolves `#include` headers, macro definitions (`#define`), and conditional preprocessor directives (`#ifdef`). Output: preprocessed text (`.i`).
2. **Compilation Proper (`cc1`)**: Performs lexical analysis, parsing, semantic checking, and optimizations, producing assembly code (`.s`).
3. **Assembly (`as`)**: Converts assembly mnemonics into raw machine code instructions, producing a relocatable object file (`.o`).
4. **Linking (`ld`)**: Combines object files with runtime startup code and C standard libraries (`printf`), producing a platform-specific native executable (`.exe` on Windows, ELF on Linux).

```
[hello.c] ──(cpp)──> [.i] ──(cc1)──> [.s] ──(as)──> [.o] ──(ld)──> [hello.exe]
```

---

## 10. JVM, JRE, and JDK

| Entity | Purpose | Components | Target Audience |
| :--- | :--- | :--- | :--- |
| **JVM** (Java Virtual Machine) | Executes Java bytecode; enforces security and abstracts hardware. | ClassLoader, Heap, Stack, Metaspace, GC, JIT Compiler. | Runtime Engine |
| **JRE** (Java Runtime Environment) | Provides the environment necessary to execute compiled Java programs. | JVM + Core Class Libraries (`java.base`, etc.) + Configs. | End Users |
| **JDK** (Java Development Kit) | Full development environment for creating, debugging, and compiling Java apps. | JRE + Tools (`javac`, `jar`, `javap`, `jdb`, `javadoc`). | Developers |

---

## 11. JavaFX Implementation

### Single-Threaded UI Model
JavaFX enforces thread confinement: all interactions with scene graph nodes (`TableView`, `Label`, `Button`) must occur on the **JavaFX Application Thread**. Background worker threads attempting to modify UI elements trigger:
```
java.lang.IllegalStateException: Not on FX application thread
```

### Solution: `Platform.runLater(Runnable)`
When background services (`AutoSaveTask`, `DeadlineMonitor`, `StatisticsWorker`) produce output, they wrap UI updates in a `Runnable` passed to `Platform.runLater()`. This queues the action onto the JavaFX event loop, ensuring thread-safe rendering without UI delays.

---

## 12. Multithreading Implementation

Three dedicated background components are implemented:

1. **`AutoSaveTask`**:
   - Implements `Runnable`.
   - Scheduled via `ScheduledExecutorService` with a fixed delay of 30 seconds.
   - Takes a thread-safe snapshot of tasks and persists them to `data/tasks.json`.
   - Sends execution feedback to the footer status bar via `Platform.runLater()`.

2. **`DeadlineMonitor`**:
   - Implements `Runnable`.
   - Scheduled every 10 seconds.
   - Iterates through the task snapshot to identify overdue items relative to `LocalDate.now()`.
   - Updates the status bar warning dynamically.

3. **`StatisticsWorker`**:
   - Implements `Runnable` and `Callable<TaskStatistics>`.
   - Offloads calculation of task completion percentages, counts, and priority histograms to an `ExecutorService` thread pool.
   - Prevents UI frame drops during large-scale aggregations.

---

## 13. Concurrency and Thread Safety

### Race Condition Simulation
If 10 threads concurrently insert 100 items into an unsynchronized `ArrayList`, the internal capacity buffer experiences race conditions: array resizing overlaps, indices conflict, and elements are lost or corrupted (`ArrayIndexOutOfBoundsException` / `ConcurrentModificationException`).

### The Solution: `ReentrantReadWriteLock`
`TaskService` manages concurrency with a `ReentrantReadWriteLock`:
- **Read Lock (`readLock.lock()`)**: Acquired during `getAllTasks()`, `filterAndSearch()`, and `getTasksSnapshot()`. Multiple threads can read concurrently without contention.
- **Write Lock (`writeLock.lock()`)**: Acquired exclusively during `addTask()`, `updateTask()`, `deleteTask()`, and `markTaskCompleted()`. Readers and writers wait until the modifying transaction completes.
- **Defensive Copying**: `getTasksSnapshot()` uses the `Task` copy constructor to clone each object, preventing background tasks from reading half-modified state while the user edits a task.

---

## 14. Git and Version Control

### Version Control Strategy
- **`main`**: Production-ready, stable codebase.
- **`feature/task-management`**: Core CRUD operations, domain models, and JSON persistence.
- **`feature/javafx-ui`**: JavaFX views, dialogs, custom table rendering, and CSS styling.
- **`feature/concurrency`**: Multithreading, `ScheduledExecutorService`, `ReentrantReadWriteLock`, and thread safety.

### Clean Hygiene with `.gitignore`
Compiled class files (`*.class`), build output directories (`target/`), IDE settings (`.idea/`, `.vscode/`), and temporary logs are excluded from version control to prevent repository bloat.

---

## 15. Testing

Automated testing was conducted using JUnit 5:
- **CRUD Operations**: Verification of auto-incrementing task IDs, input validation, and property updates.
- **Filtering & Search**: Java 8 Streams keyword queries and status/priority predicates.
- **Persistence Verification**: Saving to disk and deserializing back into memory to confirm data integrity.
- **Concurrency Test**: Spawning 20 worker threads simultaneously adding 50 tasks each (1,000 tasks total) into `TaskService`. Final count confirmed at exactly 1,000, validating `ReentrantReadWriteLock` mutual exclusion.

---

## 16. Limitations

1. **File-Based Storage**: While JSON is lightweight and portable, it requires re-writing the full dataset on save, making it less scalable than a relational database (e.g. SQLite, PostgreSQL) for millions of records.
2. **Single-Node Execution**: Background tasks run within a single JVM process and are not distributed across multiple nodes.

---

## 17. Future Improvements

1. **Database Integration**: Transition persistence to SQLite or H2 using JDBC or Hibernate.
2. **Desktop System Tray Notifications**: Provide OS-level push notifications for upcoming deadlines when the application is minimized.
3. **Task Reminders & Sound Alerts**: Implement audio reminders using JavaFX Media API.

---

## 18. Conclusion

The **Multithreaded Desktop Task Management System** fulfills all requirements of modern desktop application development. It showcases professional Java 17 development practices, elegant JavaFX GUI design, rigorous multithreading and thread synchronization, and deep theoretical understanding of compilation models. The codebase is clean, thoroughly tested, and well-structured for academic presentation and viva examination.
