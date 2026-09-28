package com.taskmanager.ui;

import com.taskmanager.concurrency.BackgroundTaskManager;
import com.taskmanager.model.OnlineData;
import com.taskmanager.service.NetworkService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * OnlineDataView
 * Provides the "Online Data" tab for HTTP networking and JSON parsing demonstration.
 *
 * Demonstrates:
 * 1. java.net.http.HttpClient — creates and sends an HTTP GET request
 * 2. HttpRequest — builds the request URI, headers, timeout
 * 3. HttpResponse — receives the JSON string body + HTTP status code
 * 4. Jackson JSON parsing — deserializes JSON to OnlineData Java object
 * 5. Background thread pool — network call runs on ExecutorService, NEVER the FX thread
 * 6. Platform.runLater() — safely updates JavaFX UI from the background thread callback
 * 7. Error handling — no internet, HTTP error, JSON parse error, timeout
 *
 * Architecture Flow:
 *   [Fetch Button] → [ExecutorService Thread Pool] → [HttpClient.send()]
 *       → [JSON Response] → [Jackson parse] → [OnlineData object]
 *       → [Platform.runLater()] → [JavaFX UI Labels updated]
 */
public class OnlineDataView extends ScrollPane {

    private final NetworkService networkService;
    private final BackgroundTaskManager backgroundTaskManager;

    // ── Status & Result Labels ──
    private final Label statusLabel        = new Label("Status: Ready — click Fetch Online Data to begin.");
    private final Label quoteLabel         = new Label("(No data fetched yet)");
    private final Label authorLabel        = new Label("");
    private final Label httpStatusLabel    = new Label("");
    private final Label fetchedAtLabel     = new Label("");
    private final Label threadNameLabel    = new Label("");
    private final Label apiUrlLabel        = new Label("");

    // ── Raw JSON display ──
    private final TextArea rawJsonArea     = new TextArea();

    // ── Fetch history log ──
    private final TextArea historyArea     = new TextArea();

    // ── Fetch button ──
    private final Button fetchBtn          = new Button("🌐  Fetch Online Productivity Quote");

    // ── Counter for repeated fetches ──
    private int fetchCount = 0;

    public OnlineDataView(NetworkService networkService, BackgroundTaskManager backgroundTaskManager) {
        this.networkService = networkService;
        this.backgroundTaskManager = backgroundTaskManager;

        setFitToWidth(true);
        setStyle("-fx-background-color: transparent;");

        VBox content = new VBox(20);
        content.getStyleClass().add("view-container");
        buildUI(content);
        setContent(content);
    }

