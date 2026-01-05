import { getPageLabels as getPageLabelsRust, simulatePageLabels as simulatePageLabelsRust } from '@/lib/api/rust_pdf';
import { type PageLabel } from '@/lib/pdf-processing/page-label';
import { getPageLabelRules, setPageLabelRules } from '@/lib/pdflib/pageLabels';
import { readFile, writeFile } from "@tauri-apps/plugin-fs";
import { addSuffixToPath } from "@/lib/utils/path";

class PageLabelService {
    /**
     * Get page labels for the provided PDF document using Rust backend.
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
     * Extract rules using pdf-lib in the frontend (Fast).
     */
    async getRulesFromData(data: Uint8Array | ArrayBuffer): Promise<PageLabel[]> {
        return getPageLabelRules(data);
    }

    /**
     * Business Logic: Applies the rules to the PDF file and saves as a new file.
     * @param srcPath The original PDF path.
     * @param rules The rules to apply.
     * @returns The path of the newly saved file.
     */
    async saveRulesAsNewFile(srcPath: string, rules: PageLabel[]): Promise<string> {
        const destPath = addSuffixToPath(srcPath);
        
        try {
            // 1. Read original bytes
            const fileBytes = await readFile(srcPath);
            
            // 2. Modify in memory via pdf-lib
            const newBytes = await setPageLabelRules(fileBytes, rules);
            
            // 3. Write new file
            await writeFile(destPath, newBytes);
            
            return destPath;
        } catch (e) {
            console.error("Error in saveRulesAsNewFile:", e);
            throw e; // Re-throw to be caught by the caller's formatError
        }
    }

    /**
     * Simulates labels for preview based on rules using Rust backend logic.
     */
    async simulateLabels(rules: PageLabel[], totalPages: number): Promise<string[]> {
        try {
            // Note: We cast to any for Rust IPC because the backend expects its own PageLabel structure
            return await simulatePageLabelsRust(rules as any, totalPages);
        } catch (e) {
            console.error("Failed to simulate page labels:", e);
            return [];
        }
    }
}

export const pageLabelService = new PageLabelService();
