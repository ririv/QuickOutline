import { EditorView, keymap, lineNumbers, highlightActiveLine, highlightActiveLineGutter, ViewPlugin, Decoration, type ViewUpdate, WidgetType } from '@codemirror/view';
import { generateDotLeaderData } from '@/lib/templates/toc/toc-gen/toc-generator.tsx';
import { parseTocLine } from '@/lib/templates/toc/toc-gen/parser';
import { validatePageTarget } from '@/lib/services/PageLinkResolver';
import { EditorState, RangeSetBuilder, Facet } from '@codemirror/state';

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

const invalidPageDecoration = Decoration.line({ class: "cm-invalid-page-line" });

const pageValidationTheme = EditorView.baseTheme({
    ".cm-invalid-page-line": {
        backgroundColor: "rgba(255, 0, 0, 0.15)"
    }
});

const pageValidationPlugin = ViewPlugin.fromClass(class {
    decorations: any;

    constructor(view: EditorView) {
        this.decorations = this.compute(view);
    }

    update(update: ViewUpdate) {
        if (update.docChanged || update.viewportChanged || update.state.facet(pageValidationConfig) !== update.startState.facet(pageValidationConfig)) {
            this.decorations = this.compute(update.view);
        }
    }

    compute(view: EditorView) {
        const { offset, totalPage, pageLabels, insertPos } = view.state.facet(pageValidationConfig);
        if (totalPage <= 0) return Decoration.none;

        const builder = new RangeSetBuilder<Decoration>();

        for (const { from, to } of view.visibleRanges) {
            for (let pos = from; pos <= to;) {
                const line = view.state.doc.lineAt(pos);
                const text = line.text;
                const parsed = parseTocLine(text);

                if (parsed) {
                    // Determine the target link string (either explicit <...> or display page)
                    const targetStr = parsed.hasExplicitLink ? parsed.linkTarget : parsed.displayPage;
                    
                    const isValid = validatePageTarget(targetStr, {
                        offset,
                        totalPage,
                        pageLabels,
                        insertPos
                    });

                    if (!isValid) {
                        builder.add(line.from, line.from, invalidPageDecoration);
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

export const pageValidationExtension = [
    pageValidationTheme,
    pageValidationPlugin
];

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
        // Empty content ensures baseline aligns with bottom edge
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
        // marginLeft: "4px", // 根据最新要求，移除此行
        cursor: "default",
        userSelect: "none",
        animation: "leaderFadeIn 0.3s ease-out forwards" // 添加淡入动画
    },
    ".toc-leader-dots": {
        flexGrow: "1",
        margin: "0 1px", /* 左右间距调整为1px */
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
    ".toc-leader-page": {
        fontWeight: "bold",
        flexShrink: "0"
    },
    "&.cm-focused": {
        outline: "none"
    },
    // Hover effect for the editor content area
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
        const layoutChanged = update.docChanged || update.viewportChanged || update.focusChanged;

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
        const builder = new RangeSetBuilder<Decoration>();

        for (const {from, to} of view.visibleRanges) {
            for (let pos = from; pos <= to;) {
                const line = view.state.doc.lineAt(pos);

                // Only decorate if cursor is NOT on this line
                if (!cursorLines.has(line.number)) {
                    const text = line.text;

                    const parsed = parseTocLine(text);


                    if (parsed) {

                        const titleLen = parsed.title.length;

                        const sepStart = line.from + titleLen;
                        const lineEnd = line.from + text.length;

                        // 1. First add the line class (at line start)
                        builder.add(
                            line.from,
                            line.from,
                            flexLineDecoration
                        );

                        // 2. Then add the widget replacement (at separator position)
                        builder.add(
                            sepStart,
                            lineEnd,
                            Decoration.replace({
                                widget: new LeaderWidget(parsed.displayPage),
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