    private void buildUI(VBox root) {

        // ══════════════════════════════════════════════════════════════
        // CARD 1: How It Works — Architecture Explanation
        // ══════════════════════════════════════════════════════════════
        VBox archCard = new VBox(10);
        archCard.getStyleClass().add("card");

        Label archTitle = new Label("Networking & JSON Parsing — Architecture");
        archTitle.getStyleClass().add("card-title");

        Label archDesc = new Label(
            "This tab demonstrates a complete HTTP networking pipeline:\n\n" +
            "  1. User clicks [Fetch] → button is on the JavaFX Application Thread\n" +
            "  2. The request is submitted to an ExecutorService thread pool (NOT the FX thread)\n" +
            "  3. A java.net.http.HttpClient sends an HTTP GET to: https://dummyjson.com/quotes/random\n" +
            "  4. The JSON response is received as an HttpResponse<String>\n" +
            "  5. Jackson ObjectMapper parses the JSON string into an OnlineData Java object\n" +
            "  6. Platform.runLater() safely dispatches the parsed object back to the FX thread\n" +
            "  7. JavaFX labels are updated — the UI shows the parsed data fields\n\n" +
            "Why background thread? — Network I/O can block for seconds. Blocking the JavaFX\n" +
            "Application Thread would freeze the entire GUI. The thread pool handles this safely."
        );
        archDesc.setWrapText(true);
        archDesc.setStyle("-fx-text-fill: #475569; -fx-font-size: 12.5px;");

        archCard.getChildren().addAll(archTitle, archDesc);

        // ══════════════════════════════════════════════════════════════
        // CARD 2: Fetch Control Panel
        // ══════════════════════════════════════════════════════════════
        VBox fetchCard = new VBox(14);
        fetchCard.getStyleClass().add("card");

        Label fetchTitle = new Label("Live HTTP Request — Online Productivity Quote");
        fetchTitle.getStyleClass().add("card-title");

        Label fetchDesc = new Label(
            "Fetches a productivity-related quote from the public DummyJSON API.\n" +
            "API Endpoint: https://dummyjson.com/quotes/random  (returns JSON)"
        );
        fetchDesc.setWrapText(true);
        fetchDesc.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");

        fetchBtn.getStyleClass().add("btn-success");
        fetchBtn.setStyle(fetchBtn.getStyle() + "-fx-font-size: 13px; -fx-padding: 10 20 10 20;");
        fetchBtn.setOnAction(e -> handleFetch());

        Button clearBtn = new Button("🗑  Clear");
        clearBtn.getStyleClass().add("btn-secondary");
        clearBtn.setOnAction(e -> handleClear());

        HBox btnBox = new HBox(12, fetchBtn, clearBtn);
        btnBox.setAlignment(Pos.CENTER_LEFT);

        // Status label
        statusLabel.setStyle("-fx-font-size: 12.5px; -fx-font-weight: bold; -fx-text-fill: #64748b;");
        statusLabel.setWrapText(true);

        fetchCard.getChildren().addAll(fetchTitle, fetchDesc, btnBox, statusLabel);

        // ══════════════════════════════════════════════════════════════
        // CARD 3: Parsed JSON Result — Java Object Fields
        // ══════════════════════════════════════════════════════════════
        VBox resultCard = new VBox(12);
        resultCard.getStyleClass().add("card");

        Label resultTitle = new Label("Parsed JSON → Java Object  (OnlineData fields)");
        resultTitle.getStyleClass().add("card-title");

        quoteLabel.setWrapText(true);
        quoteLabel.setStyle("-fx-font-size: 16px; -fx-font-style: italic; " +
                "-fx-text-fill: #1e3c72; -fx-font-weight: bold;");

        authorLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569; -fx-font-weight: bold;");

        Separator sep = new Separator();
        sep.setPadding(new Insets(4, 0, 4, 0));

        GridPane detailGrid = new GridPane();
        detailGrid.setHgap(20);
        detailGrid.setVgap(8);
        detailGrid.setPadding(new Insets(4, 0, 0, 0));

        httpStatusLabel.setStyle("-fx-font-weight: bold;");
        fetchedAtLabel.setStyle("-fx-text-fill: #475569;");
        threadNameLabel.setStyle("-fx-text-fill: #7c3aed; -fx-font-weight: bold;"); // purple = thread info
        apiUrlLabel.setStyle("-fx-text-fill: #0284c7;");

        detailGrid.add(makeFieldLabel("HTTP Status:"),    0, 0); detailGrid.add(httpStatusLabel, 1, 0);
        detailGrid.add(makeFieldLabel("Fetched At:"),     0, 1); detailGrid.add(fetchedAtLabel,  1, 1);
        detailGrid.add(makeFieldLabel("Worker Thread:"),  0, 2); detailGrid.add(threadNameLabel, 1, 2);
        detailGrid.add(makeFieldLabel("API Source:"),     0, 3); detailGrid.add(apiUrlLabel,     1, 3);

        resultCard.getChildren().addAll(resultTitle, quoteLabel, authorLabel, sep, detailGrid);

        // ══════════════════════════════════════════════════════════════
        // CARD 4: Raw JSON Response
        // ══════════════════════════════════════════════════════════════
        VBox jsonCard = new VBox(10);
        jsonCard.getStyleClass().add("card");

        Label jsonTitle = new Label("Raw JSON Response (from HttpResponse<String>)");
        jsonTitle.getStyleClass().add("card-title");

        rawJsonArea.setEditable(false);
        rawJsonArea.setPrefRowCount(5);
        rawJsonArea.setWrapText(true);
        rawJsonArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; " +
                "-fx-font-size: 12px; -fx-text-fill: #0f4c81;");
        rawJsonArea.setPromptText("Raw JSON will appear here after fetching...");

