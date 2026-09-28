# Multithreaded Desktop Task Management System — JavaFX

**University Assignment — Advanced Object-Oriented Programming (Java)**

A production-quality JavaFX desktop application demonstrating multithreading, SQLite database integration, full CRUD operations, HTTP networking, and JSON parsing.

---

## Quick Start

```powershell
# Run the application
.\run.ps1

# Build only
.\build.ps1

# Run tests
.\test.ps1
```

Or manually:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;C:\Program Files\apache-maven-3.9.6\bin;$env:PATH"
& "C:\Program Files\apache-maven-3.9.6\bin\mvn.cmd" javafx:run
```

---

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 17 LTS |
| GUI | JavaFX 17 (OpenJFX) |
| Database | SQLite 3 via JDBC (sqlite-jdbc 3.45) |
| JSON | Jackson Databind 2.16 |
| Build | Apache Maven 3.9 |
| Testing | JUnit Jupiter 5.10 |
| VCS | Git |

---

## Project Structure

```
src/main/java/com/taskmanager/
├── Main.java                          ← launcher (non-module-path safe)
├── MainApp.java                       ← JavaFX Application lifecycle
├── model/
│   ├── Task.java                      ← domain entity (copy constructor, isOverdue)
│   ├── TaskPriority.java              ← enum (HIGH, MEDIUM, LOW)
│   ├── TaskStatus.java                ← enum (PENDING, IN_PROGRESS, COMPLETED)
│   ├── TaskStatistics.java            ← statistics DTO
│   ├── OnlineData.java                ← HTTP API response DTO
│   ├── User.java                      ← relational user entity
│   └── Category.java                  ← relational category entity
├── dao/
│   ├── TaskDAO.java                   ← CRUD via JDBC PreparedStatement
│   ├── UserDAO.java                   ← user reads
│   └── CategoryDAO.java               ← category reads
├── database/
│   ├── DatabaseConnection.java        ← JDBC connection + PRAGMA foreign_keys=ON
│   └── DatabaseInitializer.java       ← DDL CREATE TABLE + seed data
├── service/
│   ├── TaskService.java               ← business logic, thread-safe cache, delegates to DAO
│   ├── StatisticsService.java         ← computes TaskStatistics from task list
│   ├── FileStorageService.java        ← JSON file persistence (fallback)
│   └── NetworkService.java            ← HttpClient → JSON → OnlineData
├── concurrency/
│   ├── BackgroundTaskManager.java     ← owns ExecutorService + ScheduledExecutorService
│   ├── AutoSaveTask.java              ← Runnable: periodic save (30 s)
│   ├── DeadlineMonitor.java           ← Runnable: periodic overdue scan (10 s)
│   ├── StatisticsWorker.java          ← Callable<TaskStatistics>: async stats
│   └── ConcurrencyDemo.java           ← live race condition + sync demo
└── ui/
    ├── MainView.java                  ← BorderPane: header + 5 tabs + status bar
    ├── DashboardView.java             ← KPI cards + urgent tasks table
    ├── TaskView.java                  ← full CRUD TableView + search/filter
    ├── StatisticsView.java            ← stats cards + concurrency lab
    ├── OnlineDataView.java            ← HTTP fetch + JSON display
    ├── AboutView.java                 ← viva notes + demo checklist
    ├── TaskDialog.java                ← Add/Edit task form dialog
    └── UITheme.java                   ← theme constants
```

---

## Database Integration

### Why SQLite?

SQLite is the ideal choice for a desktop assignment because:
- **Serverless**: No installation needed; works as a single `.db` file
- **Lightweight**: < 1 MB library, perfect for embedded use
- **Standard SQL**: Uses standard ANSI SQL with full JOIN, FK, constraints
- **Java support**: `sqlite-jdbc` driver makes JDBC integration trivial

### Database File

```
data/taskmanager.db        ← created automatically on first run
```

### ER Diagram

```
users (1)
  |
  |  1:N  (one user owns many tasks)
  ↓
tasks ──── N:1 ────> categories
```

### Schema & Relationships

```sql
-- Primary key: uniquely identifies each user
CREATE TABLE IF NOT EXISTS users (
    id    INTEGER PRIMARY KEY AUTOINCREMENT,
    name  TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL
);

-- Primary key: uniquely identifies each category
CREATE TABLE IF NOT EXISTS categories (
    id   INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE
);

