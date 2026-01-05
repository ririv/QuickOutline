import { getPageLabels as getPageLabelsRust, getPageLabelRules as getPageLabelRulesRust, simulatePageLabels as simulatePageLabelsRust } from '@/lib/api/rust_pdf';
import { type PageLabel } from '@/lib/pdf-processing/page-label';
import { getPageLabelRulesFromPdf } from '@/lib/pdflib/pageLabels';

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
     * Deprecated for large files: use getRulesFromBuffer instead.
     */
    async getRules(path: string): Promise<PageLabel[]> {
        if (!path) return [];
        try {
            // Casting because the interfaces are structurally identical but technically different types
            return await getPageLabelRulesRust(path) as unknown as PageLabel[];
        } catch (e) {
            console.error("Failed to get page label rules from Rust:", e);
            return [];
        }
    }

    /**
     * Extract rules using pdf-lib in the frontend (Fast).
     */
    async getRulesFromBuffer(buffer: ArrayBuffer): Promise<PageLabel[]> {
        return getPageLabelRulesFromPdf(buffer);
    }

    /**
     * Simulates labels for preview based on rules using Rust backend logic.
     */
    async simulateLabels(rules: PageLabel[], totalPages: number): Promise<string[]> {
        try {
            return await simulatePageLabelsRust(rules as any, totalPages);
        } catch (e) {
            console.error("Failed to simulate page labels:", e);
            return [];
        }
    }
}

export const pageLabelService = new PageLabelService();
