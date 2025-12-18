import { EditorView, keymap, lineNumbers, highlightActiveLine, highlightActiveLineGutter, ViewPlugin, Decoration, type ViewUpdate, WidgetType } from '@codemirror/view';
import { generateDotLeaderData } from '@/lib/toc-gen/toc-generator.tsx';
import { EditorState, RangeSetBuilder, Facet } from '@codemirror/state';

// --- Page Validation Plugin ---

export const pageValidationConfig = Facet.define<{offset: number, totalPage: number}, {offset: number, totalPage: number}>({
    combine: values => values[0] || { offset: 0, totalPage: 0 }
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
        const { offset, totalPage } = view.state.facet(pageValidationConfig);
        if (totalPage <= 0) return Decoration.none;

        const builder = new RangeSetBuilder<Decoration>();

        for (const { from, to } of view.visibleRanges) {
            for (let pos = from; pos <= to;) {
                const line = view.state.doc.lineAt(pos);
                const text = line.text;
                const match = text.match(tocLineRegex);

                if (match) {
                    // match[3] is the page number string
                    const pageNum = parseInt(match[3], 10);
                    if (!isNaN(pageNum)) {
                        // Check if page + offset exceeds total pages
                        // Note: offset is usually added to the logical page number to get physical page number
                        if (pageNum + offset > totalPage || pageNum + offset < 1) {
                             builder.add(line.from, line.from, invalidPageDecoration);
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

// Match: Title + (at least 1 space/tab) + PageNumber(digits) + EndOfLine
// Aggressive: Any whitespace before trailing digits triggers formatting
const tocLineRegex = /^(.*?)(\s+)(\d+)$/;

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


export const tocPlugin = ViewPlugin.fromClass(class {
    decorations: any;

    constructor(view: EditorView) {
        this.decorations = this.computeDecorations(view);
    }

    update(update: ViewUpdate) {
        if (update.docChanged || update.viewportChanged || update.selectionSet || update.focusChanged) {
            this.decorations = this.computeDecorations(update.view);
        }
    }

    computeDecorations(view: EditorView) {
        const builder = new RangeSetBuilder<Decoration>();

        // Get current cursor line(s) to exclude them from formatting
        // Only exclude if the editor HAS FOCUS. If blurred, show all widgets.
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

                        // 1. First add the line class (at line start)
                        builder.add(
                            line.from,
                            line.from,
                            Decoration.line({ attributes: { class: "cm-flex-line" } })
                        );

                        // 2. Then add the widget replacement (at separator position)
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