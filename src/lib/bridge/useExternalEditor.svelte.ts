import { listen, type UnlistenFn } from '@tauri-apps/api/event';
import { openExternalEditor as openExternalEditorApi } from '@/lib/api/rust_pdf';
import { bookmarkStore } from '@/stores/bookmarkStore.svelte';
import { appStore } from '@/stores/appStore.svelte';
import { messageStore } from '@/stores/messageStore.svelte';
import { setContext, getContext, onMount, onDestroy } from 'svelte';

export type ExternalEditorType = 'auto' | 'code' | 'code-insiders' | 'zed';

export const EXTERNAL_EDITOR_MAP: Record<ExternalEditorType, string> = {
    'auto': 'VS Code',
    'code': 'VS Code',
    'code-insiders': 'VS Code Insiders',
    'zed': 'Zed'
};

const BRIDGE_KEY = Symbol('EXTERNAL_EDITOR_BRIDGE');

export class ExternalEditorBridge {
    isEditing = $state(false);
    private unlistenFns: UnlistenFn[] = [];

    editorName = $derived(EXTERNAL_EDITOR_MAP[appStore.externalEditor as ExternalEditorType] || 'VS Code');

    /**
     * Internal initialization.
     */
    async _init() {
        if (this.unlistenFns.length > 0) return;

        this.unlistenFns.push(await listen<string>('external-editor-sync', (event) => {
            console.log('ExternalEditorBridge: Received sync');
            bookmarkStore.setText(event.payload);
        }));

        this.unlistenFns.push(await listen('external-editor-start', () => {
            this.isEditing = true;
            messageStore.add('External editor connected.', 'INFO');
        }));

        this.unlistenFns.push(await listen('external-editor-end', () => {
            this.isEditing = false;
            messageStore.add('External editor disconnected.', 'INFO');
        }));

        this.unlistenFns.push(await listen<string>('external-editor-error', (event) => {
            this.isEditing = false;
            messageStore.add(event.payload, 'ERROR');
        }));
    }

    async open(content: string, line: number = 1, col: number = 1) {
        if (!content) {
            messageStore.add('Content is empty, nothing to open.', 'WARNING');
            return;
        }
        try {
            await openExternalEditorApi(content, line, col, appStore.externalEditor);
        } catch (e: any) {
            messageStore.add('Failed to launch external editor: ' + (e.message || String(e)), 'ERROR');
        }
    }

    /**
     * Internal cleanup.
     */
    _destroy() {
        this.unlistenFns.forEach(un => un());
        this.unlistenFns = [];
    }
}

/**
 * Provides an ExternalEditorBridge instance and automatically manages its lifecycle (init/destroy).
 */
export function provideExternalEditor() {
    const bridge = new ExternalEditorBridge();
    setContext(BRIDGE_KEY, bridge);
    
    // Automatically handle lifecycle
    onMount(() => {
        bridge._init();
    });

    onDestroy(() => {
        bridge._destroy();
    });

    return bridge;
}

/**
 * Consumes the ExternalEditorBridge instance from the context.
 */
export function useExternalEditor(): ExternalEditorBridge {
    const bridge = getContext<ExternalEditorBridge>(BRIDGE_KEY);
    if (!bridge) {
        throw new Error('useExternalEditor must be used within a component that has been provided with a bridge.');
    }
    return bridge;
}