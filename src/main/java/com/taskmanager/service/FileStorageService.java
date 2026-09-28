package com.taskmanager.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.taskmanager.model.Task;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FileStorageService
 * Handles JSON persistence of tasks to and from disk.
 *
 * Demonstrates:
 * - Robust exception handling (IOException)
 * - Safe file writing via temporary files (atomic-style replace)
 * - Jackson JSON object mapping with Java 8/17 Date/Time support
 */
public class FileStorageService {

    private static final Logger LOGGER = Logger.getLogger(FileStorageService.class.getName());
    private final File storageFile;
    private final ObjectMapper objectMapper;

    public FileStorageService(String filePath) {
        this.storageFile = new File(filePath);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        ensureParentDirectoryExists();
    }

    public FileStorageService() {
        this("data/tasks.json");
    }

    private void ensureParentDirectoryExists() {
        File parent = storageFile.getParentFile();
        if (parent != null && !parent.exists()) {
            boolean created = parent.mkdirs();
            if (created) {
                LOGGER.info("Created storage directory: " + parent.getAbsolutePath());
            }
        }
    }

    /**
     * Loads tasks from the JSON file.
     * If file does not exist, returns an empty list.
     * If file is corrupt, logs warning, creates a backup, and returns empty list.
     */
    public synchronized List<Task> loadTasks() throws IOException {
        if (!storageFile.exists()) {
            LOGGER.info("No existing storage file found at " + storageFile.getAbsolutePath() + ". Starting with empty tasks.");
            return new ArrayList<>();
        }

        if (storageFile.length() == 0) {
            LOGGER.info("Storage file is empty. Returning empty list.");
            return new ArrayList<>();
        }

        try {
            List<Task> tasks = objectMapper.readValue(storageFile, new TypeReference<List<Task>>() {});
            LOGGER.info("Successfully loaded " + (tasks != null ? tasks.size() : 0) + " tasks from disk.");
            return tasks != null ? tasks : new ArrayList<>();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to parse " + storageFile.getAbsolutePath() + " - possible corruption.", e);
            // Create a backup of the corrupted file for safety
            File backup = new File(storageFile.getAbsolutePath() + ".corrupt." + System.currentTimeMillis());
            Files.copy(storageFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            LOGGER.warning("Corrupted file backed up to " + backup.getAbsolutePath());
            throw new IOException("Storage file corrupted and backed up to " + backup.getName(), e);
        }
    }

    /**
     * Saves tasks list to disk safely using a temporary file.
     */
    public synchronized void saveTasks(List<Task> tasks) throws IOException {
        ensureParentDirectoryExists();

        File tempFile = new File(storageFile.getAbsolutePath() + ".tmp");
        try {
            // Write to temporary file first
            objectMapper.writeValue(tempFile, tasks != null ? tasks : new ArrayList<>());

            // Atomic move/replace to ensure no half-written files on crash
            Files.move(tempFile.toPath(), storageFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);

            LOGGER.fine("Tasks successfully saved to disk. Total: " + (tasks != null ? tasks.size() : 0));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error saving tasks to " + storageFile.getAbsolutePath(), e);
            if (tempFile.exists()) {
                tempFile.delete();
            }
            throw e;
        }
    }

    public File getStorageFile() {
        return storageFile;
    }
}
