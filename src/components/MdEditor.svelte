<script lang="ts">
    import { onMount, onDestroy } from 'svelte';
    import { EditorState, RangeSetBuilder, type Extension } from '@codemirror/state';
    import { EditorView, Decoration, type DecorationSet, ViewPlugin, ViewUpdate, WidgetType, keymap, placeholder } from '@codemirror/view';
    import { markdown, markdownLanguage } from '@codemirror/lang-markdown';
    import { syntaxTree, syntaxHighlighting, HighlightStyle } from '@codemirror/language';
    import { tags } from '@lezer/highlight';
    import { defaultKeymap, history, historyKeymap } from '@codemirror/commands';
    import { searchKeymap } from '@codemirror/search';
    import { languages } from '@codemirror/language-data';
    import { GFM } from '@lezer/markdown'; // Import GFM
    import katex from 'katex';
    import 'katex/dist/katex.min.css';

    // --- Custom Lezer Extension for Math ---
    const MathExtension = {
        defineNodes: [
            { name: "InlineMath", style: tags.special(tags.content) },
            { name: "BlockMath", style: tags.special(tags.content) }
        ],
        parseInline: [{
            name: "InlineMath",
            parse(cx, next, pos) {
                if (next != 36) return -1; // '$'
                
                // BlockMath: $$...$$
                if (cx.char(pos + 1) == 36) {
                    let end = pos + 2;
                    while (end < cx.end) {
                        if (cx.char(end) == 36 && cx.char(end + 1) == 36) {
                            cx.addElement(cx.elt("BlockMath", pos, end + 2));
                            return end + 2;
                        }
                        end++;
                    }
                    return -1;
                }

                // InlineMath: $...$
                let end = pos + 1;
                while (end < cx.end) {
                    if (cx.char(end) == 36) {
                        cx.addElement(cx.elt("InlineMath", pos, end + 1));
                        return end + 1;
                    }
                    end++;
                }
                return -1;
            }
        }]
    };

    let editorElement: HTMLDivElement;
    let view: EditorView;

    // --- Widgets ---
    class HorizontalRuleWidget extends WidgetType {
        toDOM() {
            const hr = document.createElement("hr");
            hr.className = "cm-hr-widget";
            hr.style.border = "none";
            hr.style.borderTop = "2px solid #eee"; // Thicker line
            hr.style.margin = "1em 0";
            return hr;
        }
    }

    class CheckboxWidget extends WidgetType {
        checked: boolean;
        
        constructor(checked: boolean) {
            super();
            this.checked = checked;
        }
        
        eq(other: CheckboxWidget) { return other.checked === this.checked; }
        
        toDOM(view: EditorView) {
            const wrap = document.createElement("span");
            wrap.className = "cm-checkbox-widget";
            wrap.style.paddingRight = "0.5em";
            wrap.style.cursor = "pointer";
            
            const input = document.createElement("input");
            input.type = "checkbox";
            input.checked = this.checked;
            input.style.cursor = "pointer";
            
            // Prevent CodeMirror from handling the click so the native checkbox works visually (and we handle logic)
            // But actually we want to update the doc.
            input.onclick = (e) => {
                e.preventDefault(); 
                const pos = view.posAtDOM(wrap);
                if (pos == null) return;

                // We need to find the specific [ ] or [x] text relative to this position.
                // The widget replaced a range starting at 'pos'.
                // However, since we now replace the whole "- [ ]", pos is the start of the list item (the dash).
                
                const line = view.state.doc.lineAt(pos);
                // We want to find the first '[' after the position of the widget
                // pos is global index.
                const relativePos = pos - line.from;
                const lineText = line.text;
                
                // Find '[' starting from the widget position
                const openBracketIndex = lineText.indexOf('[', relativePos);
                if (openBracketIndex === -1) return;
                
                // The character to toggle is right after '['
                const toggleCharPos = line.from + openBracketIndex + 1;
                
                // Check if current is space or x (case insensitive)
                // Just toggle based on widget state which reflects model state at render time.
                const charToSet = this.checked ? " " : "x"; 
                
                view.dispatch({
                    changes: { from: toggleCharPos, to: toggleCharPos + 1, insert: charToSet }
                });
            };
            
            wrap.appendChild(input);
            return wrap;
        }
    }

    class ImageWidget extends WidgetType {
        url: string;
        alt: string;
        
        constructor(url: string, alt: string) { 
            super(); 
            this.url = url;
            this.alt = alt;
        }

        eq(other: ImageWidget) { return other.url === this.url && other.alt === this.alt; }

        toDOM() {
            const img = document.createElement("img");
            img.src = this.url;
            img.alt = this.alt;
            img.className = "cm-image-preview";
            img.style.maxWidth = "100%";
            img.style.maxHeight = "400px";
            img.style.display = "block";
            img.style.margin = "0.5em auto";
            img.style.borderRadius = "4px";
            return img;
        }
    }

    class MathWidget extends WidgetType {
        formula: string;
        displayMode: boolean;

        constructor(formula: string, displayMode: boolean) { 
            super(); 
            this.formula = formula;
            this.displayMode = displayMode;
        }

        eq(other: MathWidget) { return other.formula === this.formula && other.displayMode === this.displayMode; }

        toDOM() {
            const span = document.createElement("span");
            span.className = this.displayMode ? "cm-math-block" : "cm-math-inline";
            if (this.displayMode) {
                 span.style.display = "block";
                 span.style.textAlign = "center";
                 span.style.margin = "1em 0";
            }
            try {
                katex.render(this.formula, span, {
                    displayMode: this.displayMode,
                    throwOnError: false
                });
            } catch (e) {
                span.textContent = this.formula;
                span.style.color = "red";
            }
            return span;
        }
    }

    // --- 1. Rich Text Styling via HighlightStyle ---
    // This applies styles to the underlying syntax tokens
    const myHighlightStyle = HighlightStyle.define([
        { tag: tags.heading1, fontSize: "2em", fontWeight: "bold", borderBottom: "1px solid #eee", display: "inline-block", width: "100%", paddingBottom: "0.3em", marginBottom: "0.5em" },
        { tag: tags.heading2, fontSize: "1.5em", fontWeight: "bold", borderBottom: "1px solid #eee", display: "inline-block", width: "100%", paddingBottom: "0.3em" },
        { tag: tags.heading3, fontSize: "1.25em", fontWeight: "bold" },
        { tag: tags.heading, fontWeight: "bold" },
        { tag: tags.strong, fontWeight: "bold" },
        { tag: tags.emphasis, fontStyle: "italic" },
        { tag: tags.monospace, backgroundColor: "rgba(27, 31, 35, 0.05)", borderRadius: "3px", padding: "0.2em 0.4em", fontFamily: "monospace" },
        { tag: tags.link, color: "#0366d6", textDecoration: "underline" },
        { tag: tags.list, paddingLeft: "1em" },
        { tag: tags.quote, borderLeft: "4px solid #dfe2e5", paddingLeft: "1em", color: "#6a737d", fontStyle: "italic" }, // Blockquote
        // Note: FencedCode styling is complex because it contains other tags. 
        // We can style the background of lines containing it via line decorations, but HighlightStyle acts on tokens.
        // We'll rely on CM6 generic styling for code blocks or add a line decorator later if needed.
    ]);

    // --- 2. Typora-like Live Preview Logic (Hiding Markers) ---
    const styleDecorations = ViewPlugin.fromClass(class {
        decorations: DecorationSet;

        constructor(view: EditorView) {
            this.decorations = this.buildDecorations(view);
        }

        update(update: ViewUpdate) {
            // Update decorations if doc changed, viewport changed, selection changed, 
            // OR if focus state changed (we handle focus via event listeners below, but update.focusChanged is available too)
            if (update.docChanged || update.viewportChanged || update.selectionSet || update.focusChanged) {
                this.decorations = this.buildDecorations(update.view);
            }
        }

        buildDecorations(view: EditorView) {
            const builder = new RangeSetBuilder<Decoration>();
            const { state } = view;
            const selection = state.selection.main;
            const hasFocus = view.hasFocus; // Check focus state

            for (const { from, to } of view.visibleRanges) {
                syntaxTree(state).iterate({
                    from, to,
                    enter: (node) => {
                        const nodeFrom = node.from;
                        const nodeTo = node.to;
                        const text = state.sliceDoc(nodeFrom, nodeTo);
                        
                        // Check if cursor is overlapping the node
                        // CRITICAL CHANGE: Only expand if editor HAS FOCUS. 
                        // If lost focus, render everything (hide markers).
                        let isCursorOverlapping = false;
                        if (hasFocus) {
                             isCursorOverlapping = (selection.from >= nodeFrom && selection.from <= nodeTo) || 
                                                    (selection.to >= nodeFrom && selection.to <= nodeTo) ||
                                                    (selection.from <= nodeFrom && selection.to >= nodeTo);
                        }

                        if (isCursorOverlapping) {
                            return; // Don't hide anything if cursor is here AND focused
                        }

                        // --- Horizontal Rule (---) ---
                        if (node.name === 'HorizontalRule') {
                            builder.add(nodeFrom, nodeTo, Decoration.replace({
                                widget: new HorizontalRuleWidget()
                            }));
                            return;
                        }

                        // --- Task List ([ ] or [x]) ---
                        if (node.name === 'TaskMarker') {
                             const isChecked = text.toLowerCase().includes('[x]');
                             
                             // Try to find the preceding ListMark to hide it too (avoiding "- [ ]")
                             // TaskMarker starts at nodeFrom. We look at nodeFrom - 1.
                             // We expect a ListMark there (or space then ListMark).
                             // Let's resolve the node right before this one.
                             // side: -1 prefers nodes ending at or before pos.
                             const prevNode = syntaxTree(state).resolve(nodeFrom - 1, -1);
                             
                             let startReplace = nodeFrom;
                             if (prevNode && prevNode.name === 'ListMark') {
                                 startReplace = prevNode.from;
                             }
                             
                             builder.add(startReplace, nodeTo, Decoration.replace({
                                 widget: new CheckboxWidget(isChecked)
                             }));
                             return;
                        }

                        // --- Images (![alt](url)) ---
                        if (node.name === 'Image') {
                            const match = text.match(/^!\[(.*?)\]\((.*?)\)$/);
                            if (match) {
                                const alt = match[1];
                                const url = match[2];
                                builder.add(nodeFrom, nodeTo, Decoration.replace({
                                    widget: new ImageWidget(url, alt)
                                }));
                                return; 
                            }
                        }

                        // --- Math ($...$ or $...$) ---
                        // Note: Standard CM6 markdown parser doesn't detect $ by default without extension.
                        // We will rely on manual detection if node.name is generic, OR assume we enable the extension.
                        // For now, let's try manual detection within generic text if parser doesn't catch it, 
                        // BUT better to just handle 'InlineMath' and 'BlockMath' if we can enable them.
                        // Let's assume we implement regex detection here for robustness if extension is missing.
                        
                        // Fallback Regex for Math (simplistic)
                        // Inline: $...$ (no space after first $)
                        // Block: $...$
                        // Since we are iterating nodes, we might be inside a Paragraph. 
                        // Actually, without the Math extension, $ is just text.
                        // We need to enable the Math extension in the parser config below.
                        
                        if (node.name === 'InlineMath') {
                             const formula = text.slice(1, -1); // Strip $
                             builder.add(nodeFrom, nodeTo, Decoration.replace({
                                 widget: new MathWidget(formula, false)
                             }));
                        } else if (node.name === 'BlockMath') {
                             const formula = text.slice(2, -2); // Strip $
                             builder.add(nodeFrom, nodeTo, Decoration.replace({
                                 widget: new MathWidget(formula, true)
                             }));
                        }

                        // --- Bold (**text**) ---
                        if (node.name === 'StrongEmphasis') {
                            // Hide leading **
                            builder.add(nodeFrom, nodeFrom + 2, Decoration.replace({}));
                            // Hide trailing **
                            builder.add(nodeTo - 2, nodeTo, Decoration.replace({}));
                        }
                        
                        // --- Italic (*text* or _text_) ---
                        else if (node.name === 'Emphasis') {
                            builder.add(nodeFrom, nodeFrom + 1, Decoration.replace({}));
                            builder.add(nodeTo - 1, nodeTo, Decoration.replace({}));
                        }

                        // --- Headings (# H1) ---
                        else if (node.name.startsWith('ATXHeading')) {
                            const hashEnd = text.indexOf(' ') + 1;
                            if (hashEnd > 0) {
                                builder.add(nodeFrom, nodeFrom + hashEnd, Decoration.replace({}));
                            }
                        }
                        
                        // --- Inline Code (`code`) ---
                         else if (node.name === 'InlineCode') {
                            builder.add(nodeFrom, nodeFrom + 1, Decoration.replace({}));
                            builder.add(nodeTo - 1, nodeTo, Decoration.replace({}));
                         }
                    }
                });
            }
            return builder.finish();
        }
    }, {
        decorations: v => v.decorations
    });


    // --- Editor Init ---

    export const init = (initialMarkdown: string = '') => {
        if (view) return;

        const startState = EditorState.create({
            doc: initialMarkdown,
            extensions: [
                // Basics
                history(),
                keymap.of([...defaultKeymap, ...historyKeymap, ...searchKeymap]),
                placeholder('开始输入...'),
                EditorView.lineWrapping, // Typora soft wrap

                // Markdown
                markdown({ 
                    base: markdownLanguage, 
                    codeLanguages: languages,
                    extensions: [GFM, MathExtension] // Enable GFM and Custom Math parsing
                }),
                
                // Rich Text Syntax Highlighting
                syntaxHighlighting(myHighlightStyle),
                
                // Custom Theme & Styling
                EditorView.theme({
                    "&": {
                        height: "100%",
                        fontSize: "16px",
                        fontFamily: "'Inter', sans-serif"
                    },
                    ".cm-scroller": { fontFamily: "inherit" },
                    ".cm-content": {
                        padding: "40px 60px", // Typora-like padding
                        maxWidth: "900px",
                        margin: "0 auto",
                    },
                    "&.cm-focused": { outline: "none" }
                }),
                
                // Activation
                styleDecorations,
                // Focus/Blur listeners to trigger decoration update
                EditorView.domEventHandlers({
                    focus: (event, view) => {
                        // Force update
                        view.dispatch({ effects: [] }); 
                    },
                    blur: (event, view) => {
                         view.dispatch({ effects: [] });
                    }
                })
            ]
        });

        view = new EditorView({
            state: startState,
            parent: editorElement
        });
    };

    // --- External API ---
    export const getValue = () => view?.state.doc.toString() || '';
    export const setValue = (val: string) => {
        if (!view) return;
        view.dispatch({
            changes: { from: 0, to: view.state.doc.length, insert: val }
        });
    };
    export const insertValue = (val: string) => {
         if (!view) return;
         const range = view.state.selection.main;
         view.dispatch({
             changes: { from: range.from, to: range.to, insert: val },
             selection: { anchor: range.from + val.length }
         });
    };
    // Placeholder for now
    export const insertImageMarkdown = (path: string) => insertValue(`\n![](${path})\n`);
    export const getContentHtml = async () => "Preview not implemented in CM6 demo yet";
    export const getPayloads = async () => JSON.stringify({ html: "", styles: "" });


    onMount(() => {
        init('# Hello CodeMirror 6\n\nTry typing **bold text** or *italic* here.\n\nMove cursor inside and outside the styled text to see the magic!');
        
        // Tab Handler
        const handleTab = (e: KeyboardEvent) => {
             if (e.key === 'Tab' && view) {
                 e.preventDefault();
                 view.dispatch(view.state.replaceSelection('    '));
             }
        };
        editorElement.addEventListener('keydown', handleTab);
        
        return () => {
             editorElement?.removeEventListener('keydown', handleTab);
        };
    });

    onDestroy(() => {
        view?.destroy();
    });

</script>

<div class="cm-container" bind:this={editorElement}></div>

<style>
    .cm-container {
        height: 100%;
        width: 100%;
        overflow: hidden; /* CM handles scrolling */
        background-color: white;
    }
</style>
