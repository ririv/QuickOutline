import {PageLabelNumberingStyle} from "@/lib/styleMaps";

interface SectionConfig {
    left: string;
    center: string;
    right: string;
    inner: string;
    outer: string;
    drawLine: boolean;
}

export class MarkdownState {
    // Content
    content = $state('# Hello CodeMirror 6\n\nTry typing **bold text** or *italic* here.\n\nMove cursor inside and outside the styled text to see the magic!\n\n$$\\int^2_1xdx$$');

    // Status Bar State
    insertPos = $state(1);
    numberingStyle = $state(PageLabelNumberingStyle.NONE);

    // Section Configs
    headerConfig = $state<SectionConfig>({ left: '', center: '', right: '', inner: '', outer: '', drawLine: false });
    footerConfig = $state<SectionConfig>({ left: '', center: '{p}', right: '', inner: '', outer: '', drawLine: false });

    // UI State
    showHeader = $state(false);
    showFooter = $state(false);
    enableIndentedCodeBlocks = $state(false); // New config option
    
    // Preview Cache
    currentPagedPayload = $state<any>(null);

    // Actions
    updateContent(newContent: string) {
        this.content = newContent;
    }
}

export const markdownStore = new MarkdownState();