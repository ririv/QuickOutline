import { PageLabelNumberingStyle } from "@/lib/styleMaps";

interface SectionConfig {
    left: string;
    center: string;
    right: string;
    inner: string;
    outer: string;
    drawLine: boolean;
}

export class TocState {
    // File association
    filePath = $state<string | null>(null);
    
    // Content
    content = $state('');
    
    // Configuration
    title = $state('Table of Contents');
    offset = $state(0);
    insertPos = $state(1);
    numberingStyle = $state(PageLabelNumberingStyle.NONE);
    
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
        this.offset = 0;
        this.insertPos = 1;
        this.numberingStyle = PageLabelNumberingStyle.NONE;
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
