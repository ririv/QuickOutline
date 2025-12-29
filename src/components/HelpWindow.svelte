<script lang="ts">
    import Modal from './common/Modal.svelte';
    import TocGuide from './TocGuide.svelte';

    let { showHelpModal = $bindable() } = $props<{ showHelpModal: boolean }>();

    const APP_VERSION = '1.0.0'; // 从 package.json 获取

    let showGuide = $state(false);

    function browse(url: string) {
        window.open(url, '_blank');
    }

    function browseRemoteRepo() {
        browse("https://github.com/ririv/QuickOutline");
    }

    function browseHelpOnGithub() {
        browse("https://github.com/ririv/QuickOutline/blob/master/README.md");
    }

    function browseHelpOnZhihu() {
        browse("https://zhuanlan.zhihu.com/p/390719305");
    }
    
    function browseHelpOnMyPage() {
        browse("https://www.xiaohongshu.com/user/profile/5f988414000000000101ca29");
    }
</script>

<Modal title="帮助" bind:show={showHelpModal}>
    <div class="help-content" class:guide-mode={showGuide}>
        {#if !showGuide}
            <div class="info-list">
                <div class="info-item">
                    <span class="label">版本:</span>
                    <span class="value">{APP_VERSION}</span>
                </div>

                <div class="info-item">
                    <span class="label">用法说明:</span>
                    <a href="https://github.com/ririv/QuickOutline/blob/master/README.md" target="_blank" rel="noopener noreferrer">@Github</a>
                </div>

                <div class="info-item">
                    <span class="label">核心依赖:</span>
                    <span class="value">iText (AGPL 许可证)</span>
                </div>

                <div class="info-item">
                    <a href="https://github.com/ririv/QuickOutline" target="_blank" rel="noopener noreferrer">
                        <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-github"><path d="M15 22v-4a4.8 4.8 0 0 0-1-3.5c3 0 6-2 6-5.5.08-1.25-.27-2.44-.78-3.46 0 0-1.09-.35-3.5 1.5-2.67-1-5.33-1-8 0-2.41-1.85-3.5-1.5-3.5-1.5-.5.92-.78 2.03-.78 3.46 0 3.5 3 5.5 6 5.5-1 1-1 2.5-1 3.5v4"/><path d="M12 22c-5.523 0-10-4.477-10-10S6.477 2 12 2s10 4.477 10 10-4.477 10-10 10z"/></svg>
                        项目源码
                    </a>
                </div>

                <div class="info-item">
                    <span class="label">作者</span>
                    <a href="https://www.xiaohongshu.com/user/profile/5f988414000000000101ca29" target="_blank" rel="noopener noreferrer">关于我</a>
                    <a href="https://www.xiaohongshu.com/user/profile/5f988414000000000101ca29" target="_blank" rel="noopener noreferrer">
                        <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-link"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07L9.54 2.46c-.32.32-.63.64-.95.96a5 5 0 0 0 6.56 6.56l-3 3a5 5 0 0 0-7.54-.54Z"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/><path d="M11 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l3-3a5 5 0 0 0-7.54-.54Z"/></svg>
                        小红书
                    </a>
                </div>
            </div>
        {/if}

        <div class="guide-toggle" class:is-top={showGuide}>
            <button class="link-btn" onclick={() => showGuide = !showGuide}>
                {showGuide ? '← 返回关于' : '📖 查看目录语法指南'}
            </button>
        </div>

        {#if showGuide}
            <div class="guide-separator"></div>
            <TocGuide />
        {/if}
    </div>
</Modal>

<style>
    .help-content {
        padding: 5px 10px;
        display: flex;
        flex-direction: column;
        font-size: 14px;
        color: #555;
    }

    .info-list {
        display: flex;
        flex-direction: column;
        gap: 12px;
        padding-bottom: 10px;
    }

    .guide-mode {
        padding-top: 0;
    }

    .guide-toggle {
        display: flex;
        justify-content: center;
        padding: 8px 0;
    }

    .guide-toggle:not(.is-top) {
        border-top: 1px solid #eee;
        margin-top: 5px;
    }

    .guide-toggle.is-top {
        padding: 0 0 5px 0;
        justify-content: flex-start;
    }

    .guide-separator {
        border-top: 1px solid #eee;
        margin: 0;
    }

    .link-btn {
        background: transparent;
        border: none;
        color: #1677ff;
        cursor: pointer;
        font-size: 13px;
        padding: 2px 8px;
        border-radius: 4px;
        transition: background-color 0.2s;
    }

    .link-btn:hover {
        background-color: rgba(22, 119, 255, 0.05);
        text-decoration: underline;
    }

    .info-item {
        display: flex;
        align-items: center;
        gap: 8px;
    }

    .label {
        font-weight: bold;
        color: #333;
        min-width: 80px; /* Aligns labels */
    }

    .value {
        color: #666;
    }

    /* Styling for Hyperlink to match buttons or have a distinct look */
    .info-item a {
        color: #1677ff;
        text-decoration: none;
        display: flex;
        align-items: center;
        gap: 4px;
        transition: color 0.2s;
    }

    .info-item a:hover {
        color: #409eff;
        text-decoration: underline;
    }
</style>
