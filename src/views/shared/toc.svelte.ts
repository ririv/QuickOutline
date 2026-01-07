import { generateTocPage, type TocConfig, type TocLinkDto } from '@/lib/api/rust_pdf.ts';
import { outlineService } from '@/lib/services/OutlineService.ts';
import { messageStore } from '@/stores/messageStore.svelte.ts';
import { docStore } from '@/stores/docStore.svelte.ts';
import { tocStore } from '@/stores/tocStore.svelte.js';
import { printStore } from '@/stores/printStore.svelte.js';
import { generateTocHtml, DOT_GAP } from '@/lib/templates/toc/toc-gen/toc-generator.tsx';
import { PageSectionTemplate } from '@/lib/templates/PageSectionTemplate.tsx';
import { generatePageCss } from '@/lib/preview-engine/css-generator.ts';
import { TocPrintTemplate } from '@/lib/templates/toc/TocPrintTemplate.tsx';
import { getTocLinkData, getPageCount } from '@/lib/preview-engine/paged-engine.ts';
import { resolveLinkTarget } from '@/lib/services/PageLinkResolver.ts';
import { invoke } from '@tauri-apps/api/core';

export function useTocActions() {
    let debounceTimer: ReturnType<typeof setTimeout>;

    async function loadOutline() {
        try {
            const path = docStore.currentFilePath;
            if (!path) return;
            
            await outlineService.loadOutline(path);
            triggerPreview();
        } catch (e) {
            console.error("Failed to load outline", e);
        }
    }

    function handleContentChange(val: string) {
        tocStore.updateContent(val);
        triggerPreview();
    }

    async function triggerPreview() {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(async () => {
            if (!tocStore.content) {
                return; 
            }
            
            try {
                let pageOffset = 0;
                let threshold = 1;

                if (tocStore.insertionConfig.autoCorrect) {
                    const tocPageCount = getPageCount();
                    if (tocPageCount > 0) {
                        pageOffset = tocPageCount;
                        threshold = Math.max(1, (parseInt(String(tocStore.insertionConfig.pos), 10) || 0) + 1);
                    }
                }

                // Generate HTML locally instead of calling RPC
                const { html, styles } = generateTocHtml(
                    tocStore.content, 
                    tocStore.title, 
                    tocStore.offset, 
                    tocStore.numberingStyle,
                    undefined, // Use default indentStep
                    tocStore.pageLayout,
                    pageOffset,
                    threshold
                );
                
                // Update payload in store, which is passed to Preview component
                tocStore.previewData = {
                    html,
                    styles,
                    header: tocStore.headerConfig,
                    footer: tocStore.footerConfig,
                    pageLayout: tocStore.pageLayout,
                    hfLayout: tocStore.hfLayout
                };
                
            } catch (e: any) {
                console.error("Preview generation failed", e);
            }
        }, 500);
    }

    async function handleGenerate() {
        if (!tocStore.content) {
            messageStore.add("Please enter TOC content first.", "WARNING");
            return;
        }

        try {
            // 1. Calculate Links and Resolve Targets
            const rawLinks = getTocLinkData();
            const links: TocLinkDto[] = [];
            
            // Get current pageLabels from docStore
            const pageLabels = docStore.originalPageLabels;

            const insertPosVal = parseInt(String(tocStore.insertionConfig.pos), 10) || 0;

            const resolverConfig = {
                pageLabels: pageLabels && pageLabels.length > 0 ? pageLabels : null,
                offset: tocStore.offset,
                insertPos: insertPosVal
            };

            for (const raw of rawLinks) {
                const target = resolveLinkTarget(raw.targetPageLabel, resolverConfig);
                if (target !== null) {
                    links.push({
                        tocPageIndex: raw.tocPageIndex,
                        x: raw.x,
                        y: raw.y,
                        width: raw.width,
                        height: raw.height,
                        targetPageIndex: target.index,
                        targetIsOriginalDoc: target.isOriginalDoc
                    });
                }
            }

            // Prepare offset logic for visual page numbers (if auto-correct enabled)
            let pageOffset = 0;
            let threshold = 1;
            if (tocStore.insertionConfig.autoCorrect) {
                const tocPageCount = getPageCount();
                if (tocPageCount > 0) {
                    pageOffset = tocPageCount;
                    threshold = Math.max(1, insertPosVal + 1);
                }
            }

            // 2. Generate HTML with correction
            const { html, styles } = generateTocHtml(
                tocStore.content,
                tocStore.title,
                tocStore.offset,
                tocStore.numberingStyle,
                undefined, // Use default indentStep
                tocStore.pageLayout,
                pageOffset,
                threshold
            );
            
            const headerHtml = PageSectionTemplate(tocStore.headerConfig);
            const footerHtml = PageSectionTemplate(tocStore.footerConfig);
            const pageCss = generatePageCss(tocStore.headerConfig, tocStore.footerConfig, tocStore.pageLayout, tocStore.hfLayout);

            const fullHtml = TocPrintTemplate({
                styles,
                pageCss,
                headerHtml,
                footerHtml,
                tocHtml: html,
                dotGap: DOT_GAP // Pass the dynamic dotGap
            });

            // 3. Generate PDF via Rust
            messageStore.add("Generating PDF...", "INFO");
            const filename = `toc_${Date.now()}.pdf`;
            
            // Use global print mode
            let modeParam = printStore.mode.toLowerCase();
            if (printStore.mode === 'HeadlessChrome') {
                modeParam = 'headless_chrome';
            }

            const pdfPath = await invoke('print_to_pdf', { 
                html: fullHtml, 
                filename: filename,
                mode: modeParam
            });
            
            console.log("PDF Generated at:", pdfPath);

            // 4. Send to Backend for stitching
            const config: TocConfig = {
                tocContent: tocStore.content,
                tocPdfPath: pdfPath as string, // Path to the generated PDF
                title: tocStore.title,
                insertPos: parseInt(String(tocStore.insertionConfig.pos), 10),
                tocPageLabel: {
                    pageIndex: 1, // Will be overwritten by backend based on insertPos
                    numberingStyle: tocStore.numberingStyle,
                    labelPrefix: null,
                    startValue: 1
                },
                header: tocStore.headerConfig,
                footer: tocStore.footerConfig,
                links: links
            };

            const currentFile = docStore.currentFilePath;
            if (!currentFile) throw new Error("No file opened");
            await generateTocPage(currentFile, config, null);
            console.info("PDF generated");
            messageStore.add("Table of Contents generated successfully!", "SUCCESS");

        } catch (e: any) {
            console.error("Generate failed", e);
            messageStore.add("Failed: " + (e.message || e), "ERROR");
        }
    }

    return {
        loadOutline,
        handleContentChange,
        triggerPreview,
        handleGenerate
    };
}
