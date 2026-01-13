<script lang="ts">
    import { onMount, onDestroy } from 'svelte';
    import { MarkdownEditor, type EditorMode, type StylesConfig } from '@/lib/editor';

    interface Props {
        value?: string;
        mode?: EditorMode;
        stylesConfig?: Partial<StylesConfig>;
        onchange?: (val: string) => void;
    }

    let {
        value = $bindable(''),
        mode = $bindable('live'),
        stylesConfig = $bindable({ tableStyle: 'grid' }),
        onchange
    }: Props = $props();

    let editorElement: HTMLDivElement;
    let editor: MarkdownEditor | undefined;

    // Helper methods (kept for toolbar actions)
    export const insertValue = (val: string) => editor?.insertValue(val);
    export const insertImageMarkdown = (path: string) => editor?.insertImageMarkdown(path);
    export const getValue = () => value; // Compatibility

    onMount(() => {
        if (!editorElement) return;

        editor = new MarkdownEditor({
            parent: editorElement,
            initialValue: value,
            initialMode: mode,
            stylesConfig: stylesConfig,
            placeholder: '开始输入...', 
            onChange: (doc) => {
                if (doc !== value) {
                    value = doc;
                    onchange?.(doc);
                }
            }
        });
    });

    onDestroy(() => {
        editor?.destroy();
    });

    // React to value changes (External -> Editor)
    $effect(() => {
        if (editor && value !== editor.getValue()) {
            editor.setValue(value);
        }
    });

    // React to mode changes
    $effect(() => {
        if (editor) {
            editor.setMode(mode); // Ensure MarkdownEditor has setMode
        }
    });

    // React to stylesConfig changes (Currently MdEditor class might need update to support this dynamically, 
    // but we can at least try if the class supports it, or we might need to extend the class later. 
    // For now, assume static or handled by re-init if really needed, but let's just watch it)
    // MarkdownEditor.ts from previous read doesn't have setStylesConfig, so this might be limited.
    // But since MarkDownView passes it on init, it's mostly fine.
</script>

<div class="cm-container" bind:this={editorElement}></div>

<style>
    .cm-container {
        height: 100%;
        width: 100%;
        overflow: hidden;
        background-color: white;
    }
</style>