        Label jsonNote = new Label(
            "The JSON string above is parsed by Jackson ObjectMapper into a Java OnlineData object.\n" +
            "Fields extracted: id (int), quote (String), author (String)"
        );
        jsonNote.setWrapText(true);
        jsonNote.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

        jsonCard.getChildren().addAll(jsonTitle, rawJsonArea, jsonNote);

        // ══════════════════════════════════════════════════════════════
        // CARD 5: Request History Log
        // ══════════════════════════════════════════════════════════════
        VBox historyCard = new VBox(10);
        historyCard.getStyleClass().add("card");

        Label histTitle = new Label("Fetch History Log (Concurrency Execution Trace)");
        histTitle.getStyleClass().add("card-title");

        historyArea.setEditable(false);
        historyArea.setPrefRowCount(6);
        historyArea.setWrapText(true);
        historyArea.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px;");
        historyArea.setPromptText("Each fetch will log the executing thread name and result here...");

        Label histNote = new Label(
            "Notice that 'Worker Thread' is always a pool thread (e.g. DB-Network-Worker-1),\n" +
            "NEVER the JavaFX Application Thread. This proves the network request runs off the UI thread."
        );
        histNote.setWrapText(true);
        histNote.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

        historyCard.getChildren().addAll(histTitle, historyArea, histNote);

        // ══════════════════════════════════════════════════════════════
        // CARD 6: Viva Q&A Reference
        // ══════════════════════════════════════════════════════════════
        VBox vivaCard = new VBox(10);
        vivaCard.getStyleClass().add("card");

        Label vivaTitle = new Label("Viva Reference — Networking Concepts");
        vivaTitle.getStyleClass().add("card-title");

        Label vivaContent = new Label(
            "Q: What is HTTP?\n" +
            "A: HyperText Transfer Protocol — the application-layer protocol for data exchange over the web.\n\n" +
            "Q: What is an HTTP GET request?\n" +
            "A: A request to retrieve data from a server without modifying it.\n\n" +
            "Q: What is JSON?\n" +
            "A: JavaScript Object Notation — a lightweight text format for structured data interchange.\n\n" +
            "Q: What is HttpClient in Java?\n" +
            "A: java.net.http.HttpClient (Java 11+) — a modern, fluent API for sending HTTP requests.\n\n" +
            "Q: What is Jackson ObjectMapper?\n" +
            "A: A Java library that converts JSON strings to Java objects (and vice versa).\n\n" +
            "Q: Why must network requests run in a background thread?\n" +
            "A: Network I/O is blocking — it could take seconds. Blocking the JavaFX Application Thread\n" +
            "   would freeze the UI. The ExecutorService thread pool handles this without affecting the GUI.\n\n" +
            "Q: What happens if the API is unavailable?\n" +
            "A: The catch block handles IOException (network error), InterruptedException, and general\n" +
            "   exceptions. The error message is dispatched via Platform.runLater() to the status label."
        );
        vivaContent.setWrapText(true);
        vivaContent.setStyle("-fx-text-fill: #334155; -fx-font-size: 12px;");

        vivaCard.getChildren().addAll(vivaTitle, vivaContent);

