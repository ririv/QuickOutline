import {PageLabelNumberingStyle, type PageLabel} from "@/lib/types/page-label.ts";
import { defaultPageLayout, type PageLayout, defaultHeaderFooterLayout, type HeaderFooterLayout, type SectionConfig } from "@/lib/types/page";

export class MarkdownState {
    // Content
    content = $state('# Hello CodeMirror 6\n\nTry typing **bold text** or *italic* here.\n\nMove cursor inside and outside the styled text to see the magic!\n\n$$\\int^2_1xdx$$');

    // Status Bar State
    insertionConfig = $state({
        pos: 1,
        autoCorrect: false,
        showAutoCorrect: false
    });
    pageLabel = $state<PageLabel>({
        pageIndex: 1,
        numberingStyle: PageLabelNumberingStyle.NONE,
        labelPrefix: '',
        startValue: 1
    });
    pageLayout = $state<PageLayout>({ ...defaultPageLayout });
    hfLayout = $state<HeaderFooterLayout>({ ...defaultHeaderFooterLayout });

    // Section Configs
    headerConfig = $state<SectionConfig>({ left: '', center: '', right: '', inner: '', outer: '', drawLine: false });
    footerConfig = $state<SectionConfig>({ left: '', center: '{p}', right: '', inner: '', outer: '', drawLine: false });

    // UI State
    showHeader = $state(false);
    showFooter = $state(false);
    enableIndentedCodeBlocks = $state(false); // New config option
    tableStyle = $state<'grid' | 'academic'>('grid'); // New config option
    
    // Preview Cache
    currentPagedContent = $state<any>(null);

    // Actions
    updateContent(newContent: string) {
        this.content = newContent;
    }
}

export const markdownStore = new MarkdownState();