import { Decoration, type DecorationSet, EditorView, showTooltip, type Tooltip, ViewPlugin, ViewUpdate } from '@codemirror/view';
import { RangeSetBuilder, type EditorState, StateField, StateEffect } from '@codemirror/state';
import { syntaxTree } from '@codemirror/language';
import { tags } from '@lezer/highlight';
import katex from 'katex';
import { MathExtension } from './math-extension';
import { blockProviders, inlineProviders, type DecoratorContext } from './decorators';

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

// --- Preview Mode State Field ---
export const setPreviewMode = StateEffect.define<boolean>();
export const previewModeState = StateField.define<boolean>({
    create() { return false; },
    update(val, tr) {
        for (let e of tr.effects) {
            if (e.is(setPreviewMode)) return e.value;
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
        if (tr.docChanged || tr.selection || tr.effects.some(e => e.is(setFocusState) || e.is(setPreviewMode))) {
            return buildBlockDecorations(tr.state);
        }
        return decorations;
    },
    provide: f => EditorView.decorations.from(f)
});

function buildBlockDecorations(state: EditorState) {
    const builder = new RangeSetBuilder<Decoration>();
    const selection = state.selection.main;
    // state.field(field, false) returns T | undefined. Ensure boolean.
    const hasFocus = state.field(focusState, false) || false;
    const forcePreview = state.field(previewModeState, false) || false;
    const tree = syntaxTree(state);

    tree.iterate({
        enter: (node) => {
            const nodeFrom = node.from;
            const nodeTo = node.to;
            
            let isCursorOverlapping = false;
            // If in preview mode, we NEVER consider the cursor overlapping (always render widgets)
            if (!forcePreview && hasFocus) {
                isCursorOverlapping = (selection.from >= nodeFrom && selection.from <= nodeTo) || 
                                      (selection.to >= nodeFrom && selection.to <= nodeTo) ||
                                      (selection.from <= nodeFrom && selection.to >= nodeTo);
            }

            const ctx: DecoratorContext = {
                state,
                node,
                builder,
                isCursorOverlapping,
                hasFocus,
                forcePreview
            };

            for (const provider of blockProviders) {
                provider(ctx);
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
        const forcePreview = state.field(previewModeState, false) || false;

        for (const { from, to } of view.visibleRanges) {
            syntaxTree(state).iterate({
                from, to,
                enter: (node) => {
                    const nodeFrom = node.from;
                    const nodeTo = node.to;
                    
                    let isCursorOverlapping = false;
                    if (!forcePreview && hasFocus) {
                        isCursorOverlapping = (selection.from >= nodeFrom && selection.from <= nodeTo) || 
                                              (selection.to >= nodeFrom && selection.to <= nodeTo) ||
                                              (selection.from <= nodeFrom && selection.to >= nodeTo);
                    }

                    if (isCursorOverlapping) {
                        return;
                    }
                    
                    const ctx: DecoratorContext = {
                        state,
                        node,
                        builder,
                        isCursorOverlapping,
                        hasFocus,
                        forcePreview
                    };

                    for (const provider of inlineProviders) {
                        provider(ctx);
                    }
                }
            });
        }
        return builder.finish();
    }
}, {
    decorations: v => v.decorations
});