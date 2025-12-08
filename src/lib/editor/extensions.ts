import { Decoration, type DecorationSet, EditorView, showTooltip, type Tooltip, ViewPlugin, ViewUpdate } from '@codemirror/view';
import { RangeSetBuilder, type EditorState, StateField, StateEffect } from '@codemirror/state';
import { syntaxTree } from '@codemirror/language';
import { tags } from '@lezer/highlight';
import { HorizontalRuleWidget, CheckboxWidget, ImageWidget, MathWidget, BulletWidget, OrderedListWidget } from './widgets';
import katex from 'katex';
import { MathExtension } from './math-extension';

export { MathExtension }; 

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

// --- Focus State Field ---
export const setFocusState = StateEffect.define<boolean>();
export const focusState = StateField.define<boolean>({
    create() { return false; },
    update(val, tr) {
        for (let e of tr.effects) {
            if (e.is(setFocusState)) return e.value;
        }
        return val;
    }
});

// 1. StateField for Block Replacements
export const livePreviewState = StateField.define<DecorationSet>({
    create(state) {
        return buildBlockDecorations(state);
    },
    update(decorations, tr) {
        if (tr.docChanged || tr.selection || tr.effects.some(e => e.is(setFocusState))) {
            return buildBlockDecorations(tr.state);
        }
        return decorations;
    },
    provide: f => EditorView.decorations.from(f)
});

function buildBlockDecorations(state: EditorState) {
    const builder = new RangeSetBuilder<Decoration>();
    const selection = state.selection.main;
    const hasFocus = state.field(focusState, false);
    const tree = syntaxTree(state);

    tree.iterate({
        enter: (node) => {
            const nodeFrom = node.from;
            const nodeTo = node.to;
            
            let isCursorOverlapping = false;
            if (hasFocus) {
                isCursorOverlapping = (selection.from >= nodeFrom && selection.from <= nodeTo) || 
                                      (selection.to >= nodeFrom && selection.to <= nodeTo) ||
                                      (selection.from <= nodeFrom && selection.to >= nodeTo);
            }

            if (node.name === 'BlockMath') {
                const text = state.sliceDoc(nodeFrom, nodeTo);
                
                // Robustly extract formula by finding the delimiting $$
                // Find start index of first $$
                const startIdx = text.indexOf('$$');
                // Find start index of last $$
                const endIdx = text.lastIndexOf('$$');
                
                let formula = text;
                if (startIdx !== -1 && endIdx !== -1 && startIdx < endIdx) {
                    // Slice strictly between the first $$ and the last $$
                    // startIdx + 2 skips the opening $$
                    formula = text.slice(startIdx + 2, endIdx).trim();
                } else if (startIdx !== -1) {
                    // Only found start $$, maybe unclosed or being typed
                    formula = text.slice(startIdx + 2).trim();
                }

                if (isCursorOverlapping) {
                    builder.add(nodeTo, nodeTo, Decoration.widget({
                        widget: new MathWidget(formula, true),
                        side: 1, 
                        block: true 
                    }));
                } else {
                    builder.add(nodeFrom, nodeTo, Decoration.replace({
                        widget: new MathWidget(formula, true),
                        block: true
                    }));
                }
                return;
            }

            if (node.name === 'Blockquote') {
                const startLine = state.doc.lineAt(nodeFrom);
                const endLine = state.doc.lineAt(nodeTo);
                for (let i = startLine.number; i <= endLine.number; i++) {
                    const line = state.doc.line(i);
                    builder.add(line.from, line.from, Decoration.line({ class: 'cm-blockquote-line' }));
                }
                return; 
            }
            
            if (node.name === 'FencedCode') {
                const startLine = state.doc.lineAt(nodeFrom);
                const endLine = state.doc.lineAt(nodeTo);
                for (let i = startLine.number; i <= endLine.number; i++) {
                    const line = state.doc.line(i);
                    builder.add(line.from, line.from, Decoration.line({ class: 'cm-fenced-code-line' }));
                }
                return;
            }
        }
    });
    return builder.finish();
}

// 2. ViewPlugin for Inline Replacements
export const livePreviewView = ViewPlugin.fromClass(class {
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
                    
                    let isCursorOverlapping = false;
                    if (hasFocus) {
                        isCursorOverlapping = (selection.from >= nodeFrom && selection.from <= nodeTo) || 
                                              (selection.to >= nodeFrom && selection.to <= nodeTo) ||
                                              (selection.from <= nodeFrom && selection.to >= nodeTo);
                    }

                    if (isCursorOverlapping) {
                        return;
                    }
                    
                    const text = state.sliceDoc(nodeFrom, nodeTo);

                    if (node.name === 'InlineMath') {
                        const formula = text.slice(1, -1); 
                        builder.add(nodeFrom, nodeTo, Decoration.replace({
                            widget: new MathWidget(formula, false)
                        }));
                        return;
                    }

                    // --- DisplayMath ($$...$$ inline) ---
                    if (node.name === 'DisplayMath') {
                        const formula = text.slice(2, -2); // Strip $$
                        builder.add(nodeFrom, nodeTo, Decoration.replace({
                            widget: new MathWidget(formula, true) // displayMode: true
                        }));
                        return;
                    }

                    if (node.name === 'QuoteMark') {
                        builder.add(nodeFrom, nodeTo, Decoration.replace({}));
                        return;
                    }

                    if (node.name === 'CodeMark') {
                        builder.add(nodeFrom, nodeTo, Decoration.replace({}));
                        return;
                    }

                    if (node.name === 'HorizontalRule') {
                        builder.add(nodeFrom, nodeTo, Decoration.replace({
                            widget: new HorizontalRuleWidget()
                        }));
                        return;
                    }

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

                    if (node.name === 'ListMark') {
                        const isUnordered = /^[-*+]/.test(text);
                        if (isUnordered) {
                            builder.add(nodeFrom, nodeTo, Decoration.replace({
                                widget: new BulletWidget()
                            }));
                        } else {
                            const numberPart = text.trim();
                            builder.add(nodeFrom, nodeTo, Decoration.replace({
                                widget: new OrderedListWidget(numberPart)
                            }));
                        }
                        return;
                    }

                    if (node.name === 'Image') {
                        const match = text.match(/^!\[(.*?)\]\((.*?)\)$/);
                        if (match) {
                            builder.add(nodeFrom, nodeTo, Decoration.replace({
                                widget: new ImageWidget(match[2], match[1])
                            }));
                        }
                        return;
                    }

                    if (node.name === 'StrongEmphasis') {
                        builder.add(nodeFrom, nodeFrom + 2, Decoration.replace({}));
                        builder.add(nodeTo - 2, nodeTo, Decoration.replace({}));
                    }
                    
                    else if (node.name === 'Emphasis') {
                        builder.add(nodeFrom, nodeFrom + 1, Decoration.replace({}));
                        builder.add(nodeTo - 1, nodeTo, Decoration.replace({}));
                    }

                    else if (node.name.startsWith('ATXHeading')) {
                        const hashEnd = text.indexOf(' ') + 1;
                        if (hashEnd > 0) {
                            builder.add(nodeFrom, nodeFrom + hashEnd, Decoration.replace({}));
                        }
                    }
                    
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
