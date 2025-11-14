package com.ririv.quickoutline.view.utils;

import javafx.application.Platform;
import javafx.concurrent.Task;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A utility class that executes a task after a certain delay of inactivity.
 * This is useful for implementing "debounced" real-time previews.
 *
 * @param <T> The type of the input to the task.
 * @param <R> The type of the result from the task.
 */
public class DebouncedPreviewer<T, R> {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true); // Use daemon threads so they don't prevent application exit
        return t;
    });

    private final long delayMillis;
    private final Function<T, R> taskLogic;
    private final Consumer<R> onSuccess;
    private final Consumer<Throwable> onError;

    private ScheduledFuture<?> scheduledTask;
    private Task<R> runningTask;

    /**
     * Constructs a DebouncedPreviewer.
     *
     * @param delayMillis The delay in milliseconds to wait after the last trigger.
     * @param taskLogic   The long-running function to execute on a background thread. It takes an input of type T and returns a result of type R.
     * @param onSuccess   The callback to execute on the JavaFX Application Thread upon successful completion. It consumes the result of type R.
     * @param onError     The callback to execute on the JavaFX Application Thread if the task fails. It consumes the exception.
     */
    public DebouncedPreviewer(long delayMillis, Function<T, R> taskLogic, Consumer<R> onSuccess, Consumer<Throwable> onError) {
        this.delayMillis = delayMillis;
        this.taskLogic = taskLogic;
        this.onSuccess = onSuccess;
        this.onError = onError;
    }

    /**
     * Triggers the execution of the task after the configured delay.
     * If triggered again before the delay has passed, the previous trigger is cancelled and the delay is reset.
     *
     * @param input The input to be passed to the task logic.
     */
    public void trigger(T input) {
        System.out.println("[Preview DEBUG] trigger() inputHash=" + (input == null ? 0 : input.hashCode()) + " len=" + (input == null ? 0 : input.toString().length()));
        // Cancel any previously scheduled (but not yet started) task
        if (scheduledTask != null && !scheduledTask.isDone()) {
            System.out.println("[Preview DEBUG] cancel previously scheduled task");
            scheduledTask.cancel(false);
        }

        // Schedule the new task to run after the delay
        scheduledTask = scheduler.schedule(() -> {
            System.out.println("[Preview DEBUG] scheduler fired, queuing FX runLater");
            Platform.runLater(() -> {
                // Cancel the previous *running* task, if it's still running
                if (runningTask != null && runningTask.isRunning()) {
                    System.out.println("[Preview DEBUG] cancel runningTask before new one starts");
                    runningTask.cancel();
                }

                // Create and configure the new background task
                runningTask = new Task<>() {
                    @Override
                    protected R call() throws Exception {
                        System.out.println("[Preview DEBUG] background task start");
                        // Execute the long-running logic
                        return taskLogic.apply(input);
                    }
                };

                runningTask.setOnSucceeded(event -> {
                    R result = runningTask.getValue();
                    if (result != null) {
                        System.out.println("[Preview DEBUG] task succeeded, result!=null forwarding to onSuccess");
                        onSuccess.accept(result);
                    }
                });

                runningTask.setOnFailed(event -> {
                    if (onError != null) {
                        System.out.println("[Preview DEBUG] task failed: " + runningTask.getException());
                        onError.accept(runningTask.getException());
                    } else {
                        runningTask.getException().printStackTrace();
                    }
                });

                // Run the task on a new background thread
                Thread t = new Thread(runningTask);
                t.setDaemon(true);
                t.start();
            });
        }, delayMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Shuts down the internal scheduler. Should be called when the view is disposed.
     */
    public void shutdown() {
        scheduler.shutdownNow();
    }
}
