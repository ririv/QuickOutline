<script lang="ts">
    import BaseModal from './common/BaseModal.svelte';
    import { confirmState } from '../stores/confirm.svelte';

    function handleConfirm() {
        confirmState.close(true);
    }

    function handleCancel() {
        confirmState.close(false);
    }

    // Icons Mapping
    const icons = {
        info: `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/></svg>`,
        warning: `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/><path d="M12 9v4"/><path d="M12 17h.01"/></svg>`,
        error: `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="m15 9-6 6"/><path d="m9 9 6 6"/></svg>`
    };
</script>

<BaseModal 
    isOpen={confirmState.isOpen} 
    onClose={handleCancel} 
    blur={true} 
    width="max-w-[420px]" 
    zIndex="z-[9999]"
>
    <div class="confirm-content-wrapper">
        <div class="confirm-icon-area {confirmState.type}">
            {@html icons[confirmState.type]}
        </div>
        
        <div class="confirm-text-area">
            <div class="confirm-title">{confirmState.title}</div>
            <div class="confirm-message">{confirmState.message}</div>
        </div>
    </div>

    <div class="confirm-footer">
        <button class="btn btn-secondary" onclick={handleCancel}>
            {confirmState.cancelText}
        </button>
        <button class="btn btn-primary {confirmState.type}" onclick={handleConfirm}>
            {confirmState.confirmText}
        </button>
    </div>
</BaseModal>

<style>
    .confirm-content-wrapper {
        padding: 24px 24px 20px 24px;
        display: flex;
        gap: 16px;
        align-items: flex-start;
    }

    .confirm-icon-area {
        width: 24px;
        height: 24px;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
        margin-top: 2px; /* 关键：微调以对齐 16px 标题的第一行 */
    }

    /* 针对三角形图标的视觉修正 */
    .confirm-icon-area.warning :global(svg) {
        transform: translateY(-1px);
    }

    /* Type Colors */
    .confirm-icon-area.info { color: #1677ff; }
    .confirm-icon-area.warning { color: #faad14; }
    .confirm-icon-area.error { color: #ff4d4f; }

    .confirm-text-area {
        flex: 1;
    }

    .confirm-title {
        font-size: 16px;
        font-weight: 600;
        color: #1f1f1f;
        margin-bottom: 8px;
    }

    .confirm-message {
        font-size: 14px;
        color: #595959;
        line-height: 1.57;
    }

    .confirm-footer {
        padding: 12px 16px;
        background-color: #ffffff;
        display: flex;
        justify-content: flex-end;
        gap: 8px;
        border-top: 1px solid #f0f0f0;
        /* Ensure rounded bottom corners match BaseModal if explicitly needed, 
           but BaseModal has overflow-hidden so it's fine */
    }

    .btn {
        padding: 5px 16px;
        border-radius: 6px;
        font-size: 14px;
        cursor: pointer;
        transition: all 0.2s;
        border: 1px solid #d9d9d9;
        background: #fff;
        color: #262626;
        height: 32px;
        font-weight: 400;
    }

    .btn-secondary:hover {
        color: #4096ff;
        border-color: #4096ff;
    }

    .btn-primary {
        background: #1677ff;
        color: #fff;
        border: none;
    }

    .btn-primary:hover { background: #4096ff; }
    
    .btn-primary.warning { background: #faad14; }
    .btn-primary.warning:hover { background: #ffc53d; }
    
    .btn-primary.error { background: #ff4d4f; }
    .btn-primary.error:hover { background: #ff7875; }
</style>