-- tasks.user_id → Foreign key → users.id     (One user : Many tasks)
-- tasks.category_id → Foreign key → categories.id  (Many tasks : One category)
CREATE TABLE IF NOT EXISTS tasks (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER NOT NULL,
    category_id INTEGER,
    title       TEXT NOT NULL,
    description TEXT,
    priority    TEXT NOT NULL,
    status      TEXT NOT NULL,
    created_at  TEXT NOT NULL,
    deadline    TEXT,
    completed   INTEGER DEFAULT 0,
    FOREIGN KEY (user_id)     REFERENCES users(id)      ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
);

-- SQLite requires explicit activation of FK enforcement
PRAGMA foreign_keys = ON;
```

**Key Concepts:**
- **Primary Key**: Uniquely identifies a row. `AUTOINCREMENT` ensures no reuse of deleted IDs.
- **Foreign Key**: `tasks.user_id` references `users.id`, creating a hard link between tables.
- **One-to-Many**: One user can own many tasks. One category can group many tasks.
- **Referential Integrity**: `ON DELETE CASCADE` ensures deleting a user also deletes their tasks.
- **Default User**: User ID=1 ("Default Student") is seeded automatically so the UI doesn't need a login screen, while still demonstrating the FK relationship.

---

## CRUD Operations

All four CRUD operations are performed against SQLite via `TaskDAO` using `PreparedStatement`.

| Operation | Java Method | SQL Statement | Trigger |
|-----------|-------------|--------------|---------|
| **CREATE** | `TaskDAO.createTask()` | `INSERT INTO tasks (...) VALUES (?,?,?,?,?,?,?,?,?)` | Click "+ Add Task" in Tasks tab |
| **READ** | `TaskDAO.getAllTasks()` | `SELECT t.*, u.name, c.name FROM tasks t JOIN users u ON t.user_id=u.id LEFT JOIN categories c ON t.category_id=c.id` | App startup, tab switch, after any change |
| **UPDATE** | `TaskDAO.updateTask()` | `UPDATE tasks SET title=?, description=?, priority=?, status=?, deadline=?, completed=? WHERE id=?` | Click "Edit Task" or "Mark Completed" |
| **DELETE** | `TaskDAO.deleteTask()` | `DELETE FROM tasks WHERE id=?` | Click "Delete Task" with confirmation |

### PreparedStatement (prevents SQL Injection)

```java
// ✅ SAFE — parameterized
String sql = "INSERT INTO tasks (user_id, category_id, title, ...) VALUES (?, ?, ?, ...)";
PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
pstmt.setInt(1, userId);
pstmt.setString(3, task.getTitle());  // user input safely bound
pstmt.executeUpdate();

// ❌ DANGEROUS — never do this (SQL injection risk)
// stmt.execute("INSERT INTO tasks VALUES (" + userInput + ")");
```

---

## Concurrency — Multithreading & Thread Pools

### Architecture

```
BackgroundTaskManager  (single concurrency hub, created in MainApp)
  │
  ├── generalThreadPool  : Executors.newFixedThreadPool(4)
  │     Used for:
  │     ├── NetworkService.fetchOnlineDataAsync()   ← HTTP GET requests
  │     └── StatisticsWorker async calculation
  │
  └── scheduledThreadPool : Executors.newScheduledThreadPool(2)
        Used for:
        ├── AutoSaveTask    ← every 30 s, saves data off the UI thread
        └── DeadlineMonitor ← every 10 s, scans for overdue tasks
```

### Where Thread is Used

```java
// BackgroundTaskManager.runStandaloneThread() — explicit new Thread creation
Thread standaloneThread = new Thread(task);
standaloneThread.setName("Standalone-Worker");
standaloneThread.setDaemon(true);
standaloneThread.start();
```

### Where ExecutorService (Thread Pool) is Used

```java
// THREAD POOL: Fixed pool — reuses 4 threads for background operations
// Avoids OS overhead of creating a new Thread for every task
private final ExecutorService generalThreadPool =
    Executors.newFixedThreadPool(4, namedThreadFactory("DB-Network-Worker"));

// Submit network task to pool
generalThreadPool.submit(() -> {
    // HTTP request runs here — NOT on JavaFX Application Thread
});
```

### Where ScheduledExecutorService is Used

```java
// SCHEDULED THREAD POOL: Periodic tasks
private final ScheduledExecutorService scheduledThreadPool =
    Executors.newScheduledThreadPool(2, namedThreadFactory("Scheduled-Monitor"));

// AutoSave every 30 seconds
scheduledThreadPool.scheduleWithFixedDelay(autoSaveTask, 15, 30, TimeUnit.SECONDS);

