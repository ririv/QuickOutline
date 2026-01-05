/**
 * Safely extracts a human-readable message from any caught exception.
 * Prevents potential crashes from objects with throwing toString() methods.
 */
export function formatError(e: unknown): string {
    // 1. Standard Error object
    if (e instanceof Error) {
        return e.message || e.name || "Unknown error";
    }

    // 2. Plain string
    if (typeof e === 'string') {
        return e || "Empty error message";
    }

    // 3. Fallback with safety try-catch
    if (e === undefined) return "Undefined error";
    if (e === null) return "Null error";

    try {
        return String(e);
    } catch {
        return "An unknown error occurred (unserializable)";
    }
}
