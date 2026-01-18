import { EditorView, keymap, lineNumbers, highlightActiveLine, highlightActiveLineGutter, ViewPlugin, Decoration, type ViewUpdate, WidgetType } from '@codemirror/view';
import { generateDotLeaderData } from '@/lib/templates/toc/toc-gen/toc-generator.tsx';
import { parseTocLine, scanMathInString } from '@/lib/templates/toc/toc-gen/parser';
import { validatePageTarget } from '@/lib/services/PageLinkResolver';
import { EditorState, RangeSetBuilder, Facet } from '@codemirror/state';
import katex from 'katex';

// --- Page Validation Plugin ---

export interface ValidationState {
    offset: number;
    totalPage: number;
    pageLabels: string[] | null;
    insertPos: number;
}

export const pageValidationConfig = Facet.define<ValidationState, ValidationState>({
    combine: values => values[0] || { offset: 0, totalPage: 0, pageLabels: null, insertPos: 0 }
});

const invalidPageDecoration = Decoration.mark({ class: "cm-invalid-page-target" });

const pageValidationTheme = EditorView.baseTheme({
    ".cm-invalid-page-target": {
        textDecoration: "underline wavy red 1px",
        textDecorationSkipInk: "none"
    }
});

// pageValidationPlugin removed as its logic is merged into tocPlugin

export const pageValidationExtension = [
    pageValidationTheme,
    // pageValidationPlugin removed
];

// --- TOC Decoration Plugin ---

class LeaderWidget extends WidgetType {
    constructor() {
        super();
    }

    toDOM() {
        const span = document.createElement("span");
        span.className = "toc-leader-widget toc-leader-only";
        const dots = document.createElement("span");
        dots.className = "toc-leader-dots";
        span.appendChild(dots);
        return span;
    }

    ignoreEvent() { return false; }
    eq(other: LeaderWidget) { return true; }
}

class PageWidget extends WidgetType {
    readonly displayPage: string;
    readonly isValid: boolean;

    constructor(displayPage: string, isValid: boolean) {
        super();
        this.displayPage = displayPage;
        this.isValid = isValid;
    }

    toDOM() {
        const span = document.createElement("span");
        span.className = "toc-page-widget";
        span.textContent = this.displayPage;
        
        if (!this.isValid) {
            span.classList.add("cm-invalid-page-target");
        }
        
        return span;
    }

    ignoreEvent() { return false; }

    eq(other: PageWidget) { 
        return other.displayPage === this.displayPage && other.isValid === this.isValid; 
    }
}

class MathWidget extends WidgetType {
    readonly formula: string;

    constructor(formula: string) {
        super();
        this.formula = formula;
    }

    toDOM() {
        const span = document.createElement("span");
        span.className = "toc-math-widget";
        span.style.display = "inline-block";
        try {
            katex.render(this.formula, span, { 
                throwOnError: false, 
                displayMode: false 
            });
        } catch (e) {
            span.textContent = this.formula;
        }
        return span;
    }

    ignoreEvent() { return false; }

    eq(other: MathWidget) { return other.formula == this.formula; }
}

class EscapeWidget extends WidgetType {
    toDOM() {
        const span = document.createElement("span");
        span.textContent = "$";
        span.className = "toc-escape-widget";
        return span;
    }
    eq(other: EscapeWidget) { return true; }
    ignoreEvent() { return false; }
}

export const lineTheme = EditorView.theme({
    ".cm-line": {
        borderRadius: "4px",
        transition: "background-color 0.1s ease"
    },
    // Hover effect for lines
    ".cm-line:hover": {
        backgroundColor: "rgba(0, 0, 0, 0.04)"
    }
})

export const tocTheme = EditorView.theme({
    ".cm-content": {
        fontFamily: "'Consolas', 'Monaco', 'Courier New', monospace",
        fontSize: "14px",
        lineHeight: "1.6",
        padding: "16px 22px"
    },
    // Global reset for line padding to ensure alignment
    ".cm-line": {
        padding: "0"
    },
    // Only apply flex to lines with the leader widget
    ".cm-flex-line": {
        display: "flex !important",
        alignItems: "baseline",
        width: "100%"
    },
    ".toc-leader-widget": {
        display: "flex",
        flexGrow: "1", 
        alignItems: "baseline",
        cursor: "default",
        userSelect: "none",
        animation: "leaderFadeIn 0.3s ease-out forwards"
    },
    ".toc-leader-dots": {
        flexGrow: "1",
        margin: "0 1px",
        ...generateDotLeaderData({
            color: '#a0a0a0',
            width: 4,
            height: 4,
            radius: 0.6,
            position: 'left bottom 0px'
        }),
        minHeight: "1em",
        display: "block",
        opacity: "0.6"
    },
    ".toc-page-widget": {
        fontWeight: "bold",
        flexShrink: "0"
    },
    ".toc-math-widget": {
        verticalAlign: "baseline",
        padding: "0 2px"
    },
    ".toc-escape-widget": {
        color: "inherit"
    },
    "&.cm-focused": {
        outline: "none"
    },
    "&.cm-editor:hover .cm-scroller": {
        backgroundColor: "rgba(0, 0, 0, 0.02)"
    },
    ".cm-activeLine": {
        backgroundColor: "transparent" // Disable default active line bg if unwanted
    },
    ".cm-activeLineGutter": {
        backgroundColor: "transparent"
    }
});