// DeadlineMonitor every 10 seconds
scheduledThreadPool.scheduleWithFixedDelay(deadlineMonitor, 2, 10, TimeUnit.SECONDS);
```

### Why Thread Pool > Repeatedly Creating New Threads

| Concern | New Thread each time | Thread Pool |
|---------|---------------------|------------|
| OS cost | High (~1 MB stack allocation per thread) | Low (threads are reused) |
| Scalability | 1000 requests = 1000 threads (OutOfMemoryError) | 1000 requests → 4 threads handle them in queue |
| Lifecycle | Orphaned threads risk on shutdown | Clean `awaitTermination()` shutdown |
| Naming / Monitoring | Hard to identify | Custom thread names via `ThreadFactory` |

### Thread Safety in TaskService

```java
// ReentrantReadWriteLock — multiple threads can READ concurrently
// but only ONE thread can WRITE at a time
private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

readLock.lock();   // ← multiple background threads can hold this simultaneously
try { return new ArrayList<>(taskMap.values()); }
finally { readLock.unlock(); }

writeLock.lock();  // ← exclusive — no other reader or writer can proceed
try { taskMap.put(task.getId(), task); }
finally { writeLock.unlock(); }
```

### JavaFX Thread Safety

```java
// ❌ WRONG — updating a Label from a background thread throws IllegalStateException
label.setText("result");   // called from pool thread

// ✅ CORRECT — dispatch to JavaFX Application Thread via Platform.runLater
Platform.runLater(() -> label.setText("result"));
```

---

## Networking & JSON Parsing

### Architecture Flow

```
User clicks [Fetch Online Data]
          ↓
  JavaFX Application Thread
          ↓
  networkService.fetchOnlineDataAsync(backgroundTaskManager, ...)
          ↓
  backgroundTaskManager.submit(Runnable)
          ↓
  ExecutorService Thread Pool (DB-Network-Worker-N)
          ↓
  HttpClient.newBuilder()
    .connectTimeout(6s)
    .build()
    .send(HttpRequest.GET("https://dummyjson.com/quotes/random"),
          BodyHandlers.ofString())
          ↓
  HttpResponse<String>  (statusCode + body)
          ↓
  Jackson ObjectMapper.readTree(responseBody)  → JsonNode
          ↓
  OnlineData object  { id, content, author, httpStatusCode, fetchedAt, threadName }
          ↓
  Platform.runLater(() -> uiCallback.accept(onlineData))
          ↓
  JavaFX Application Thread updates labels in OnlineDataView
```

### Sample JSON Response

```json
{
  "id": 72,
  "quote": "The secret of getting ahead is getting started.",
  "author": "Mark Twain"
}
```

### Java Implementation (NetworkService.java)

```java
// 1. Build HTTP Request
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://dummyjson.com/quotes/random"))
    .timeout(Duration.ofSeconds(7))
    .header("Accept", "application/json")
    .GET()
    .build();

// 2. Send HTTP Request (blocking — runs on pool thread, not FX thread)
HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
int statusCode = response.statusCode();
String body    = response.body();

// 3. Parse JSON → Java object
JsonNode root  = objectMapper.readTree(body);
String quote   = root.get("quote").asText();
String author  = root.get("author").asText();

// 4. Wrap into Java DTO
OnlineData data = new OnlineData(id, quote, author, url, statusCode, time, threadName, body);

// 5. Dispatch to JavaFX thread safely
Platform.runLater(() -> onSuccess.accept(data));
```

### Error Handling

| Error Scenario | Exception Caught | User Feedback |
|---------------|-----------------|--------------|
| No internet | `IOException` | "Network Error: Could not connect..." |
| HTTP 4xx/5xx | Checked manually | "HTTP Error 404: ..." |
| Timeout (7s) | `HttpTimeoutException` (→ `IOException`) | "Network Error: timeout" |
| Bad JSON | `JsonProcessingException` | "JSON Parsing Error: ..." |
| Thread interrupt | `InterruptedException` | "Network request was interrupted" |

---

## C and Java Compilation

### C Compilation (GCC — Native Machine Code)

```
hello.c  →  [GCC Preprocessor]  →  [GCC Compiler]  →  [Linker]  →  hello.exe
                                                          (native machine code)
```

```bash
gcc hello.c -o hello
./hello        # executes directly on CPU
```

### Java Compilation (JVM — Bytecode)

```
Hello.java  →  [javac compiler]  →  Hello.class (bytecode)  →  [JVM]  →  CPU
                                     (platform-independent)    interprets/JIT
