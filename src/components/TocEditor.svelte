<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { EditorState, RangeSetBuilder } from '@codemirror/state';
  import { EditorView, keymap, lineNumbers, highlightActiveLine, highlightActiveLineGutter, ViewPlugin, Decoration, type ViewUpdate, WidgetType } from '@codemirror/view';
  import { defaultKeymap, history, historyKeymap } from '@codemirror/commands';
  import { indentOnInput } from '@codemirror/language';
  import { searchKeymap, highlightSelectionMatches } from '@codemirror/search';

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

  // --- TOC Decoration Plugin ---
  
  class LeaderWidget extends WidgetType {
    readonly page: string;

    constructor(page: string) { 
      super(); 
      this.page = page;
    }

    toDOM() {
      const span = document.createElement("span");
      span.className = "toc-leader-widget";
      
      const dots = document.createElement("span");
      dots.className = "toc-leader-dots";
      dots.innerHTML = "&nbsp;"; // Add a non-breaking space to ensure content
      span.appendChild(dots);

      const pageNum = document.createElement("span");
      pageNum.className = "toc-leader-page";
      pageNum.textContent = this.page;
      span.appendChild(pageNum);

      return span;
    }

    ignoreEvent() { return false; }

    eq(other: LeaderWidget) { return other.page == this.page; }
  }

  // Match: Title + (at least 1 space/tab) + PageNumber(digits) + EndOfLine
  // Aggressive: Any whitespace before trailing digits triggers formatting
  const tocLineRegex = /^(.*?)(\s+)(\d+)$/;

  const tocTheme = EditorView.baseTheme({
    ".cm-content": {
        fontFamily: "'Consolas', 'Monaco', 'Courier New', monospace",
        fontSize: "14px",
        lineHeight: "1.6",
        padding: "16px 22px"
    },
    ".cm-line": {
        display: "flex !important", 
        alignItems: "baseline",
        width: "100%",
        padding: "0"
    },
    ".toc-leader-widget": {
        display: "flex",
        flexGrow: "1",
        alignItems: "baseline",
        // marginLeft: "4px", // 根据最新要求，移除此行
        cursor: "default",
        userSelect: "none",
        animation: "leaderFadeIn 0.3s ease-out forwards" // 添加淡入动画
    },
    ".toc-leader-dots": {
        flexGrow: "1",
        margin: "0 1px", /* 左右间距调整为1px */
        backgroundImage: "radial-gradient(circle, #aaa 1px, transparent 1px)",
        backgroundSize: "4px 4px",
        backgroundRepeat: "repeat-x",
        backgroundPosition: "left bottom 4px",
        minHeight: "1em", 
        display: "block",
        opacity: "0.6"
    },
    ".toc-leader-page": {
        fontWeight: "bold",
        flexShrink: "0"
    },
    "&.cm-focused": {
        outline: "none"
    },
    ".cm-activeLine": {
        backgroundColor: "transparent" // Disable default active line bg if unwanted
    },
    ".cm-activeLineGutter": {
        backgroundColor: "transparent"
    }
  });

  const tocPlugin = ViewPlugin.fromClass(class {
    decorations: any;

    constructor(view: EditorView) {
      this.decorations = this.computeDecorations(view);
    }

    update(update: ViewUpdate) {
      if (update.docChanged || update.viewportChanged || update.selectionSet) {
        this.decorations = this.computeDecorations(update.view);
      }
    }

    computeDecorations(view: EditorView) {
      const builder = new RangeSetBuilder<Decoration>();
      
      // Get current cursor line(s) to exclude them from formatting
      const selection = view.state.selection;
      const cursorLines = new Set<number>();
      for (const range of selection.ranges) {
          const startLine = view.state.doc.lineAt(range.from).number;
          const endLine = view.state.doc.lineAt(range.to).number;
          for (let i = startLine; i <= endLine; i++) {
              cursorLines.add(i);
          }
      }

      for (const { from, to } of view.visibleRanges) {
        for (let pos = from; pos <= to;) {
          const line = view.state.doc.lineAt(pos);
          
          // Only decorate if cursor is NOT on this line
          if (!cursorLines.has(line.number)) {
              const text = line.text;
              const match = text.match(tocLineRegex);
              
              if (match) {
                  // match[1] = title (keep)
                  // match[2] = separator spaces (replace)
                  // match[3] = page number (replace -> move into widget)
                  
                  const titleLen = match[1].length;
                  const sepLen = match[2].length;
                  const pageLen = match[3].length;
                  
                  const sepStart = line.from + titleLen;
                  const lineEnd = line.from + text.length;
                  
                  // Replace [Spaces + Page] with [LeaderWidget(Page)]
                  builder.add(
                      sepStart, 
                      lineEnd, 
                      Decoration.replace({
                          widget: new LeaderWidget(match[3]),
                          inclusive: true // Include text in replacement
                      })
                  );
              }
          }
          
          pos = line.to + 1;
        }
      }
      return builder.finish();
    }
  }, {
    decorations: v => v.decorations
  });

  // --- Lifecycle ---

  onMount(() => {
    if (!editorContainer) return;

    const startState = EditorState.create({
      doc: value,
      extensions: [
        history(),
        highlightActiveLine(),
        highlightActiveLineGutter(), // Optional
        // lineNumbers(), // Optional, maybe noisy for TOC
        indentOnInput(),
        keymap.of([
            ...defaultKeymap, 
            ...historyKeymap, 
            ...searchKeymap,
            {
                key: "Tab",
                run: (view) => {
                    view.dispatch(view.state.replaceSelection("    "));
                    return true;
                }
            }
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
