import { messageStore } from '@/stores/messageStore.svelte.ts';
import { printStore } from '@/stores/printStore.svelte.js';
import { markdownStore } from '@/stores/markdownStore.svelte.js';
import { invoke } from '@tauri-apps/api/core';
import { getEditorPreviewCss } from '@/lib/editor/style-converter.ts';
import markdownPreviewCss from '@/lib/editor/styles/markdown-preview.css?inline';
import { PageSectionTemplate } from '@/lib/templates/PageSectionTemplate.tsx';
import { generatePageCss } from '@/lib/preview-engine/css-generator.ts';
import { MarkdownPrintTemplate } from '@/lib/templates/MarkdownPrintTemplate.tsx';
import type { EditorMode, StylesConfig } from '@/lib/editor';

import { useAdaptiveDebounce } from '@/lib/composables/useAdaptiveDebounce.ts';
import { markdownService } from '@/lib/services/MarkdownService.ts';

// Define an interface for the editor interaction
export interface EditorInterface {
    init(content: string, mode: EditorMode, options?: Partial<StylesConfig>): void;
    getValue(): string;
}

export function useMarkdownActions() {
    const { handleRenderStats, debouncedTrigger, clearDebounce } = useAdaptiveDebounce({
        initialTime: 10,
        minTime: 10,
        maxTime: 1000,
        penaltyTime: 300,
        maxWait: 3000 // Force refresh every 3s during continuous typing
    });

    async function triggerPreview() {
        // Read directly from store (Single Source of Truth)
        const content = markdownStore.content;
        const tableStyle = markdownStore.tableStyle;
        const enableIndentedCodeBlocks = markdownStore.enableIndentedCodeBlocks;

        if (!content) {
            // Handle empty content if necessary, or just render empty
        }

        const htmlContent = markdownService.compileHtml(content, {
            enableIndentedCodeBlocks
        });

        await updatePreview(htmlContent, tableStyle);
    }

    async function updatePreview(htmlContent: string, tableStyle: StylesConfig['tableStyle']) {
        const editorThemeCss = getEditorPreviewCss(tableStyle, ".markdown-body");
        
        const generatedCss = `
            ${markdownPreviewCss}
            ${editorThemeCss}
        `;

        markdownStore.currentPagedContent = {
            html: `<div class="markdown-body">${htmlContent}</div>`,
            styles: generatedCss,
            header: markdownStore.headerConfig,
            footer: markdownStore.footerConfig,
            pageLayout: markdownStore.pageLayout,
            hfLayout: markdownStore.hfLayout
        };
    }

    async function handleGenerate() {
        const pagedContent = markdownStore.currentPagedContent;
        if (!pagedContent || !pagedContent.html) {
            messageStore.add("No content to generate.", "WARNING");
            return;
        }
   
       messageStore.add("Preparing PDF resources...", "INFO");
   
       const baseUrl = '.';
   
       // Generate Header/Footer HTML
       const headerHtml = PageSectionTemplate(pagedContent.header);
       const footerHtml = PageSectionTemplate(pagedContent.footer);
   
       // Generate Page CSS
       const pageCss = generatePageCss(pagedContent.header, pagedContent.footer, pagedContent.pageLayout, pagedContent.hfLayout);
   
       // Construct full HTML for printing
   
       const fullHtml = MarkdownPrintTemplate({
          styles: pagedContent.styles,
          pageCss: pageCss,
          headerHtml: headerHtml,
          footerHtml: footerHtml,
          contentHtml: pagedContent.html,
          baseUrl: baseUrl
       });
       messageStore.add("Generating PDF...", "INFO");
        const filename = `markdown_${Date.now()}.pdf`;
        
        try {
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
            messageStore.add(`PDF Generated successfully!`, "SUCCESS");
   
        } catch (e: any) {
            console.error("Generate failed", e);
            messageStore.add("Failed: " + (e.message || e), "ERROR");
        }
    }

    function saveContent(editor: EditorInterface) {
        if (!editor) return;
        markdownStore.updateContent(editor.getValue());
    }

    return {
        handleRenderStats,
        debouncedTrigger,
        triggerPreview,
        clearDebounce,
        updatePreview,
        handleGenerate,
        saveContent
    };
}