```

```bash
javac Hello.java
java Hello      # JVM executes bytecode
```

**Key Difference**: C compiles to native machine code (platform-specific). Java compiles to bytecode that runs on the JVM (Write Once, Run Anywhere).

---

## Git Version Control

### Commit History

```bash
git log --oneline
```

Key commits:
- `Initial complete commit` — full project scaffold, JavaFX UI, JSON persistence
- `feat: Integrate SQLite CRUD via TaskDAO` — replaced JSON storage with real SQLite DB
- `feat: Add OnlineDataView` — HTTP networking, JSON parsing, thread pool integration
- `feat: Add BackgroundTaskManager` — shared ExecutorService + ScheduledExecutorService
- Future: `fix:`, `docs:`, `test:` commits as needed

---

## Assignment Demonstration Checklist

### Database & CRUD
1. Open application → `data/taskmanager.db` auto-created
2. Tasks tab → **"+ Add Task"** → fill form → Save → `INSERT` executes in `TaskDAO`
3. Open `data/taskmanager.db` in DB Browser → `SELECT * FROM tasks;` → show row
4. Select task → **"Edit Task"** → change title → Save → `UPDATE` in `TaskDAO`
5. Select task → **"Delete Task"** → confirm → `DELETE` in `TaskDAO`
6. Show `TaskDAO.java` → highlight `PreparedStatement`, `RETURN_GENERATED_KEYS`, `JOIN`

### Concurrency
7. Status bar: `Auto-Save: ✓ ...` updates every 30 s (background `ScheduledExecutorService`)
8. Status bar: `Deadlines: ...` updates every 10 s (background scheduled thread)
9. Statistics tab → **"⚠️ Run Race Condition"** → count ≠ 1000 (data corruption)
10. **"✅ Run Thread-Safe Test"** → count = 1000 (ReentrantReadWriteLock works)
11. Show `BackgroundTaskManager.java` → `newFixedThreadPool(4)` + `newScheduledThreadPool(2)`
12. Show `MainView.java → initBackgroundTasks()` → `scheduleWithFixedDelay()`
13. Explain `Platform.runLater()` — JavaFX single-threaded UI rule

### Networking
14. Click **"🌐 Online Data"** tab → click **"Fetch Online Productivity Quote"**
15. Status: "Fetching..." → "✅ Data loaded successfully (HTTP 200)"
16. Show parsed quote, author, HTTP status, **worker thread name** (not FX thread!)
17. Show raw JSON in text area
18. Show `NetworkService.java` → `HttpClient`, `HttpRequest`, `HttpResponse`, `ObjectMapper.readTree()`
19. Disconnect internet → Fetch → error handled gracefully (no crash)

### Git
20. `git log --oneline` → show commit history
21. Meaningful commit messages for each feature added

---

## Viva Q&A Reference

### Database
| Question | Answer |
|----------|--------|
| Why SQLite? | Lightweight, serverless, file-based — no server installation needed for desktop apps |
| What is JDBC? | Java Database Connectivity — standard Java API for SQL database access |
| What is a primary key? | Uniquely identifies each row; cannot be NULL or duplicate |
| What is a foreign key? | References a PK in another table; enforces referential integrity |
| What is a relationship? | How tables are linked. Here: users 1:N tasks, tasks N:1 categories |
| What is a JOIN? | Combines rows from two tables based on a matching column value |
| Why PreparedStatement? | Prevents SQL injection; precompiled for performance |
| DELETE vs DROP? | DELETE removes rows; DROP removes the entire table structure |
| Why PRAGMA foreign_keys? | SQLite disables FK enforcement by default; this PRAGMA activates it |

### Concurrency
| Question | Answer |
|----------|--------|
| What is multithreading? | Running multiple threads concurrently within one JVM process |
| What is a thread pool? | Pre-created reusable threads managed by ExecutorService |
| Why pool over new Thread()? | Reuse avoids ~1 MB OS allocation per thread; limits max concurrent threads |
| What is ScheduledExecutorService? | Runs tasks at fixed intervals without manual `sleep()` loops |
| What is a race condition? | Concurrent unsynchronized writes causing data corruption |
| What is synchronization? | Using locks (ReentrantReadWriteLock) to control shared access |
| Why can't background thread update FX controls? | JavaFX enforces single-threaded UI — use `Platform.runLater()` |

### Networking
| Question | Answer |
|----------|--------|
| What is HTTP? | HyperText Transfer Protocol — request/response protocol for web data |
| What is HTTP GET? | Retrieves data from a server without modifying it |
| What is JSON? | JavaScript Object Notation — lightweight text format for data interchange |
| How is JSON parsed? | `Jackson ObjectMapper.readTree()` converts JSON string to `JsonNode` tree |
| What is HttpClient? | `java.net.http.HttpClient` (Java 11+) — modern HTTP client API |
| Why network in background? | Network I/O blocks; blocking FX thread freezes the UI |
| What if API is unavailable? | `IOException` caught → error dispatched via `Platform.runLater()` to status label |
