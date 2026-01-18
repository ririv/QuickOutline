<script lang="ts">
    import Modal from './common/Modal.svelte';
    import Icon from './Icon.svelte';

    let { showAboutModal = $bindable() } = $props<{ showAboutModal: boolean }>();

    const APP_VERSION = __APP_VERSION__;
</script>

<Modal title="About" bind:show={showAboutModal}>
    <div class="about-content">
        <div class="product-header">
            <div class="logo-wrapper">
                <img src="/icon.png" alt="QuickOutline Logo" class="app-logo" />
            </div>
            <h2 class="app-name">QuickOutline</h2>
            <div class="app-version">Version {APP_VERSION}</div>
        </div>

        <div class="info-sections">
            <div class="info-section">
                <div class="section-label">Created by</div>
                <div class="section-content">
                    <a href="https://www.xiaohongshu.com/user/profile/5f988414000000000101ca29" target="_blank" rel="noopener noreferrer" class="author-link">
                        xhh
                        <Icon name="xiaohongshu" width="14" height="14" style="color: #ff2442;" />
                    </a>
                </div>
            </div>

            <div class="info-section">
                <div class="section-label">Source</div>
                <div class="section-content">
                    <a href="https://github.com/ririv/QuickOutline" target="_blank" rel="noopener noreferrer" class="source-link">
                        <Icon name="github" width="16" height="16" />
                        GitHub
                    </a>
                </div>
            </div>
        </div>
        
        <div class="footer-note">
            © 2025-2026 ririv
        </div>
    </div>
</Modal>

<style>
    .about-content {
        padding: 20px 24px;
        display: flex;
        flex-direction: column;
        align-items: center;
        min-width: 280px;
    }

    .product-header {
        display: flex;
        flex-direction: column;
        align-items: center;
        margin-bottom: 24px;
    }

    .logo-wrapper {
        width: 64px;
        height: 64px;
        background: #fff;
        border-radius: 16px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 12px;
        overflow: hidden;
    }

    .app-logo {
        width: 100%;
        height: 100%;
        object-fit: cover;
    }

    .app-name {
        margin: 0;
        font-size: 20px;
        font-weight: 600;
        color: #1a1a1a;
    }

    .app-version {
        font-size: 13px;
        color: #888;
        margin-top: 4px;
        font-family: 'Consolas', monospace;
    }

    .info-sections {
        width: 100%;
        display: flex;
        flex-direction: column;
        gap: 16px;
        padding: 16px 0;
        border-top: 1px solid #f0f0f0;
    }

    .info-section {
        display: flex;
        justify-content: space-between;
        align-items: center;
        width: 100%;
    }

    .section-label {
        font-size: 13px;
        color: #999;
    }

    .section-content {
        display: flex;
        align-items: center;
    }

    .section-content a {
        font-size: 14px;
        color: #1677ff;
        text-decoration: none;
        display: flex;
        align-items: center;
        gap: 6px;
        transition: all 0.2s;
        padding: 4px 8px;
        border-radius: 6px;
    }

    .section-content a:hover {
        background-color: #f0f7ff;
        color: #409eff;
    }

    .source-link {
        color: #333 !important;
    }
    
    .source-link:hover {
        background-color: #f5f5f5 !important;
    }

    .footer-note {
        margin-top: 16px;
        font-size: 11px;
        color: #bbb;
    }
</style>
