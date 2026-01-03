import { getPageLabels as getPageLabelsRust, getPageLabelRules, simulatePageLabels, type PageLabel } from '@/lib/api/rust_pdf';

class PageLabelService {
    /**
     * Get page labels for the provided PDF document using Rust backend.
     * Returns a flattened array of label strings (e.g. ["i", "ii", "1", "2"]).
     */
    async getPageLabels(path: string): Promise<string[] | null> {
        if (!path) return null;
        try {
            return await getPageLabelsRust(path);
        } catch (e) {
            console.error("Failed to get page labels from Rust:", e);
            return null;
        }
    }

    /**
     * Gets the structured page label rules from the file using Rust backend.
     * This allows the UI to display and edit existing rules.
     */
    async getRules(path: string): Promise<PageLabel[]> {
        if (!path) return [];
        try {
            return await getPageLabelRules(path);
        } catch (e) {
            console.error("Failed to get page label rules from Rust:", e);
            return [];
        }
    }

    /**
     * Simulates labels for preview based on rules using Rust backend logic.
     * This ensures the preview matches exactly what will be written to the file.
     */
    async simulateLabels(rules: PageLabel[], totalPages: number): Promise<string[]> {
        try {
            return await simulatePageLabels(rules, totalPages);
        } catch (e) {
            console.error("Failed to simulate page labels:", e);
            return [];
        }
    }
}

export const pageLabelService = new PageLabelService();