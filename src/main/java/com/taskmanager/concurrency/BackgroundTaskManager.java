package com.taskmanager.concurrency;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * BackgroundTaskManager
 * Central coordinator managing application concurrency, thread pools, and single thread lifecycles.
 *
 * Demonstrates:
 * A. Multithreading vs. Thread Pools (Viva Core Topic)
 * B. ExecutorService fixed thread pool for background database, network, and calculation operations
 * C. ScheduledExecutorService for periodic deadline monitoring
 * D. Standalone Thread creation
 * E. Safe application shutdown and pool termination
 *
 * WHY THREAD POOLS ARE PREFERABLE TO REPEATEDLY CREATING NEW THREADS:
 * 1. Thread Creation Cost: Creating a Java Thread involves allocating ~1 MB of stack memory and
 *    invoking operating system kernel calls (context switching and OS thread scheduling).
 * 2. Resource Exhaustion / DoS Prevention: If 1,000 tasks spawn 1,000 independent threads, system
 *    memory is exhausted, causing OutOfMemoryError. A thread pool caps concurrent workers.
 * 3. Thread Reuse: Worker threads remain alive, picking up queued tasks continuously.
 * 4. Centralized Lifecycle: Enables graceful shutdown (awaitTermination) without orphaned daemon threads.
 */
public class BackgroundTaskManager {

    private static final Logger LOGGER = Logger.getLogger(BackgroundTaskManager.class.getName());

    // THREAD POOL: Used for background database/network operations and asynchronous statistics
    private final ExecutorService generalThreadPool;

    // SCHEDULED THREAD POOL: Periodically checks task deadlines and triggers auto-save
    private final ScheduledExecutorService scheduledThreadPool;

    public BackgroundTaskManager() {
        // THREAD POOL: Initialize fixed pool of 4 worker threads with custom named factory
        this.generalThreadPool = Executors.newFixedThreadPool(4, new ThreadFactory() {
            private final AtomicInteger count = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("DB-Network-Worker-" + count.getAndIncrement());
                return t;
            }
        });

        // SCHEDULED THREAD POOL: Initialize pool of 2 threads for periodic scheduling
        this.scheduledThreadPool = Executors.newScheduledThreadPool(2, new ThreadFactory() {
            private final AtomicInteger count = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("Scheduled-Monitor-" + count.getAndIncrement());
                return t;
            }
        });

        LOGGER.info("BackgroundTaskManager initialized with general worker pool (4 threads) and scheduled pool (2 threads).");
    }

    /**
     * Submits a background task to the general thread pool.
     */
    public Future<?> submit(Runnable task) {
        return generalThreadPool.submit(task);
    }

    /**
     * Submits a Callable task to the general thread pool.
     */
    public <T> Future<T> submit(Callable<T> task) {
        return generalThreadPool.submit(task);
    }

    /**
     * Schedules a periodic task on the scheduled thread pool.
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return scheduledThreadPool.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    /**
     * SINGLE THREAD: Demonstrates standalone Thread creation (manual thread creation).
     * Used for isolated one-off demonstrations or diagnostics.
     */
    public Thread runStandaloneThread(Runnable task, String threadName) {
        // SINGLE THREAD: Explicit instantiation of java.lang.Thread
        Thread standaloneThread = new Thread(task);
        standaloneThread.setName(threadName != null ? threadName : "Standalone-Worker");
        standaloneThread.setDaemon(true);
        standaloneThread.start();
        LOGGER.info("Started standalone single Thread: " + standaloneThread.getName());
        return standaloneThread;
    }

    public ExecutorService getGeneralThreadPool() {
        return generalThreadPool;
    }

    public ScheduledExecutorService getScheduledThreadPool() {
        return scheduledThreadPool;
    }

    /**
     * Shuts down all thread pools gracefully.
     */
    public void shutdown() {
        LOGGER.info("Initiating shutdown of BackgroundTaskManager thread pools...");
        generalThreadPool.shutdown();
        scheduledThreadPool.shutdown();
        try {
            if (!generalThreadPool.awaitTermination(2, TimeUnit.SECONDS)) {
                generalThreadPool.shutdownNow();
            }
            if (!scheduledThreadPool.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduledThreadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            generalThreadPool.shutdownNow();
            scheduledThreadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOGGER.info("All background thread pools terminated.");
    }
}
