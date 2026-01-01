<script lang="ts">
    import Modal from './common/Modal.svelte';
    import TocGuide from './TocGuide.svelte';
    import Icon from './Icon.svelte';

    let { showHelpModal = $bindable() } = $props<{ showHelpModal: boolean }>();

    const APP_VERSION = __APP_VERSION__;

    let showGuide = $state(false);
</script>

<Modal title="帮助" bind:show={showHelpModal}>
    <div class="help-content" class:guide-mode={showGuide}>
        {#if !showGuide}
            <div class="info-list">
                <div class="info-item">
                    Version {APP_VERSION}
                </div>

                <div class="info-item">
                    <a href="https://github.com/ririv/QuickOutline" target="_blank" rel="noopener noreferrer">
                        <Icon name="github" width="16" height="16" style="color: #666;" />
                        Source
                    </a>
                </div>

                <div class="info-item">
                    By
                    <a href="https://www.xiaohongshu.com/user/profile/5f988414000000000101ca29" target="_blank" rel="noopener noreferrer">
                        xhh
                        <Icon name="xiaohongshu" width="16" height="16" style="color: #ff2442;" />
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
