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
    content = $state('');

    // Status Bar State
    insertPos = $state(1);
    style = $state('None');

    // Section Configs
    headerConfig = $state<SectionConfig>({ left: '', center: '', right: '', inner: '', outer: '', drawLine: false });
    footerConfig = $state<SectionConfig>({ left: '', center: '{p}', right: '', inner: '', outer: '', drawLine: false });

    // UI State
    showHeader = $state(false);
    showFooter = $state(false);
    
    // Preview Cache
    currentPagedPayload = $state<any>(null);

    // Actions
    updateContent(newContent: string) {
        this.content = newContent;
    }
}

export const markdownStore = new MarkdownState();
