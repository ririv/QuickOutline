import { Previewer, Handler, registerHandlers } from 'pagedjs';
import { Numbering } from '@/lib/pdf-processing/numbering';
import { PageLabelNumberingStyle } from '@/lib/types/page-label';
import { workerClient, type PagedPayload } from './paged-worker-client';

/**
 * A Paged.js Handler to manually fix page numbers in the preview.
 * 
 * Problem:
 * In some browsers (specifically Safari/WebKit) and contexts (like dynamic previewing),
 * CSS counters (e.g. `content: counter(page)`) used within "running elements"
 * (elements moved to margin boxes via `position: running()`) fail to update correctly
 * and often display as 0 or the initial value.
 * 
 * Solution:
 * This handler hooks into the `afterPageLayout` event, retrieves the correct page number
 * from the page element's dataset (`data-page-number`), and manually sets the text content
 * of the page number placeholders.
 * 
 * This works in conjunction with CSS selectors like `.page-num:empty::after`, ensuring that
 * if this JS fix works, the CSS counter is hidden/overridden, but if JS fails, CSS acts as a fallback.
 */
class PageNumberHandler extends Handler {
    constructor(chunker: any, polisher: any, caller: any) {
        super(chunker, polisher, caller);
    }

    afterPageLayout(pageElement: HTMLElement, page: any, breakToken: any) {
        const pageNumStr = pageElement.dataset.pageNumber;
        if (!pageNumStr) return;
        
        const pageNum = parseInt(pageNumStr, 10);
        if (isNaN(pageNum)) return;

        const replaceContent = (selector: string, style: PageLabelNumberingStyle) => {
            const elements = pageElement.querySelectorAll(selector);
            elements.forEach(el => {
                // Manually set the content for Safari compatibility where CSS counters in running elements fail
                (el as HTMLElement).innerText = Numbering.formatPageNumber(style, pageNum, null);
            });
        };

        replaceContent('.page-num', PageLabelNumberingStyle.DECIMAL_ARABIC_NUMERALS);
        replaceContent('.page-num-upper-roman', PageLabelNumberingStyle.UPPERCASE_ROMAN_NUMERALS);
        replaceContent('.page-num-lower-roman', PageLabelNumberingStyle.LOWERCASE_ROMAN_NUMERALS);
        replaceContent('.page-num-upper-alpha', PageLabelNumberingStyle.UPPERCASE_LETTERS);
        replaceContent('.page-num-lower-alpha', PageLabelNumberingStyle.LOWERCASE_LETTERS);
    }
}

registerHandlers(PageNumberHandler);

// Global reference for compatibility with external components (e.g. TOC Generator)
// In a multi-tab environment, this should point to the currently active/focused engine.
let activeEngineInstance: PagedEngine | null = null;

export class PagedEngine {
    private currentPreviewer: Previewer | null = null;
    private isRendering = false;
    private bufferA: HTMLDivElement | null = null;
    private bufferB: HTMLDivElement | null = null;
    private activeBuffer: 'A' | 'B' = 'B'; 
    private generatedStyles: HTMLStyleElement[] = [];
    
    // Cancellation Token
    private latestRenderToken = 0;

    constructor() {
        // Register self as active when created (simplified logic for now)
        activeEngineInstance = this;
    }

    public setVisible(visible: boolean) {
        this.generatedStyles.forEach(s => s.disabled = !visible);
    }

    public destroy() {
        this.latestRenderToken++; // Invalidate any running tasks
        this.isRendering = false;
        this.currentPreviewer = null;
        this.generatedStyles.forEach(s => s.remove());
        this.generatedStyles = [];

        if (this.bufferA) {
            this.bufferA.remove();
            this.bufferA = null;
        }
        if (this.bufferB) {
            this.bufferB.remove();
            this.bufferB = null;
        }
        
        if (activeEngineInstance === this) {
            activeEngineInstance = null;
        }
    }

    public async update(
        payload: PagedPayload,
        container: HTMLElement,
        onRenderComplete?: (duration: number) => void,
        postProcess?: (buffer: HTMLElement) => Promise<void>
    ) {
        // Mark as active on update
        activeEngineInstance = this;

        // Generate new token for this request
        const token = ++this.latestRenderToken;
        
        // We don't block/queue anymore. We just start a new race.
        // The "Soft Cancellation" logic inside renderToBuffer will ensure
        // that only the latest winner applies its result.
        
        this.isRendering = true;
        const startTime = performance.now();
        
        try {
            await this.renderToBuffer(payload, container, token, postProcess);
            
            // Only report completion if WE are still the winner
            if (token === this.latestRenderToken) {
                const endTime = performance.now();
                onRenderComplete?.(endTime - startTime);
            }
        } catch (e) {
            if ((e as any)?.message !== 'RenderCancelled') {
                console.error("Paged Engine Render Error:", e);
            }
        } finally {
            if (token === this.latestRenderToken) {
                this.isRendering = false;
            }
        }
    }

