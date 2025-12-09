import { EditorState, RangeSetBuilder } from '@codemirror/state';
import { Decoration } from '@codemirror/view';
import { type SyntaxNodeRef, type SyntaxNode } from '@lezer/common';
import { HorizontalRuleWidget, CheckboxWidget, ImageWidget, MathWidget, BulletWidget, OrderedListWidget, TableWidget } from './widgets';
import { syntaxTree } from '@codemirror/language';

// --- Helper Functions ---
function hasDescendant(node: SyntaxNode, name: string): boolean {
    if (node.name === name) return true;
    for (let child = node.firstChild; child; child = child.nextSibling) {
        if (hasDescendant(child, name)) return true;
    }
    return false;
}

// --- Interfaces ---

export interface DecoratorContext {
    state: EditorState;
    node: SyntaxNodeRef;
    builder: RangeSetBuilder<Decoration>;
    isCursorOverlapping: boolean; // For blocks, overlaps entire block. For inline, overlaps node.
    hasFocus: boolean; // Global focus state
    forcePreview?: boolean; // Force preview mode (ignoring cursor)
}

export type DecoratorProvider = (ctx: DecoratorContext) => void;

// --- Block Providers (for livePreviewState) ---

export const taskListClassProvider: DecoratorProvider = ({ state, node, builder }) => {
    if (node.name !== 'ListItem') return;

    // Check if this ListItem contains a TaskMarker
    if (hasDescendant(node.node, 'TaskMarker')) {
        // Add class to the line
        // ListItem usually starts at line start (including indentation)
        const line = state.doc.lineAt(node.from);
        builder.add(line.from, line.from, Decoration.line({ class: 'cm-task-list-item' }));
    }
};

export const blockMathProvider: DecoratorProvider = ({ state, node, builder, isCursorOverlapping }) => {
    if (node.name !== 'BlockMath') return;

    const text = state.sliceDoc(node.from, node.to);
    const startIdx = text.indexOf('$$');
    const endIdx = text.lastIndexOf('$$');
    
    let formula = text;
    if (startIdx !== -1 && endIdx !== -1 && startIdx < endIdx) {
        formula = text.slice(startIdx + 2, endIdx).trim();
    } else if (startIdx !== -1) {
        formula = text.slice(startIdx + 2).trim();
    }

    // Check if full line
    const startLine = state.doc.lineAt(node.from);
    const endLine = state.doc.lineAt(node.to);
    const isFullLine = (startLine.from === node.from) && (endLine.to === node.to);

    if (isCursorOverlapping) {
        builder.add(node.to, node.to, Decoration.widget({
            widget: new MathWidget(formula, true),
            side: 1, 
            block: isFullLine
        }));
    } else {
        builder.add(node.from, node.to, Decoration.replace({
            widget: new MathWidget(formula, true),
            block: isFullLine
        }));
    }
};

export const fencedCodeProvider: DecoratorProvider = ({ state, node, builder, isCursorOverlapping }) => {
    if (node.name !== 'FencedCode') return;

    const startLine = state.doc.lineAt(node.from);
    const endLine = state.doc.lineAt(node.to);
    
    // Calculate ranges to hide (if not editing)
    let openFenceRange = null;
    let closeFenceRange = null;

    if (!isCursorOverlapping) {
        // Opening fence range
        let openFenceEnd = node.from + 3; 
        let firstChild = node.node.firstChild;
        if (firstChild && firstChild.name === 'CodeMark') {
            let next = firstChild.nextSibling;
            if (next && next.name === 'CodeInfo') {
                openFenceEnd = next.to;
            } else {
                openFenceEnd = firstChild.to;
            }
        }
        openFenceRange = { from: node.from, to: openFenceEnd };

        // Closing fence range
        let lastChild = node.node.lastChild;
        if (lastChild && lastChild.name === 'CodeMark') {
            closeFenceRange = { from: lastChild.from, to: lastChild.to };
        } else {
            closeFenceRange = { from: node.to - 3, to: node.to };
        }
    }

    // Iterate lines and add decorations in STRICT order
    for (let i = startLine.number; i <= endLine.number; i++) {
        const line = state.doc.line(i);
        
        // 1. Add Line Decoration (Background) - always at line.from
        builder.add(line.from, line.from, Decoration.line({ class: 'cm-fenced-code-line' }));
        
        // 2. Add Replacement Decorations (Content)
        // Opening Fence Replacement
        if (openFenceRange && i === startLine.number) {
            builder.add(openFenceRange.from, openFenceRange.to, Decoration.replace({}));
        }
        
        // Closing Fence Replacement
        if (closeFenceRange && i === endLine.number) {
            builder.add(closeFenceRange.from, closeFenceRange.to, Decoration.replace({}));
        }
    }
};

