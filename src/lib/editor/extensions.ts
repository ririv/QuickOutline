import { ViewPlugin, Decoration, type DecorationSet, ViewUpdate, EditorView, showTooltip, type Tooltip } from '@codemirror/view';
import { RangeSetBuilder, type EditorState } from '@codemirror/state';
import { syntaxTree } from '@codemirror/language';
import { tags } from '@lezer/highlight';
import { HorizontalRuleWidget, CheckboxWidget, ImageWidget, MathWidget, BulletWidget, OrderedListWidget } from './widgets';
import katex from 'katex';

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

// --- Math Tooltip (Cursor based) ---
export function mathTooltip(state: EditorState): Tooltip | null {
    const { main } = state.selection;
    if (!main.empty) return null;
    
    const pos = main.head;
    const node = syntaxTree(state).resolveInner(pos, -1);
    
    if (node.name === 'InlineMath') {
        const from = node.from;
        const to = node.to;
        const text = state.sliceDoc(from, to);
        const formula = text.slice(1, -1); // Strip $
        
        return {
            pos: from,
            above: true,
            strictSide: true,
            create: () => {
                const dom = document.createElement("div");
                dom.className = "cm-math-tooltip";
                dom.style.padding = "4px 8px";
                dom.style.backgroundColor = "#fff";
                dom.style.border = "1px solid #ddd";
                dom.style.borderRadius = "4px";
                dom.style.boxShadow = "0 2px 8px rgba(0,0,0,0.15)";
                dom.style.zIndex = "100";
                
                try {
                    katex.render(formula, dom, {
                        throwOnError: false,
                        displayMode: false
                    });
                } catch (e) {
                    dom.textContent = "Invalid Formula";
                    dom.style.color = "red";
                }
                return { dom };
            }
        };
    }
    return null;
}

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

                    // --- List Markers (-, *, + or 1.) ---
                    // Use Widgets to replace markers so they respect indentation naturally
                    if (node.name === 'ListMark') {
                        // Unordered: -, *, +
                        // Check if starts with -, *, or + (without requiring a trailing space)
                        const isUnordered = /^[-*+]/.test(text); 
                        
                        if (isUnordered) {
                             builder.add(nodeFrom, nodeTo, Decoration.replace({
                                 widget: new BulletWidget()
                             }));
                        } else {
                             // Ordered list: 1.
                             // Extract the number part (e.g. "1.")
                             const numberPart = text.trim();
                             builder.add(nodeFrom, nodeTo, Decoration.replace({
                                 widget: new OrderedListWidget(numberPart)
                             }));
                        }
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
                            
                            console.log(`[BlockMath Debug] Node: from=${nodeFrom}, to=${nodeTo}, formula='${formula}'`);
                            console.log(`[BlockMath Debug] isCursorOverlapping: ${isCursorOverlapping}`);

                            // If cursor overlaps (editing), show BOTH source and preview
                            if (isCursorOverlapping) {
                                // Add preview widget at the end of the block
                                builder.add(nodeTo, nodeTo, Decoration.widget({
                                    widget: new MathWidget(formula, true),
                                    side: 1, // Render after the content
                                    block: true // Ensure it renders as a block element
                                }));
                                console.log(`[BlockMath Debug] Added preview widget at nodeTo=${nodeTo}`);
                            } else {
                                // Not editing: Replace source with preview (standard behavior)
                                builder.add(nodeFrom, nodeTo, Decoration.replace({
                                    widget: new MathWidget(formula, true)
                                }));
                                console.log(`[BlockMath Debug] Replaced source with preview from=${nodeFrom}, to=${nodeTo}`);
                            }
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
