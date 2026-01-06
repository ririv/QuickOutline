import { pageLabelService } from '@/lib/services/PageLabelService';
import { offsetStore } from './offsetStore.svelte';
import { pageLabelStore } from './pageLabelStore.svelte';
import { checkPdf } from '@/lib/pdfjs/pdfChecker';
import { messageStore } from './messageStore.svelte';
import { formatError } from '@/lib/utils/error';
import { loadDocument, closeDocument } from '@/lib/api/rust_pdf';
import { readFile } from '@tauri-apps/plugin-fs';
import type { PDFDocumentProxy } from 'pdfjs-dist';

import { type PageLabel } from '@/lib/types/page-label.ts';

/**
 * Represents the state of a single opened PDF document.
 * This structure makes it easy to transition to multi-tab support later.
 */
export class DocContext {
    path = $state("");
    pageCount = $state(0);
    version = $state(0);
    pdfDoc = $state.raw<PDFDocumentProxy | null>(null);
    originalPageLabels = $state.raw<string[]>([]);
    originalRules = $state.raw<PageLabel[]>([]);

    constructor(path: string, doc: PDFDocumentProxy) {
        this.path = path;
        this.pdfDoc = doc;
        this.pageCount = doc.numPages;
        this.version = Date.now();
    }

    async destroy() {
        if (this.pdfDoc) {
            await this.pdfDoc.destroy();
            this.pdfDoc = null;
        }
        try {
            await closeDocument(this.path);
        } catch (e) {
            console.warn("Failed to close document in Rust backend:", e);
        }
    }
}

class DocStore {
    // Current active document context
    activeDoc = $state<DocContext | null>(null);

    // Derived properties for backward compatibility with existing components
    currentFilePath = $derived(this.activeDoc?.path || null);
    pageCount = $derived(this.activeDoc?.pageCount || 0);
    pdfDoc = $derived(this.activeDoc?.pdfDoc || null);
    originalPageLabels = $derived(this.activeDoc?.originalPageLabels || []);
    originalRules = $derived(this.activeDoc?.originalRules || []);
    version = $derived(this.activeDoc?.version || 0);

    async openFile(path: string) {
        console.time("DocStore:openFile");
        try {
            // 1. Cleanup old document
            console.time("1. Cleanup");
            if (this.activeDoc) {
                console.log("Closing existing document...");
                await this.activeDoc.destroy();
                this.activeDoc = null;
            }
            console.timeEnd("1. Cleanup");

            // 2. Pre-load in Rust backend
            console.time("2. Rust:loadDocument");
            await loadDocument(path, 'MemoryBuffer');
            console.timeEnd("2. Rust:loadDocument");

            // 3. Pre-check and get instance (stateless backend handled via URL)
            console.time("3. CheckPdf");
            const checkResult = await checkPdf(path);
            console.timeEnd("3. CheckPdf");
            
            if (!checkResult.isValid || !checkResult.doc) {
                if (checkResult.isEncrypted) {
                    messageStore.add("The PDF is password protected.", "ERROR");
                } else if (checkResult.isCorrupted) {
                    messageStore.add("The PDF file is corrupted.", "ERROR");
                } else {
                    messageStore.add(`Failed to open PDF: ${checkResult.errorName || 'Unknown error'}`, "ERROR");
                }
                // Cleanup rust session if frontend loading failed
                await closeDocument(path);
                return;
            }

            // 4. Create new context
            const newContext = new DocContext(path, checkResult.doc);
            
            // 5. Load initial metadata
            // Optimization: Use pdf-lib in frontend to parse originalRules instead of slow Rust lopdf
            console.time("5. Metadata:PageLabels");
            const fileBytes = await readFile(path);
            const originalRules = await pageLabelService.getRulesFromData(fileBytes);
            console.log(originalRules);
            const labels = await pageLabelService.simulateLabels(originalRules, newContext.pageCount) || [];
            
            newContext.originalPageLabels = labels;
            newContext.originalRules = originalRules;
            console.timeEnd("5. Metadata:PageLabels");

            // 6. Activate context
            this.activeDoc = newContext;

            // 7. Init side stores
            console.time("7. SideStores");
            offsetStore.autoDetect(labels);
            pageLabelStore.init(labels);
            
            // Set originalRules to store
            pageLabelStore.setRules(originalRules, newContext.pageCount);
            console.timeEnd("7. SideStores");

            console.log(`Document opened: ${path}`);

        } catch (e: unknown) {
            console.error("DocStore: Failed to open file", e);
            messageStore.add("Failed to open file: " + formatError(e), "ERROR");
            this.activeDoc = null;
            // Best effort cleanup
            try { await closeDocument(path); } catch {}
        } finally {
            console.timeEnd("DocStore:openFile");
        }
    }

    setCurrentFile(path: string | null) {
        if (!path) {
            this.reset();
            return;
        }
        // If we are just restoring a path without a doc instance (e.g. from session)
        // we might need a separate 'restore' logic, but for now we keep it simple.
        if (this.activeDoc) this.activeDoc.path = path;
    }

    async reset() {
        if (this.activeDoc) {
            await this.activeDoc.destroy();
            this.activeDoc = null;
        }
        offsetStore.set(0);
    }
}

export const docStore = new DocStore();
