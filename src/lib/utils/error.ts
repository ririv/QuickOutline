/**
 * Safely extracts a human-readable message from any caught exception.
 * Prevents potential crashes from objects with throwing toString() methods.
 */
export function formatError(e: unknown): string {
    // 1. Standard Error object
    if (e instanceof Error) {
        return e.message;
    }

    // 2. Plain string
    if (typeof e === 'string') {
        return e;
    }

    // 3. Fallback with safety try-catch
    try {
        return String(e);
    } catch {
        return "An unknown error occurred (unserializable)";
    }
}
