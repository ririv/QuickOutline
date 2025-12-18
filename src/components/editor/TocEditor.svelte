<script lang="ts">
    import { onMount, onDestroy } from 'svelte';
    import { EditorState, RangeSetBuilder } from '@codemirror/state';
    import { EditorView, keymap, lineNumbers, highlightActiveLine, highlightActiveLineGutter, ViewPlugin, Decoration, type ViewUpdate, WidgetType } from '@codemirror/view';
    import { defaultKeymap, history, historyKeymap, indentWithTab } from '@codemirror/commands';
    import { indentOnInput, indentUnit } from '@codemirror/language';
    import { searchKeymap, highlightSelectionMatches } from '@codemirror/search';
    import { tocTheme, tocPlugin } from './tocPlugins';

    // --- Props ---
    interface Props {
        value?: string;
        placeholder?: string;
        onchange?: (val: string) => void;
    }

    let {
        value = $bindable(''),
        placeholder = '',
        onchange
    }: Props = $props();

    let editorContainer: HTMLDivElement;
    let view: EditorView;


    // --- Lifecycle ---

    onMount(() => {
        if (!editorContainer) return;

        const startState = EditorState.create({
            doc: value,
            extensions: [
                history(),
                highlightActiveLine(),
                // highlightActiveLineGutter(), // Remove potentially layout-shifting gutter
                // lineNumbers(), // Optional, maybe noisy for TOC
                indentOnInput(),
                indentUnit.of("    "), // Set indentation to 4 spaces
                keymap.of([
                    indentWithTab, // Standard Tab/Shift-Tab behavior
                    ...defaultKeymap,
                    ...historyKeymap,
                    ...searchKeymap
                ]),
                tocTheme,
                tocPlugin,
                EditorView.updateListener.of((update) => {
                    if (update.docChanged) {
                        const newVal = update.state.doc.toString();
                        value = newVal;
                        onchange?.(newVal);
                    }
                })
            ]
        });

        view = new EditorView({
            state: startState,
            parent: editorContainer
        });
    });

    onDestroy(() => {
        if (view) {
            view.destroy();
        }
    });

    // React to prop updates
    $effect(() => {
        if (view && value !== view.state.doc.toString()) {
            // Only update if content matches (avoid cursor jump loop)
            // But this is tricky with binding.
            // Usually simple way:
            const current = view.state.doc.toString();
            if (value !== current) {
                view.dispatch({
                    changes: { from: 0, to: current.length, insert: value }
                });
            }
        }
    });

</script>

<div class="toc-editor-wrapper" bind:this={editorContainer}></div>

<style>
    .toc-editor-wrapper {
        width: 100%;
        height: 100%;
        overflow: hidden; /* CM handles scroll */
        background: transparent;
    }

    :global(.cm-editor) {
        height: 100%;
        background: transparent;
    }

    :global(.cm-scroller) {
        font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
    }

    /* Remove default outline */
    :global(.cm-focused) {
        outline: none !important;
    }
</style>
