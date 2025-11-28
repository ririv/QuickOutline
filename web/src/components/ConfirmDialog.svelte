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
    function handleBackdropClick(e: MouseEvent) {
        if (e.target === e.currentTarget) {
            handleCancel();
        }
    }
</script>

{#if confirmState.isOpen}
    <!-- Backdrop -->
    <div 
        class="confirm-backdrop"
        onclick={handleBackdropClick}
        onkeydown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') {
                handleBackdropClick(e);
            }
        }}
        role="button"        
        tabindex="0"         
        transition:fade={{ duration: 200 }}
    >
        <!-- Modal Card -->
        <div 
            class="confirm-modal"
            transition:scale={{ start: 0.95, duration: 200, easing: cubicOut }}
        >
            <!-- Header -->
            <div class="confirm-header">
                <span class="confirm-title">{confirmState.title}</span>
            </div>

            <!-- Body -->
            <div class="confirm-body">
                <!-- Icon (Optional based on type) -->
                {#if confirmState.type === 'warning'}
                    <span class="confirm-icon warning">⚠️</span>
                {/if}
                <p>{confirmState.message}</p>
            </div>

            <!-- Footer -->
            <div class="confirm-footer">
                <button class="btn btn-secondary" onclick={handleCancel}>
                    {confirmState.cancelText}
                </button>
                <button class="btn btn-primary" onclick={handleConfirm}>
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
        backdrop-filter: blur(4px); /* 毛玻璃效果 */
        z-index: 9999; /* 确保在最顶层 */
        display: flex;
        align-items: center;
        justify-content: center;
    }

    .confirm-modal {
        background: white;
        width: 400px;
        max-width: 90%;
        border-radius: 8px;
        box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
        overflow: hidden;
        display: flex;
        flex-direction: column;
        border: 1px solid rgba(0,0,0,0.05);
    }

    .confirm-header {
        padding: 16px 20px;
        border-bottom: 1px solid #f0f0f0;
    }

    .confirm-title {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
    }

    .confirm-body {
        padding: 20px;
        font-size: 14px;
        color: #606266;
        line-height: 1.5;
        display: flex;
        align-items: flex-start;
        gap: 12px;
    }

    .confirm-icon.warning {
        font-size: 20px;
    }

    .confirm-footer {
        padding: 12px 20px;
        background-color: #f9fafb;
        display: flex;
        justify-content: flex-end;
        gap: 12px;
        border-top: 1px solid #f0f0f0;
    }

    /* Buttons */
    .btn {
        padding: 8px 16px;
        border-radius: 4px;
        font-size: 14px;
        cursor: pointer;
        transition: all 0.2s;
        border: 1px solid transparent;
        font-weight: 500;
    }

    .btn-primary {
        background-color: var(--color-el-primary, #409eff);
        color: white;
        box-shadow: 0 2px 4px rgba(64, 158, 255, 0.2);
    }

    .btn-primary:hover {
        background-color: var(--color-el-primary-pressed, #66b1ff);
    }

    .btn-secondary {
        background-color: white;
        border-color: #dcdfe6;
        color: #606266;
    }

    .btn-secondary:hover {
        background-color: #ecf5ff;
        color: var(--color-el-primary, #409eff);
        border-color: #c6e2ff;
    }
</style>
