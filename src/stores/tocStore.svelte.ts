import { PageLabelNumberingStyle, type PageLabel } from "@/lib/types/page-label.ts";
import { defaultPageLayout, type PageLayout, defaultHeaderFooterLayout, type HeaderFooterLayout, type SectionConfig } from "@/lib/types/page";
import { offsetStore } from "./offsetStore.svelte";

export class TocState {
    // File association
    filePath = $state<string | null>(null);
    
    // Content
    content = $state('');
    
    // Configuration
    title = $state('Table of Contents');
    
    // Proxy offset to the global offsetStore
    get offset() {
        return offsetStore.value;
    }

    set offset(val: number) {
        offsetStore.set(val);
    }
    
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
    
    headerConfig = $state<SectionConfig>({ left: '', center: '', right: '', inner: '', outer: '', drawLine: false });
    footerConfig = $state<SectionConfig>({ left: '', center: '{p}', right: '', inner: '', outer: '', drawLine: false });

    // Cache
    previewData = $state<any>(null);
    scrollTop = $state(0);

    // Helpers for content update
    updateContent(newContent: string) {
        this.content = newContent;
    }

    // Load new file: reset everything to defaults, set new path and content
    setFile(path: string | null, initialContent: string = '') {
        if (this.filePath !== path) {
            this.filePath = path;
            this.content = initialContent;
            this.resetConfig();
            this.previewData = null; // Clear preview cache
            this.scrollTop = 0; // Reset scroll
        }
    }
    
    // Reset configuration to defaults
    resetConfig() {
        this.title = 'Table of Contents';
        // offset is managed globally by offsetStore now, so we don't reset it here.
        this.insertionConfig = {
            pos: 1,
            autoCorrect: false,
            showAutoCorrect: false
        };
        this.pageLabel = {
            pageIndex: 1,
            numberingStyle: PageLabelNumberingStyle.NONE,
            labelPrefix: '',
            startValue: 1
        };
        this.pageLayout = { ...defaultPageLayout };
        this.hfLayout = { ...defaultHeaderFooterLayout };
        this.headerConfig = { left: '', center: '', right: '', inner: '', outer: '', drawLine: false };
        this.footerConfig = { left: '', center: '{p}', right: '', inner: '', outer: '', drawLine: false };
    }

    // Check if store has valid content for the given path
    hasContentFor(path: string | null) {
        return this.filePath === path && (this.content.length > 0 || this.filePath !== null); 
        // Relaxed check: if paths match, we trust the store state (even if content is empty, maybe user cleared it)
    }
    
    getContent() {
        return this.content;
    }
}

export const tocStore = new TocState();
