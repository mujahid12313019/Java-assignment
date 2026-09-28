package com.taskmanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.concurrency.BackgroundTaskManager;
import com.taskmanager.model.OnlineData;
import javafx.application.Platform;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * NetworkService
 * Handles remote HTTP communications using Java 11+ HttpClient and parses JSON responses.
 *
 * Demonstrates:
 * - java.net.http.HttpClient, HttpRequest, HttpResponse
 * - Asynchronous execution on an ExecutorService thread pool (never blocking JavaFX thread)
 * - JSON Parsing with Jackson into Java domain DTO (OnlineData)
 * - Safe UI dispatch using Platform.runLater()
 * - Error handling for timeouts, offline status, and invalid JSON
 */
public class NetworkService {

    private static final Logger LOGGER = Logger.getLogger(NetworkService.class.getName());
    private static final String API_URL = "https://dummyjson.com/quotes/random";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public NetworkService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Executes the HTTP GET request on a background thread pool and safely delivers
     * results back to the JavaFX Application Thread.
     *
     * @param taskManager The concurrency manager providing the background thread pool.
     * @param onSuccess Callback invoked on the JavaFX Application Thread with parsed OnlineData.
     * @param onError Callback invoked on the JavaFX Application Thread on failure.
     */
    public void fetchOnlineDataAsync(BackgroundTaskManager taskManager,
                                     Consumer<OnlineData> onSuccess,
                                     Consumer<String> onError) {

        // THREAD POOL: Submit network task to general thread pool
        taskManager.submit(() -> {
            String threadName = Thread.currentThread().getName();
            LOGGER.info("[" + threadName + "] Initiating HTTP GET request to: " + API_URL);

            try {
                // 1. Build HTTP Request
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .timeout(Duration.ofSeconds(7))
                        .header("Accept", "application/json")
                        .GET()
                        .build();

                // 2. Send HTTP Request and receive response
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int statusCode = response.statusCode();
                String responseBody = response.body();

                LOGGER.info("[" + threadName + "] HTTP Response received. Status Code: " + statusCode);

                if (statusCode != 200) {
                    throw new IOException("HTTP Error " + statusCode + ": Unable to fetch remote data.");
                }

                // 3. Parse JSON response using Jackson
                JsonNode root = objectMapper.readTree(responseBody);
                int id = root.has("id") ? root.get("id").asInt() : 1;
                String quote = root.has("quote") ? root.get("quote").asText() : "Stay focused on your deliverables.";
                String author = root.has("author") ? root.get("author").asText() : "Productivity Assistant";

                String fetchedTime = LocalDateTime.now().format(TIME_FORMAT);

                // 4. Construct Java Object (DTO)
                OnlineData onlineData = new OnlineData(
                        id,
                        quote,
                        author,
                        API_URL,
                        statusCode,
                        fetchedTime,
                        threadName,
                        responseBody
                );

                // 5. Safely deliver to JavaFX Application Thread
                if (onSuccess != null) {
                    Platform.runLater(() -> onSuccess.accept(onlineData));
                }

            } catch (IOException e) {
                String errorMsg = "Network Error: Could not connect to API (" + e.getMessage() + "). Check internet connection.";
                LOGGER.log(Level.WARNING, "[" + threadName + "] " + errorMsg, e);
                if (onError != null) {
                    Platform.runLater(() -> onError.accept(errorMsg));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                String errorMsg = "Network request was interrupted.";
                if (onError != null) {
                    Platform.runLater(() -> onError.accept(errorMsg));
                }
            } catch (Exception e) {
                String errorMsg = "JSON Parsing or Unexpected Error: " + e.getMessage();
                LOGGER.log(Level.SEVERE, "[" + threadName + "] " + errorMsg, e);
                if (onError != null) {
                    Platform.runLater(() -> onError.accept(errorMsg));
                }
            }
        });
    }
}