const flexLineDecoration = Decoration.line({attributes: {class: "cm-flex-line"}});

export const tocPlugin = ViewPlugin.fromClass(class {
    decorations: any;
    lastCursorLines: Set<number> = new Set();

    constructor(view: EditorView) {
        this.lastCursorLines = this.getCursorLines(view);
        this.decorations = this.computeDecorations(view, this.lastCursorLines);
    }

    update(update: ViewUpdate) {
        const selectionChanged = update.selectionSet;
        const layoutChanged = update.docChanged || update.viewportChanged || update.focusChanged || 
                              update.state.facet(pageValidationConfig) !== update.startState.facet(pageValidationConfig);

        if (layoutChanged) {
            this.lastCursorLines = this.getCursorLines(update.view);
            this.decorations = this.computeDecorations(update.view, this.lastCursorLines);
        } else if (selectionChanged) {
            const currentCursorLines = this.getCursorLines(update.view);
            if (!this.areSetsEqual(this.lastCursorLines, currentCursorLines)) {
                this.lastCursorLines = currentCursorLines;
                this.decorations = this.computeDecorations(update.view, currentCursorLines);
            }
        }
    }

    areSetsEqual(a: Set<number>, b: Set<number>) {
        if (a.size !== b.size) return false;
        for (let item of a) if (!b.has(item)) return false;
        return true;
    }

    getCursorLines(view: EditorView): Set<number> {
        const selection = view.state.selection;
        const hasFocus = view.hasFocus;
        const cursorLines = new Set<number>();
        if (hasFocus) {
            for (const range of selection.ranges) {
                const startLine = view.state.doc.lineAt(range.from).number;
                const endLine = view.state.doc.lineAt(range.to).number;
                for (let i = startLine; i <= endLine; i++) {
                    cursorLines.add(i);
                }
            }
        }
        return cursorLines;
    }

    computeDecorations(view: EditorView, cursorLines: Set<number>) {
        const { offset, totalPage, pageLabels, insertPos } = view.state.facet(pageValidationConfig);
        const builder = new RangeSetBuilder<Decoration>();

        for (const {from, to} of view.visibleRanges) {
            for (let pos = from; pos <= to;) {
                const line = view.state.doc.lineAt(pos);

                // Common logic: Validation
                const text = line.text;
                const parsed = parseTocLine(text);
                
                let isValid = true;
                let targetStr = "";

                if (parsed) {
                    targetStr = parsed.hasExplicitLink ? parsed.linkTarget : parsed.displayPage;
                    isValid = validatePageTarget(targetStr, {
                        offset,
                        totalPage,
                        pageLabels,
                        insertPos
                    });
                }

                // If cursor is NOT on this line -> Fold mode (Show Widgets)
                if (!cursorLines.has(line.number)) {
                    if (parsed) {
                        const titleLen = parsed.title.length;
                        const sepStart = line.from + titleLen;
                        const sepEnd = sepStart + parsed.separator.length; 
                        const lineEnd = line.from + text.length;

                        builder.add(line.from, line.from, flexLineDecoration);

                        const mathNodes = scanMathInString(parsed.title);
                        for (const node of mathNodes) {
                            const nodeStart = line.from + node.start;
                            const nodeEnd = line.from + node.end;
                            if (nodeEnd <= sepStart) {
                                if (node.type === 'escape') {
                                    builder.add(nodeStart, nodeEnd, Decoration.replace({ widget: new EscapeWidget() }));
                                } else if (node.type === 'math' && node.content) {
                                    builder.add(nodeStart, nodeEnd, Decoration.replace({ widget: new MathWidget(node.content) }));
                                }
                            }
                        }

                        builder.add(sepStart, sepEnd, Decoration.replace({ widget: new LeaderWidget(), inclusive: true }));

                        if (lineEnd > sepEnd) {
                             builder.add(sepEnd, lineEnd, Decoration.replace({ widget: new PageWidget(parsed.displayPage, isValid), inclusive: true }));
                        }
                    }
                } 
                // If cursor IS on this line -> Edit mode (Show Source + Source Highlighting)
                else {
                    if (parsed && !isValid) {
                         const start = line.from + parsed.title.length + parsed.separator.length;
                         const end = line.to;
                         if (end > start) {
                             builder.add(start, end, invalidPageDecoration);
                         }
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
