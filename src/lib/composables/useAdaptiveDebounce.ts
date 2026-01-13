export interface DebounceConfig {
    initialTime?: number;
    minTime?: number;
    maxTime?: number;
    penaltyTime?: number; // Extra time to add based on render duration
    maxWait?: number; // Maximum time to wait before forcing execution
}

export function useAdaptiveDebounce(config: DebounceConfig = {}) {
    // Default values
    const initial = config.initialTime ?? 10;
    const min = config.minTime ?? 10;
    const max = config.maxTime ?? 1000;
    const penalty = config.penaltyTime ?? 300;
    const maxWait = config.maxWait; // Optional

    let debounceTimer: ReturnType<typeof setTimeout>;
    let maxWaitTimer: ReturnType<typeof setTimeout> | null = null;
    let currentDebounceTime = initial;
    let lastInvokeTime = 0;

    function handleRenderStats(stats: { duration: number }) {
        if (stats.duration < 100) {
            currentDebounceTime = min;
        } else {
            // Adaptive: If render takes X ms, wait X + penalty ms before next try
            currentDebounceTime = Math.min(max, stats.duration + penalty);
        }
        // Optional: Debug logging
        console.debug(`[Adaptive Debounce] Render: ${Math.round(stats.duration)}ms. Next delay: ${currentDebounceTime}ms`);

    }

    function invoke(callback: () => void) {
        clearTimeout(debounceTimer);
        if (maxWaitTimer) {
            clearTimeout(maxWaitTimer);
            maxWaitTimer = null;
        }
        lastInvokeTime = Date.now();
        callback();
    }

    function debouncedTrigger(callback: () => void) {
        clearTimeout(debounceTimer);

        // If maxWait is configured and we don't have a maxWait timer running, start one
        if (maxWait && !maxWaitTimer) {
            maxWaitTimer = setTimeout(() => {
                invoke(callback);
            }, maxWait);
        }

        debounceTimer = setTimeout(() => {
            invoke(callback);
        }, currentDebounceTime);
    }
    
    function clearDebounce() {
        clearTimeout(debounceTimer);
        if (maxWaitTimer) {
            clearTimeout(maxWaitTimer);
            maxWaitTimer = null;
        }
    }

    return {
        handleRenderStats,
        debouncedTrigger,
        clearDebounce,
        // Expose current time for debugging if needed
        getCurrentDelay: () => currentDebounceTime
    };
}
