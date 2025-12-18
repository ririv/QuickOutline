<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { EditorState, RangeSetBuilder, Compartment } from '@codemirror/state';
  import { EditorView, keymap, lineNumbers, highlightActiveLine, highlightActiveLineGutter, ViewPlugin, Decoration, type ViewUpdate, WidgetType } from '@codemirror/view';
  import { defaultKeymap, history, historyKeymap, indentWithTab } from '@codemirror/commands';
  import { indentOnInput, indentUnit } from '@codemirror/language';
  import { searchKeymap, highlightSelectionMatches } from '@codemirror/search';
  import {tocTheme, tocPlugin, pageValidationConfig, pageValidationExtension, lineTheme} from './tocPlugins';

  // --- Props ---
  interface Props {
    value?: string;
    placeholder?: string;
    disabled?: boolean;
    offset?: number;
    totalPage?: number;
    onchange?: (val: string, changedLines: number[]) => void;
    onFocus?: () => void;
    onBlur?: () => void;
  }

  let { 
    value = $bindable(''), 
    placeholder = '', 
    disabled = false,
    offset = 0,
    totalPage = 0,
    onchange,
    onFocus,
    onBlur
  }: Props = $props();

  let editorContainer: HTMLDivElement;
  let view: EditorView;
  let readOnlyCompartment = new Compartment();
  let validationConf = new Compartment();

  // --- Lifecycle ---
  
    onMount(() => {
      if (!editorContainer) return;
  
      const startState = EditorState.create({
        doc: value,
        extensions: [
          readOnlyCompartment.of(EditorState.readOnly.of(disabled)),
          validationConf.of(pageValidationConfig.of({ offset, totalPage })),
          lineTheme,
          pageValidationExtension,
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
      if (view) {
          view.dispatch({
              effects: validationConf.reconfigure(pageValidationConfig.of({ offset, totalPage }))
          });
      }
  });

  $effect(() => {
      if (view && value !== view.state.doc.toString()) {
          const current = view.state.doc.toString();
          if (value === current) return; // No actual change, skip dispatch

          // Implement a character-based head-tail matching for incremental updates
          let start = 0;
          let oldEnd = current.length;
          let newEnd = value.length;

          // Find common prefix
          while (start < oldEnd && start < newEnd && current.charCodeAt(start) === value.charCodeAt(start)) {
              start++;
          }

          // Find common suffix
          while (oldEnd > start && newEnd > start && current.charCodeAt(oldEnd - 1) === value.charCodeAt(newEnd - 1)) {
              oldEnd--;
              newEnd--;
          }
          
          view.dispatch({
              changes: {
                  from: start,
                  to: oldEnd,
                  insert: value.slice(start, newEnd)
              }
          });
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
