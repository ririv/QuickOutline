import { ViewPlugin, Decoration, type DecorationSet, ViewUpdate, EditorView } from '@codemirror/view';
import { RangeSetBuilder } from '@codemirror/state';
import { syntaxTree } from '@codemirror/language';
import { tags } from '@lezer/highlight';
import { HorizontalRuleWidget, CheckboxWidget, ImageWidget, MathWidget } from './widgets';

// --- Custom Lezer Extension for Math ---
export const MathExtension = {
    defineNodes: [
        { name: "InlineMath", style: tags.special(tags.content) },
        { name: "BlockMath", style: tags.special(tags.content) }
    ],
    parseInline: [{
        name: "InlineMath",
        parse(cx: any, next: number, pos: number) {
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

// --- Typora-like Live Preview Logic (Hiding Markers) ---
export const livePreview = ViewPlugin.fromClass(class {
    decorations: DecorationSet;

    constructor(view: EditorView) {
        this.decorations = this.buildDecorations(view);
    }

    update(update: ViewUpdate) {
        if (update.docChanged || update.viewportChanged || update.selectionSet || update.focusChanged) {
            this.decorations = this.buildDecorations(update.view);
        }
    }

    buildDecorations(view: EditorView) {
        const builder = new RangeSetBuilder<Decoration>();
        const { state } = view;
        const selection = state.selection.main;
        const hasFocus = view.hasFocus;

        for (const { from, to } of view.visibleRanges) {
            syntaxTree(state).iterate({
                from, to,
                enter: (node) => {
                    const nodeFrom = node.from;
                    const nodeTo = node.to;
                    const text = state.sliceDoc(nodeFrom, nodeTo);
                    
                    let isCursorOverlapping = false;
                    if (hasFocus) {
                            isCursorOverlapping = (selection.from >= nodeFrom && selection.from <= nodeTo) || 
                                                (selection.to >= nodeFrom && selection.to <= nodeTo) ||
                                                (selection.from <= nodeFrom && selection.to >= nodeTo);
                    }

                    if (isCursorOverlapping) {
                        return;
                    }

                    // --- Blockquotes (>) ---
                    if (node.name === 'Blockquote') {
                        // Find the QuoteMark node within the Blockquote
                        for (let cur = node.node.firstChild; cur; cur = cur.nextSibling) {
                            if (cur.name === 'QuoteMark') {
                                builder.add(cur.from, cur.to, Decoration.replace({}));
                            }
                        }
                        // Add line decoration for the entire blockquote lines
                        // Correct iteration over lines
                        const startLine = state.doc.lineAt(nodeFrom);
                        const endLine = state.doc.lineAt(nodeTo);
                        for (let i = startLine.number; i <= endLine.number; i++) {
                            const line = state.doc.line(i);
                            builder.add(line.from, line.from, Decoration.line({ class: 'cm-blockquote-line' }));
                        }
                        return; // Prevent processing children as they're part of blockquote
                    }
                    
                    // --- Fenced Code Blocks (```) ---
                    if (node.name === 'FencedCode') {
                        // Hide fences
                        builder.add(nodeFrom, nodeFrom + 3, Decoration.replace({})); // Hide opening fence ```
                        builder.add(nodeTo - 3, nodeTo, Decoration.replace({}));     // Hide closing fence ```
                        // Add line decoration for the code block lines
                        const startLine = state.doc.lineAt(nodeFrom);
                        const endLine = state.doc.lineAt(nodeTo);
                        for (let i = startLine.number; i <= endLine.number; i++) {
                            const line = state.doc.line(i);
                            builder.add(line.from, line.from, Decoration.line({ class: 'cm-fenced-code-line' }));
                        }
                        return; // Prevent processing children
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

                    if (node.name === 'InlineMath') {
                            const formula = text.slice(1, -1); // Strip $
                            builder.add(nodeFrom, nodeTo, Decoration.replace({
                                widget: new MathWidget(formula, false)
                            }));
                    } else if (node.name === 'BlockMath') {
                            const formula = text.slice(2, -2); // Strip $$
                            builder.add(nodeFrom, nodeTo, Decoration.replace({
                                widget: new MathWidget(formula, true)
                            }));
                    }

                    // --- Bold (**text**) ---
                    if (node.name === 'StrongEmphasis') {
                        builder.add(nodeFrom, nodeFrom + 2, Decoration.replace({}));
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