export const blockquoteProvider: DecoratorProvider = ({ state, node, builder }) => {
    if (node.name !== 'Blockquote') return;

    // Hide QuoteMarks
    for (let cur = node.node.firstChild; cur; cur = cur.nextSibling) {
        if (cur.name === 'QuoteMark') {
            builder.add(cur.from, cur.to, Decoration.replace({}));
        }
    }
    // Line decoration
    const startLine = state.doc.lineAt(node.from);
    const endLine = state.doc.lineAt(node.to);
    for (let i = startLine.number; i <= endLine.number; i++) {
        const line = state.doc.line(i);
        builder.add(line.from, line.from, Decoration.line({ class: 'cm-blockquote-line' }));
    }
};

export const tableProvider: DecoratorProvider = ({ state, node, builder, isCursorOverlapping }) => {
    if (node.name !== 'Table') return;

    if (isCursorOverlapping) {
        // Edit Mode: Source view with monospace font
        const startLine = state.doc.lineAt(node.from);
        const endLine = state.doc.lineAt(node.to);
        for (let i = startLine.number; i <= endLine.number; i++) {
            const line = state.doc.line(i);
            builder.add(line.from, line.from, Decoration.line({ class: 'cm-table-edit-mode' }));
        }
    } else {
        // Preview Mode: Render as HTML Table Widget
        const text = state.sliceDoc(node.from, node.to);
        builder.add(node.from, node.to, Decoration.replace({
            widget: new TableWidget(text),
            block: true
        }));
    }
};

// --- Inline Providers (for livePreviewView) ---

export const inlineMathProvider: DecoratorProvider = ({ state, node, builder }) => {
    if (node.name !== 'InlineMath') return;
    const text = state.sliceDoc(node.from, node.to);
    const formula = text.slice(1, -1); 
    builder.add(node.from, node.to, Decoration.replace({
        widget: new MathWidget(formula, false)
    }));
};

export const displayMathProvider: DecoratorProvider = ({ state, node, builder }) => {
    if (node.name !== 'DisplayMath') return;
    const text = state.sliceDoc(node.from, node.to);
    const formula = text.slice(2, -2);
    builder.add(node.from, node.to, Decoration.replace({
        widget: new MathWidget(formula, true)
    }));
};

export const hiddenMarkersProvider: DecoratorProvider = ({ node, builder }) => {
    // QuoteMark, CodeMark, CodeInfo are handled by Block Providers or View logic?
    // Wait, in previous logic:
    // QuoteMark was in ViewPlugin.
    // CodeMark was handled by StateField (via FencedCode replacement).
    // CodeInfo was handled by StateField.
    
    // However, QuoteMark inside Blockquote needs to be hidden.
    // Blockquote provider (StateField) iterates children and hides them?
    // Yes, see blockquoteProvider above.
    
    // So we only need to handle standalone markers if any?
    // Actually, inline providers shouldn't double-hide things handled by block providers.
    
    // QuoteMark in ViewPlugin?
    // My blockquoteProvider iterates children and adds replace decorations.
    // Does StateField iteration enter children? Yes.
    // So blockquoteProvider is enough for QuoteMark.
};

export const horizontalRuleProvider: DecoratorProvider = ({ node, builder }) => {
    if (node.name === 'HorizontalRule') {
        builder.add(node.from, node.to, Decoration.replace({
            widget: new HorizontalRuleWidget()
        }));
    }
};

export const taskListProvider: DecoratorProvider = ({ state, node, builder, isCursorOverlapping, hasFocus }) => {
    if (node.name !== 'TaskMarker') return;
    
    // 1. If cursor is directly on the TaskMarker, show source
    if (isCursorOverlapping) return;

    // 2. Refined Source Reveal: Only show source if cursor is in the "Hotspot"
    // Hotspot = from ListItem start to TaskMarker end.
    // This allows cursor in the text content (e.g. "abc") to still see the widget.
    
    let ancestor = node.node.parent;
    while (ancestor && ancestor.name !== 'ListItem') {
        ancestor = ancestor.parent;
    }

    if (ancestor && ancestor.name === 'ListItem') {
        const selection = state.selection.main;
        // Sensitive area: [ListItem.from, TaskMarker.to]
        // Note: node.to is the end of TaskMarker
        if (hasFocus && selection.from >= ancestor.from && selection.to <= node.to) {
             return; 
        }
    }

    const text = state.sliceDoc(node.from, node.to);
    const isChecked = text.toLowerCase().includes('[x]');
    const prevNode = syntaxTree(state).resolve(node.from - 1, -1);
    let startReplace = node.from;
    
    // If preceded by a ListMark (e.g. "- [ ]"), we want to replace that too visually?
    // Actually, with the CSS hide strategy, we don't strictly need to replace the ListMark here
    // if we are hiding the BulletWidget.
    // BUT, the text "-" itself is hidden by BulletWidget's replacement decoration.
    // If we hide BulletWidget, we might see the raw "-".
    // Wait, BulletWidget is a REPLACEMENT decoration. If we display:none the widget, the replacement is still active (so raw text is hidden), but the widget is invisible.
    // Perfect.
    
    // However, existing logic tried to extend replacement range.
    if (prevNode && prevNode.name === 'ListMark') {
        startReplace = prevNode.from;
    }
    
    // Add Checkbox Widget
    builder.add(startReplace, node.to, Decoration.replace({
        widget: new CheckboxWidget(isChecked)
    }));
    
    // Line Decoration logic moved to taskListClassProvider in blockProviders
    // to prevent "Ranges must be added sorted" error
};

