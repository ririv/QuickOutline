package com.ririv.quickoutline.view.utils;

// NOTE: Removed JavaFX-specific Task/Platform to avoid accessibility issues in non-JavaFX context.

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Trailing throttle strategy: ensures at least one execution after a burst of rapid inputs,
 * and if content changes during execution, schedules another run after the delay.
 *
 * Differences vs DebouncedPreviewer:
 * - Does NOT cancel the scheduled task on every trigger; only one pending task exists.
 * - During continuous typing, the pending task will execute after delay using the latest content.
 * - If user keeps typing while previous task is running, a follow-up run is scheduled.
 */
public class TrailingThrottlePreviewer<T, R> {

    private static final Logger log = LoggerFactory.getLogger(TrailingThrottlePreviewer.class);

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "preview-throttle");
        t.setDaemon(true);
        return t;
    });

    private final long delayMillis;
    private final Function<T, R> taskLogic;
    private final Consumer<R> onSuccess;
    private final Consumer<Throwable> onError;

    private volatile T latestInput;
    private volatile boolean contentChangedDuringRun = false;
    private ScheduledFuture<?> scheduled; // pending execution
    private Thread runningThread;
    private volatile boolean running = false;
    private volatile T lastExecutedSnapshot; // 用于比较执行后是否需要自动尾随调度
    private volatile int pendingChangeCount = 0; // 调度等待阶段累计的变更次数

    public TrailingThrottlePreviewer(long delayMillis, Function<T, R> taskLogic, Consumer<R> onSuccess, Consumer<Throwable> onError) {
        this.delayMillis = delayMillis;
        this.taskLogic = taskLogic;
        this.onSuccess = onSuccess;
        this.onError = onError;
    }

    public void trigger(T input) {
        latestInput = input; // always keep most recent
        if (running) {
            contentChangedDuringRun = true; // mark for follow-up run
        }
        if (scheduled == null || scheduled.isDone()) {
            if (log.isDebugEnabled()) log.debug("schedule initial in {}ms", delayMillis);
            scheduled = scheduler.schedule(this::execute, delayMillis, TimeUnit.MILLISECONDS);
            pendingChangeCount = 1; // 首次触发
        } else {
            // 已经有等待中的执行，仅更新最新内容并累积计数
            pendingChangeCount++;
            if (log.isDebugEnabled()) log.debug("updated latestInput while pending (pendingChangeCount={})", pendingChangeCount);
        }
    }

    private void execute() {
        T inputSnapshot = latestInput;
        contentChangedDuringRun = false; // reset before run
    if (log.isDebugEnabled()) log.debug("execute start (snapshot hash={}) pendingChangeCount={}", (inputSnapshot==null?0:inputSnapshot.hashCode()), pendingChangeCount);
    int localPendingCount = pendingChangeCount; // 捕获本次执行前的累计变更次数
    pendingChangeCount = 0; // 重置
        running = true;
        runningThread = new Thread(() -> {
            try {
                R result = taskLogic.apply(inputSnapshot);
                if (result != null) {
                    // 确保 UI 更新在 FX Application Thread 上执行，避免 "Not on FX application thread" / "no Fx application" 通知
                    Platform.runLater(() -> onSuccess.accept(result));
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
                if (onError != null) onError.accept(ex); else log.error("throttle task failed", ex);
            } finally {
                running = false;
                if (log.isDebugEnabled()) log.debug("execute finished; contentChangedDuringRun={}", contentChangedDuringRun);
                lastExecutedSnapshot = inputSnapshot;
                scheduleFollowUpIfNeeded(localPendingCount);
            }
        }, "preview-throttle-run");
        runningThread.setDaemon(true);
        runningThread.start();
    }

    private void scheduleFollowUpIfNeeded(int localPendingCount) {
        // If content changed during run or latestInput differs from what we used, schedule again.
        if (contentChangedDuringRun) {
            if (log.isDebugEnabled()) log.debug("follow-up scheduled (flag) in {}ms", delayMillis);
            scheduled = scheduler.schedule(this::execute, delayMillis, TimeUnit.MILLISECONDS);
            return;
        }
        // 若执行结束后 latestInput 已与刚才使用的 snapshot 不同，也安排一次尾随运行
        if (lastExecutedSnapshot != latestInput) {
            if (log.isDebugEnabled()) log.debug("follow-up scheduled (diff) in {}ms", delayMillis);
            scheduled = scheduler.schedule(this::execute, delayMillis, TimeUnit.MILLISECONDS);
            return;
        }
        // 如果等待阶段发生了多次变化（说明用户在打字的一个小爆发里输入很快），也安排一次尾随执行
        if (localPendingCount > 1) {
            if (log.isDebugEnabled()) log.debug("follow-up scheduled (burst={}) in {}ms", localPendingCount, delayMillis);
            scheduled = scheduler.schedule(this::execute, delayMillis, TimeUnit.MILLISECONDS);
            return;
        }
        if (log.isDebugEnabled()) log.debug("no follow-up needed (identical / single change)");
        scheduled = null;
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
