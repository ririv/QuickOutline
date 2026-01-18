<script lang="ts">
    import Modal from './common/Modal.svelte';
    import Icon from './Icon.svelte';

    let { showHelpModal = $bindable() } = $props<{ showHelpModal: boolean }>();

    const APP_VERSION = __APP_VERSION__;
</script>

<Modal title="帮助" bind:show={showHelpModal}>
    <div class="help-content">
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
    </div>
</Modal>

<style>
    .help-content {
        padding: 10px 15px;
        display: flex;
        flex-direction: column;
        font-size: 14px;
        color: #555;
    }

    .info-list {
        display: flex;
        flex-direction: column;
        gap: 12px;
    }

    .info-item {
        display: flex;
        align-items: center;
        gap: 8px;
    }

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