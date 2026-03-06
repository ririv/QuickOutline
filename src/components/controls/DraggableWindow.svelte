<script lang="ts">
    import { draggable } from '@/lib/actions/draggable';
    import { portal } from '@/lib/actions/portal';
    import { fade } from 'svelte/transition';
    import Icon from '@/components/Icon.svelte';

        interface Props {
            visible: boolean;
            title: string;
            onClose: () => void;
            children: import('svelte').Snippet;
            actions?: import('svelte').Snippet;
            width?: string;
            height?: string;
            initialX?: number;
            initialY?: number;
        }
    
        let {
            visible = $bindable(false),
            title,
            onClose,
            children,
            actions,
            width = '400px',
            height = 'auto',
            initialX = 100,
            initialY = 100
        }: Props = $props();
    
        function handleClose() {
            visible = false;
            onClose?.();
        }
    </script>
    
    {#if visible}
        <div 
            class="draggable-window" 
            use:portal 
            use:draggable={{ handle: '.window-header' }}
            style:left="{initialX}px"
            style:top="{initialY}px"
            style:width={width}
            style:height={height}
            transition:fade={{ duration: 150 }}
        >
            <div class="window-header">
                <span class="window-title">{title}</span>
                <div class="window-actions">
                    {@render actions?.()}
                    <button class="close-btn" onclick={handleClose} title="Close">
                        <Icon name="add" width="14" height="14" style="transform: rotate(45deg);" />
                    </button>
                </div>
            </div>
            <div class="window-content">
                {@render children()}
            </div>
        </div>
    {/if}
    
    <style>
        .draggable-window {
            position: fixed;
            background: white;
            border: 1px solid #dcdfe6;
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
            border-radius: 8px;
            display: flex;
            flex-direction: column;
            z-index: 2000; /* High enough to be above status bar but maybe below some modals */
            overflow: hidden;
        }
    
        .window-header {
            height: 36px;
            background: #f5f7fa;
            border-bottom: 1px solid #e4e7ed;
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 0 12px;
            cursor: move;
            flex-shrink: 0;
            user-select: none;
        }
    
        .window-title {
            font-size: 13px;
            font-weight: 600;
            color: #303133;
        }
    
        .window-actions {
            display: flex;
            align-items: center;
            gap: 8px;
        }
    
        .close-btn {
            background: transparent;
            border: none;
            cursor: pointer;
            color: #909399;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 4px;
            border-radius: 4px;
            transition: all 0.2s;
        }
    
        .close-btn:hover {
            background-color: #e4e7ed;
            color: #f56c6c;
        }
    
        .window-content {
            flex: 1;
            overflow: auto;
            padding: 0;
            position: relative;
            -webkit-user-select: text; /* Safari support */
            user-select: text;
            cursor: auto;
        }
    </style>
