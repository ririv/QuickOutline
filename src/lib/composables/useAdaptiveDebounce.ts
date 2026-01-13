export interface DebounceConfig {
    initialTime?: number;
    minTime?: number;
    maxTime?: number;
    penaltyTime?: number; // Extra time to add based on render duration
}

export function useAdaptiveDebounce(config: DebounceConfig = {}) {
    // Default values
    const initial = config.initialTime ?? 10;
    const min = config.minTime ?? 10;
    const max = config.maxTime ?? 1000;
    const penalty = config.penaltyTime ?? 300;

    let debounceTimer: ReturnType<typeof setTimeout>;
    let currentDebounceTime = initial;

    function handleRenderStats(stats: { duration: number }) {
        if (stats.duration < 100) {
            currentDebounceTime = min;
        } else {
            // Adaptive: If render takes X ms, wait X + penalty ms before next try
            currentDebounceTime = Math.min(max, stats.duration + penalty);
        }
        // Optional: Debug logging
        // console.log(`[Adaptive Debounce] Render: ${Math.round(stats.duration)}ms. Next delay: ${currentDebounceTime}ms`);
    }

    function debouncedTrigger(callback: () => void) {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
            callback();
        }, currentDebounceTime);
    }
    
    function clearDebounce() {
        clearTimeout(debounceTimer);
    }

    return {
        handleRenderStats,
        debouncedTrigger,
        clearDebounce,
        // Expose current time for debugging if needed
        getCurrentDelay: () => currentDebounceTime
    };
}