    private async renderToBuffer(
        payload: PagedPayload, 
        container: HTMLElement,
        token: number,
        postProcess?: (buffer: HTMLElement) => Promise<void>
    ) {
        // Check Cancellation: Start
        if (token !== this.latestRenderToken) return;

        // Initialize buffers if needed
        if (!this.bufferA || !this.bufferB) {
            this.bufferA = this.createBuffer();
            this.bufferB = this.createBuffer();
            // Initially hide both
            this.bufferA.style.display = 'none';
            this.bufferB.style.display = 'none';
            // Append to container
            container.appendChild(this.bufferA);
            container.appendChild(this.bufferB);
        } else {
             // Ensure buffers are still attached to the current container
             if (!container.contains(this.bufferA)) container.appendChild(this.bufferA);
             if (!container.contains(this.bufferB)) container.appendChild(this.bufferB);
        }

        // Determine target buffer (Render to the HIDDEN one)
        const targetBuffer = this.activeBuffer === 'A' ? this.bufferB : this.bufferA;
        const targetBufferName = this.activeBuffer === 'A' ? 'B' : 'A';
        const oldActive = this.activeBuffer === 'A' ? this.bufferA : this.bufferB;

        // Clear target buffer
        targetBuffer!.innerHTML = '';
        
        targetBuffer!.style.opacity = '0';
        targetBuffer!.style.position = 'absolute';
        targetBuffer!.style.top = '0';
        targetBuffer!.style.left = '0';
        targetBuffer!.style.zIndex = '-1'; 
        targetBuffer!.style.display = 'block'; 

        // Prepare Content
        // --- Offload preparation to Worker ---
        const { pageCss, contentWithStyle } = await workerClient.prepare(payload);
        
        // Check Cancellation: After Worker
        if (token !== this.latestRenderToken) return;
        // -------------------------------------
        
        const pageCssObject = {
            [window.location.href]: pageCss
        };

        const previewer = new Previewer({
            settings: {
                maxChars: 1500,
            }
        });
        
        console.log(`[PagedEngine] Starting preview... (Token: ${token})`);
        
        // Capture styles before
        const head = document.head;
        const stylesBefore = Array.from(head.querySelectorAll('style'));

        try {
            await previewer.preview(contentWithStyle, [pageCssObject], targetBuffer);
            
            // Check Cancellation: After Paged.js (Most expensive part)
            if (token !== this.latestRenderToken) {
                console.log(`[PagedEngine] Render cancelled (Token: ${token} < ${this.latestRenderToken})`);
                return;
            }
            
            console.log(`[PagedEngine] Preview finished. (Token: ${token})`);
            
            // Execute Post Process (e.g. fix dots) BEFORE swapping buffers
            if (postProcess) {
                await postProcess(targetBuffer!);
            }
            
            // Check Cancellation: After PostProcess
            if (token !== this.latestRenderToken) return;

            // Capture styles after
            const stylesAfter = Array.from(head.querySelectorAll('style'));
            const newStyles = stylesAfter.filter(s => !stylesBefore.includes(s));
            
            // Clean up old styles from previous render of THIS engine instance
            this.generatedStyles.forEach(s => s.remove());
            this.generatedStyles = newStyles;
            
            targetBuffer!.style.position = 'static';
            targetBuffer!.style.zIndex = 'auto';
            targetBuffer!.style.opacity = '1';
            
            if (oldActive) {
                oldActive.style.display = 'none';
                oldActive.style.zIndex = ''; 
            }
            
        } catch (err) {
            console.error('[PagedEngine] Preview failed:', err);
            throw err;
        } 

        // SWAP BUFFERS
        console.log('[PagedEngine] Swapping buffers. Showing:', targetBufferName);
        this.activeBuffer = targetBufferName;
        this.currentPreviewer = previewer;
    }

    private createBuffer(): HTMLDivElement {
        const div = document.createElement('div');
        div.className = 'paged-buffer';
        div.style.width = '100%';
        return div;
    }

    public getRenderedTocData() {
        const targetEl = this.activeBuffer === 'A' ? this.bufferA : this.bufferB;
        if (!targetEl) return [];

        const tocEntries: Array<{ title: string; level: number; pageIndex: number; y: number }> = [];
        const pages = targetEl.querySelectorAll('.pagedjs_page');

        pages.forEach((pageEl, pageIndex) => {
            const headings = pageEl.querySelectorAll('h1, h2, h3, h4, h5, h6');
            const pageRect = pageEl.getBoundingClientRect();

            headings.forEach((heading) => {
                const headingRect = heading.getBoundingClientRect();
                const y = headingRect.top - pageRect.top;

                tocEntries.push({
                    title: (heading as HTMLElement).innerText || '',
                    level: parseInt(heading.tagName.substring(1)),
                    pageIndex: pageIndex,
                    y: Math.max(0, Math.round(y))
                });
            });
        });

        return tocEntries;
    }

    public getTocLinkData() {
        const targetEl = this.activeBuffer === 'A' ? this.bufferA : this.bufferB;
        if (!targetEl) return [];

        const links: Array<{ tocPageIndex: number, x: number, y: number, width: number, height: number, targetPageLabel: string }> = [];
        const pages = targetEl.querySelectorAll('.pagedjs_page');

        pages.forEach((pageEl, pageIndex) => {
            const pageRect = pageEl.getBoundingClientRect();
            const items = pageEl.querySelectorAll('.toc-item');

            items.forEach(item => {
                const itemRect = item.getBoundingClientRect();
                const x = itemRect.left - pageRect.left;
                const y = itemRect.top - pageRect.top;
                const targetPageStr = item.getAttribute('data-target-page');

                if (targetPageStr) {
                    links.push({
                        tocPageIndex: pageIndex,
                        x,
                        y,
                        width: itemRect.width,
                        height: itemRect.height,
                        targetPageLabel: targetPageStr
                    });
                }
            });
        });

        return links;
    }

    public getPageCount(): number {
        const targetEl = this.activeBuffer === 'A' ? this.bufferA : this.bufferB;
        if (!targetEl) return 0;
        return targetEl.querySelectorAll('.pagedjs_page').length;
    }
}


export function getRenderedTocData() {
    return activeEngineInstance?.getRenderedTocData() || [];
}

export function getTocLinkData() {
    return activeEngineInstance?.getTocLinkData() || [];
}

export function getPageCount() {
    return activeEngineInstance?.getPageCount() || 0;
}