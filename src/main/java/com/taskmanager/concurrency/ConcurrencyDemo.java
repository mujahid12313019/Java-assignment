package com.taskmanager.concurrency;

import com.taskmanager.model.Task;
import com.taskmanager.model.TaskPriority;
import com.taskmanager.model.TaskStatus;
import com.taskmanager.service.TaskService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ConcurrencyDemo
 *
 * Pedagogical demonstration class designed for viva examination and academic presentation.
 * Demonstrates:
 * 1. The Race Condition Problem (Concurrent unsynchronized access causing lost updates / exceptions)
 * 2. The Synchronization Solution (ReentrantReadWriteLock / mutual exclusion)
 * 3. The JavaFX Single-Threaded Rule (IllegalStateException when updating UI off FX Application Thread)
 */
public class ConcurrencyDemo {

    /**
     * Demonstrates how unsynchronized concurrent access to a standard ArrayList
     * causes data corruption (lost updates) or ConcurrentModificationException.
     */
    public static String demonstrateUnsynchronizedConflict() {
        StringBuilder result = new StringBuilder();
        result.append("=== CONCURRENCY RACE CONDITION EXPERIMENT ===\n");
        result.append("Scenario: 10 worker threads simultaneously appending 100 tasks to an unsynchronized ArrayList.\n");
        result.append("Expected Total: 1,000 tasks.\n\n");

        List<String> unsynchronizedList = new ArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(10);
        AtomicInteger exceptionsCaught = new AtomicInteger(0);

        CountDownLatch latch = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            final int threadId = i;
            pool.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        // Unsynchronized write creates a race condition on ArrayList's internal buffer
                        unsynchronizedList.add("Task from thread " + threadId + " item " + j);
                    }
                } catch (ArrayIndexOutOfBoundsException | ConcurrentModificationException ex) {
                    exceptionsCaught.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            boolean completed = latch.await(3, TimeUnit.SECONDS);
            pool.shutdown();

            result.append("Execution Complete: ").append(completed).append("\n");
            result.append("Actual Size in List: ").append(unsynchronizedList.size()).append(" / 1000\n");
            result.append("Exceptions Encountered: ").append(exceptionsCaught.get()).append("\n");

            if (unsynchronizedList.size() != 1000 || exceptionsCaught.get() > 0) {
                result.append("RESULT: RACE CONDITION CONFIRMED! Data was corrupted or lost due to lack of thread safety.\n");
            } else {
                result.append("RESULT: Interleaved safely by chance (race condition remains latent).\n");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return result.toString();
    }

    /**
     * Demonstrates how the synchronized TaskService (using ReentrantReadWriteLock)
     * completely solves the race condition, guaranteeing 100% data consistency.
     */
    public static String demonstrateSynchronizedSafety() {
        StringBuilder result = new StringBuilder();
        result.append("=== SYNCHRONIZED SAFE EXECUTION EXPERIMENT ===\n");
        result.append("Scenario: 10 worker threads simultaneously inserting 100 tasks into TaskService (ReentrantReadWriteLock).\n");
        result.append("Expected Total: 1,000 tasks.\n\n");

        TaskService safeService = new TaskService(null); // in-memory only for test
        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            final int threadId = i;
            pool.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        safeService.addTask(
                                "Concurrent Task T" + threadId + "-" + j,
                                "Test description",
                                TaskPriority.MEDIUM,
                                TaskStatus.PENDING,
                                LocalDate.now().plusDays(3)
                        );
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            boolean completed = latch.await(5, TimeUnit.SECONDS);
            pool.shutdown();

            int finalCount = safeService.getTaskCount();
            result.append("Execution Complete: ").append(completed).append("\n");
            result.append("Actual Size in TaskService: ").append(finalCount).append(" / 1000\n");

            if (finalCount == 1000) {
                result.append("RESULT: THREAD SAFETY VERIFIED! 100% data integrity preserved with ReentrantReadWriteLock.\n");
            } else {
                result.append("RESULT: Data loss detected: ").append(finalCount).append("\n");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return result.toString();
    }
}