export const listProvider: DecoratorProvider = ({ state, node, builder, isCursorOverlapping, hasFocus }) => {
    if (node.name !== 'ListMark') return;

    // 1. If cursor is overlapping the ListMark itself, don't render widget
    if (isCursorOverlapping) return;

    // 2. Refined Source Reveal: Only show source if cursor is in the "Hotspot"
    const parent = node.node.parent;
    if (parent && parent.name === 'ListItem') {
        const selection = state.selection.main;
        
        // Find TaskMarker end to define hotspot
        let hotspotEnd = node.to; // Default to ListMark end
        
        // Try to find a TaskMarker sibling (or nephew via Task)
        // Since we can't easily traverse down from here without re-querying state tree or iterating siblings
        // Let's iterate next siblings of node.node
        let curr = node.node.nextSibling;
        while (curr) {
            if (curr.name === 'TaskMarker') {
                hotspotEnd = curr.to;
                break;
            }
            if (curr.name === 'Task') {
                // Task contains TaskMarker
                // We can assume Task end is TaskMarker end for hotspot purposes (or close enough)
                hotspotEnd = curr.to;
                break;
            }
            // If we hit Paragraph or Blockquote, we went too far
            if (curr.name === 'Paragraph' || curr.name === 'Blockquote') break;
            
            curr = curr.nextSibling;
        }

        if (hasFocus && selection.from >= parent.from && selection.to <= hotspotEnd) {
             return; 
        }
    }

    // Unconditionally render bullet. We rely on CSS (.cm-task-list-item .cm-bullet-widget { display: none }) to hide it for tasks.
    const text = state.sliceDoc(node.from, node.to);
    const isUnordered = /^[-*+]/.test(text);
    if (isUnordered) {
        builder.add(node.from, node.to, Decoration.replace({
            widget: new BulletWidget()
        }));
    } else {
        const numberPart = text.trim();
        builder.add(node.from, node.to, Decoration.replace({
            widget: new OrderedListWidget(numberPart)
        }));
    }
};

export const imageProvider: DecoratorProvider = ({ state, node, builder }) => {
    if (node.name !== 'Image') return;
    const text = state.sliceDoc(node.from, node.to);
    const match = text.match(/^!\[(.*?)\]\((.*?)\)$/);
    if (match) {
        builder.add(node.from, node.to, Decoration.replace({
            widget: new ImageWidget(match[2], match[1])
        }));
    }
};

export const styleProvider: DecoratorProvider = ({ node, builder }) => {
    if (node.name === 'StrongEmphasis') {
        builder.add(node.from, node.from + 2, Decoration.replace({}));
        builder.add(node.to - 2, node.to, Decoration.replace({}));
    } else if (node.name === 'Emphasis') {
        builder.add(node.from, node.from + 1, Decoration.replace({}));
        builder.add(node.to - 1, node.to, Decoration.replace({}));
    } else if (node.name.startsWith('ATXHeading')) {
        // Not checking text content for speed, assuming standard format
        // We need to know where the hash ends.
        // Can't know without text.
        // The node covers the whole heading.
        // We need to hide just the hashes.
        // This requires reading text.
        // We can leave this to be handled by caller passing text?
        // Or just read it here?
        // Let's skip implementation here and let caller handle or optimize.
        // Actually, caller (iterate) has access to text?
        // We passed state.
    } else if (node.name === 'InlineCode') {
        builder.add(node.from, node.from + 1, Decoration.replace({}));
        builder.add(node.to - 1, node.to, Decoration.replace({}));
    }
};

// Special handling for Heading which needs text analysis
export const headingProvider: DecoratorProvider = ({ state, node, builder }) => {
    if (!node.name.startsWith('ATXHeading')) return;
    const text = state.sliceDoc(node.from, node.to);
    const hashEnd = text.indexOf(' ') + 1;
    if (hashEnd > 0) {
        builder.add(node.from, node.from + hashEnd, Decoration.replace({}));
    }
};

export const blockProviders = [
    taskListClassProvider, // Added new provider for task list classes
    blockMathProvider,
    fencedCodeProvider,
    blockquoteProvider,
    tableProvider
];

export const inlineProviders = [
    inlineMathProvider,
    displayMathProvider,
    horizontalRuleProvider,
    taskListProvider,
    listProvider,
    imageProvider,
    styleProvider,
    headingProvider
];