        root.getChildren().addAll(archCard, fetchCard, resultCard, jsonCard, historyCard, vivaCard);
    }

    // ── Event Handlers ──

    private void handleFetch() {
        fetchBtn.setDisable(true);
        fetchCount++;
        int currentFetch = fetchCount;

        setStatus("⏳ Fetching... (request #" + currentFetch + " submitted to thread pool)", "#7c3aed");

        // Delegate to NetworkService — runs HTTP request on BackgroundTaskManager thread pool
        networkService.fetchOnlineDataAsync(
                backgroundTaskManager,
                this::onSuccess,    // called via Platform.runLater() on success
                this::onError       // called via Platform.runLater() on error
        );
    }

    /** Called on JavaFX Application Thread via Platform.runLater() from NetworkService */
    private void onSuccess(OnlineData data) {
        fetchBtn.setDisable(false);
        setStatus("✅ Data loaded successfully (HTTP " + data.getHttpStatusCode() + ")", "#059669");

        quoteLabel.setText("\"" + data.getContent() + "\"");
        authorLabel.setText("— " + data.getAuthor());
        httpStatusLabel.setText(data.getHttpStatusCode() + " OK");
        fetchedAtLabel.setText(data.getFetchedAt());
        threadNameLabel.setText(data.getExecutingThreadName());   // shows pool thread, not FX thread
        apiUrlLabel.setText(data.getSourceUrl());

        // Pretty-format raw JSON
        String raw = data.getRawJson();
        if (raw != null && !raw.isBlank()) {
            rawJsonArea.setText(prettyFormatJson(raw));
        }

        // Append to history log
        String entry = String.format(
            "[#%d] Thread: %-30s | HTTP: %d | Author: %s%n",
            fetchCount, data.getExecutingThreadName(), data.getHttpStatusCode(), data.getAuthor()
        );
        historyArea.appendText(entry);
    }

    /** Called on JavaFX Application Thread via Platform.runLater() from NetworkService */
    private void onError(String errorMessage) {
        fetchBtn.setDisable(false);
        setStatus("❌ Error: " + errorMessage, "#dc2626");
        quoteLabel.setText("(Network error — see status bar)");
        authorLabel.setText("");
        rawJsonArea.setText("Error: " + errorMessage);
        historyArea.appendText("[#" + fetchCount + "] ERROR: " + errorMessage + "\n");
    }

    private void handleClear() {
        quoteLabel.setText("(No data fetched yet)");
        authorLabel.setText("");
        httpStatusLabel.setText("");
        fetchedAtLabel.setText("");
        threadNameLabel.setText("");
        apiUrlLabel.setText("");
        rawJsonArea.clear();
        historyArea.clear();
        fetchCount = 0;
        setStatus("Status: Cleared. Click Fetch to load fresh data.", "#64748b");
    }

    private void setStatus(String text, String colorHex) {
        statusLabel.setText(text);
        statusLabel.setStyle("-fx-font-size: 12.5px; -fx-font-weight: bold; -fx-text-fill: " + colorHex + ";");
    }

    private Label makeFieldLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #475569; -fx-font-size: 12px;");
        return lbl;
    }

    /**
     * Naive JSON pretty-printer (adds newlines after { } , characters).
     * Jackson pretty-printing requires ObjectMapper which is in NetworkService;
     * this gives a readable display without extra dependencies here.
     */
    private String prettyFormatJson(String json) {
        StringBuilder sb = new StringBuilder();
        int indent = 0;
        boolean inString = false;
        for (char c : json.toCharArray()) {
            if (c == '"') inString = !inString;
            if (!inString) {
                if (c == '{' || c == '[') {
                    sb.append(c).append('\n').append("  ".repeat(++indent));
                    continue;
                } else if (c == '}' || c == ']') {
                    sb.append('\n').append("  ".repeat(--indent)).append(c);
                    continue;
                } else if (c == ',') {
                    sb.append(c).append('\n').append("  ".repeat(indent));
                    continue;
                } else if (c == ':') {
                    sb.append(c).append(' ');
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
