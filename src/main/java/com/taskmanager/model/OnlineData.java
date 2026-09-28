package com.taskmanager.model;

/**
 * OnlineData Model
 *
 * Demonstrates:
 * - Data Transfer Object (DTO) for HTTP JSON API integration
 * - Encapsulation of remote data parsed from JSON payloads
 * - Capturing thread execution metadata for concurrency demonstration
 */
public class OnlineData {

    private int id;
    private String content;
    private String author;
    private String sourceUrl;
    private int httpStatusCode;
    private String fetchedAt;
    private String executingThreadName;
    private String rawJson;

    public OnlineData() {}

    public OnlineData(int id, String content, String author, String sourceUrl,
                      int httpStatusCode, String fetchedAt, String executingThreadName, String rawJson) {
        this.id = id;
        this.content = content;
        this.author = author;
        this.sourceUrl = sourceUrl;
        this.httpStatusCode = httpStatusCode;
        this.fetchedAt = fetchedAt;
        this.executingThreadName = executingThreadName;
        this.rawJson = rawJson;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(int httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public String getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(String fetchedAt) {
        this.fetchedAt = fetchedAt;
    }

    public String getExecutingThreadName() {
        return executingThreadName;
    }

    public void setExecutingThreadName(String executingThreadName) {
        this.executingThreadName = executingThreadName;
    }

    public String getRawJson() {
        return rawJson;
    }

    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }

    @Override
    public String toString() {
        return "\"" + content + "\" — " + author;
    }
}
