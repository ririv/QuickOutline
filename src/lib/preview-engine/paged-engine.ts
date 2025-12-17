import { Previewer } from 'pagedjs';
import type { PageLayout, HeaderFooterLayout } from '@/lib/types/page';
import { generatePageCss } from './css-generator';
import { generateSectionHtml } from '@/lib/utils/html-generator';

interface PagedPayload {
    html: string;
    styles: string;
    header: any;
    footer: any;
    pageLayout?: PageLayout;
    hfLayout?: HeaderFooterLayout;
}

// Global reference for compatibility with external components (e.g. TOC Generator)
// In a multi-tab environment, this should point to the currently active/focused engine.
let activeEngineInstance: PagedEngine | null = null;

export class PagedEngine {
    private currentPreviewer: Previewer | null = null;
    private isRendering = false;
    private pendingPayload: PagedPayload | null = null;
    private bufferA: HTMLDivElement | null = null;
    private bufferB: HTMLDivElement | null = null;
    private activeBuffer: 'A' | 'B' = 'B'; 
    private generatedStyles: HTMLStyleElement[] = [];

    constructor() {
        // Register self as active when created (simplified logic for now)
        activeEngineInstance = this;
    }

    public setVisible(visible: boolean) {
        this.generatedStyles.forEach(s => s.disabled = !visible);
    }

    public destroy() {
        this.isRendering = false;
        this.pendingPayload = null;
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

        // 1. Queue handling (Simple Debounce/Lock)
        if (this.isRendering) {
            this.pendingPayload = payload;
            return;
        }

        this.isRendering = true;
        const startTime = performance.now();
        
        try {
            await this.renderToBuffer(payload, container, postProcess);
            const endTime = performance.now();
            onRenderComplete?.(endTime - startTime);

            // Process queue
            while (this.pendingPayload) {
                const next = this.pendingPayload;
                this.pendingPayload = null;
                const queueStart = performance.now();
                await this.renderToBuffer(next, container, postProcess);
                const queueEnd = performance.now();
                onRenderComplete?.(queueEnd - queueStart);
            }
        } catch (e) {
            console.error("Paged Engine Render Error:", e);
        } finally {
            this.isRendering = false;
        }
    }

    private async renderToBuffer(
        payload: PagedPayload, 
        container: HTMLElement,
        postProcess?: (buffer: HTMLElement) => Promise<void>
    ) {
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
             // This handles cases where container might have changed (though usually Engine is recreated)
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
        const { html, styles, header, footer, pageLayout, hfLayout } = payload;
        const pageCss = generatePageCss(header, footer, pageLayout, hfLayout);
        
        const pageCssObject = {
            [window.location.href]: pageCss
        };

        const headerHtml = generateSectionHtml(header);
        const footerHtml = generateSectionHtml(footer);

        const contentWithStyle = `
        <style>${styles}</style>
        <div class="print-header">${headerHtml}</div>
        <div class="print-footer">${footerHtml}</div>
        ${html}
        `;

        const previewer = new Previewer({
            settings: {
                maxChars: 1500,
            }
        });
        
        console.log('[PagedEngine] Starting preview...');
        
        // Capture styles before
        const head = document.head;
        const stylesBefore = Array.from(head.querySelectorAll('style'));

        try {
            await previewer.preview(contentWithStyle, [pageCssObject], targetBuffer);
            console.log('[PagedEngine] Preview finished.');
            
            // Execute Post Process (e.g. fix dots) BEFORE swapping buffers
            // This ensures user sees the finalized content without flickering
            if (postProcess) {
                await postProcess(targetBuffer!);
            }

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

        const links: Array<{ tocPageIndex: number, x: number, y: number, width: number, height: number, targetPage: string }> = [];
        const pages = targetEl.querySelectorAll('.pagedjs_page');

        pages.forEach((pageEl, pageIndex) => {
            const pageRect = pageEl.getBoundingClientRect();
            const items = pageEl.querySelectorAll('.toc-item');

            items.forEach(item => {
                const itemRect = item.getBoundingClientRect();
                const x = itemRect.left - pageRect.left;
                const y = itemRect.top - pageRect.top;
                const targetPage = item.getAttribute('data-target-page');

                if (targetPage) {
                    links.push({
                        tocPageIndex: pageIndex,
                        x,
                        y,
                        width: itemRect.width,
                        height: itemRect.height,
                        targetPage
                    });
                }
            });
        });

        return links;
    }
}


export function getRenderedTocData() {
    return activeEngineInstance?.getRenderedTocData() || [];
}

export function getTocLinkData() {
    return activeEngineInstance?.getTocLinkData() || [];
}