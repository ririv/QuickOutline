<script lang="ts">
    import { onMount, onDestroy } from 'svelte';
    import { MarkdownEditor, type EditorMode } from '@/lib/editor';

    let editorElement: HTMLDivElement;
    let editor: MarkdownEditor;

    // Props
    export let onchange: ((val: string) => void) | undefined = undefined;

    export const getValue = () => editor?.getValue() || '';
    export const setValue = (val: string) => editor?.setValue(val);
    export const insertValue = (val: string) => editor?.insertValue(val);
    export const insertImageMarkdown = (path: string) => editor?.insertImageMarkdown(path);
    export const getContentHtml = async () => editor?.getHTML() || '';
    export const getPayloads = async () => JSON.stringify({ html: editor?.getHTML() || "", styles: "" });
    export const setMode = (mode: EditorMode) => editor?.setMode(mode); // Expose setMode

    // Init function (exposed)
    export const init = (initialMarkdown: string = '', initialMode: EditorMode = 'live') => {
        if (editor) return;
        editor = new MarkdownEditor({
            parent: editorElement,
            initialValue: initialMarkdown,
            placeholder: '开始输入...',
            initialMode: initialMode,
            onChange: (doc) => {
                if (onchange) onchange(doc);
            }
        });
    };

    onMount(() => {
        // Ensure init() is called here with default values
        init('# Hello CodeMirror 6\n\nTry typing **bold text** or *italic* here.\n\nMove cursor inside and outside the styled text to see the magic!');

        return () => {
             // Clean up any other manual listeners here if added later.
        };
    });

    onDestroy(() => {
        editor?.destroy();
    });
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