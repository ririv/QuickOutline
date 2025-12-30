<script lang="ts">
    import { confirmState } from '../stores/confirm.svelte';
    import { fade, scale } from 'svelte/transition';
    import { cubicOut } from 'svelte/easing';

    function handleConfirm() {
        confirmState.close(true);
    }

    function handleCancel() {
        confirmState.close(false);
    }

    // Close on backdrop click
    function handleBackdropClick(e: MouseEvent | KeyboardEvent) {
        if (e.target === e.currentTarget) {
            handleCancel();
        }
    }

    // Icons Mapping
    const icons = {
        info: `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/></svg>`,
        warning: `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/><path d="M12 9v4"/><path d="M12 17h.01"/></svg>`,
        error: `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="m15 9-6 6"/><path d="m9 9 6 6"/></svg>`
    };
</script>

{#if confirmState.isOpen}
    <div 
        class="confirm-backdrop"
        onclick={handleBackdropClick}
        onkeydown={(e) => { if (e.key === 'Enter' || e.key === ' ') handleBackdropClick(e); }}
        role="button"        
        tabindex="0"         
        transition:fade={{ duration: 150 }}
    >
        <div 
            class="confirm-modal"
            transition:scale={{ start: 0.96, duration: 150, easing: cubicOut }}
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
        </div>
    </div>
{/if}

<style>
    .confirm-backdrop {
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background-color: rgba(0, 0, 0, 0.4);
        backdrop-filter: blur(2px);
        z-index: 9999;
        display: flex;
        align-items: center;
        justify-content: center;
    }

    .confirm-modal {
        background: white;
        width: 420px;
        max-width: 90%;
        border-radius: 12px;
        box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
        overflow: hidden;
        display: flex;
        flex-direction: column;
    }

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