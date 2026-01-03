import { pageLabelService } from '@/lib/services/PageLabelService';
import { offsetStore } from './offsetStore.svelte';
import { pageLabelStore } from './pageLabelStore.svelte';
import { checkPdf } from '@/lib/pdfjs/pdfChecker';
import { messageStore } from './messageStore.svelte';
import { formatError } from '@/lib/utils/error';
import type { PDFDocumentProxy } from 'pdfjs-dist';

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
    version = $derived(this.activeDoc?.version || 0);

    async openFile(path: string) {
        try {
            // 1. Cleanup old document
            if (this.activeDoc) {
                console.log("Closing existing document...");
                await this.activeDoc.destroy();
                this.activeDoc = null;
            }

            // 2. Pre-check and get instance (stateless backend handled via URL)
            const checkResult = await checkPdf(path);
            
            if (!checkResult.isValid || !checkResult.doc) {
                if (checkResult.isEncrypted) {
                    messageStore.add("The PDF is password protected.", "ERROR");
                } else if (checkResult.isCorrupted) {
                    messageStore.add("The PDF file is corrupted.", "ERROR");
                } else {
                    messageStore.add(`Failed to open PDF: ${checkResult.errorName || 'Unknown error'}`, "ERROR");
                }
                return;
            }

            // 3. Create new context
            const newContext = new DocContext(path, checkResult.doc);
            
            // 4. Load initial metadata
            // Optimization: Load rules once, then simulate labels to avoid double loading PDF in Rust
            const rules = await pageLabelService.getRules(path);
            const labels = await pageLabelService.simulateLabels(rules, newContext.pageCount) || [];
            
            newContext.originalPageLabels = labels;

            // 5. Activate context
            this.activeDoc = newContext;

            // 6. Init side stores
            offsetStore.autoDetect(labels);
            pageLabelStore.init(labels);
            
            // Set rules to store
            pageLabelStore.setRules(rules, newContext.pageCount);

            console.log(`Document opened: ${path}`);

        } catch (e: unknown) {
            console.error("DocStore: Failed to open file", e);
            messageStore.add("Failed to open file: " + formatError(e), "ERROR");
            this.activeDoc = null;
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
