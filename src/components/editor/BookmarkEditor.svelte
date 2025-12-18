<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { EditorState, RangeSetBuilder, Compartment } from '@codemirror/state';
  import { EditorView, keymap, lineNumbers, highlightActiveLine, highlightActiveLineGutter, ViewPlugin, Decoration, type ViewUpdate, WidgetType } from '@codemirror/view';
  import { defaultKeymap, history, historyKeymap, indentWithTab } from '@codemirror/commands';
  import { indentOnInput, indentUnit } from '@codemirror/language';
  import { searchKeymap, highlightSelectionMatches } from '@codemirror/search';
  import { tocTheme, tocPlugin } from './tocPlugins';

  // --- Props ---
  interface Props {
    value?: string;
    placeholder?: string;
    disabled?: boolean;
    onchange?: (val: string, changedLines: number[]) => void;
    onFocus?: () => void;
    onBlur?: () => void;
  }

  let { 
    value = $bindable(''), 
    placeholder = '', 
    disabled = false,
    onchange,
    onFocus,
    onBlur
  }: Props = $props();

  let editorContainer: HTMLDivElement;
  let view: EditorView;
  let readOnlyCompartment = new Compartment();

  // --- Lifecycle ---
  
    onMount(() => {
      if (!editorContainer) return;
  
      const startState = EditorState.create({
        doc: value,
        extensions: [
          readOnlyCompartment.of(EditorState.readOnly.of(disabled)),
          history(),
          highlightActiveLine(),
          indentOnInput(),
          indentUnit.of("    "), 
          keymap.of([
              indentWithTab, 
              ...defaultKeymap, 
              ...historyKeymap, 
              ...searchKeymap
          ]),
          tocTheme,
          tocPlugin,
          EditorView.updateListener.of((update) => {
              if (update.docChanged) {
                  const newVal = update.state.doc.toString();
                  
                  const changedLines = new Set<number>();
                  update.changes.iterChanges((fromA, toA, fromB, toB, inserted) => {
                       // Get line numbers in the new document state
                       const startLine = update.state.doc.lineAt(fromB).number;
                       const endLine = update.state.doc.lineAt(toB).number;
                       for (let i = startLine; i <= endLine; i++) {
                           changedLines.add(i);
                       }
                  });
  
                  value = newVal;
                  onchange?.(newVal, Array.from(changedLines));
              }
          }),
          // Add DOM event handlers for focus and blur
          EditorView.domEventHandlers({
              focus: () => onFocus?.(),
              blur: () => onBlur?.(),
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
      if (view) {
          view.dispatch({
              effects: readOnlyCompartment.reconfigure(EditorState.readOnly.of(disabled))
          });
      }
  });

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
