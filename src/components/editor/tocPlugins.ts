import { EditorView, keymap, lineNumbers, highlightActiveLine, highlightActiveLineGutter, ViewPlugin, Decoration, type ViewUpdate, WidgetType } from '@codemirror/view';
import { generateDotLeaderData } from '@/lib/templates/toc/toc-gen/toc-generator.tsx';
import { parseTocLine } from '@/lib/templates/toc/toc-gen/parser';
import { resolveLinkTarget } from '@/lib/services/PageLinkResolver';
import { EditorState, RangeSetBuilder, Facet } from '@codemirror/state';

// --- Page Validation Plugin ---

export interface ValidationState {
    offset: number;
    totalPage: number;
    labels: string[] | null;
    insertPos: number;
}

export const pageValidationConfig = Facet.define<ValidationState, ValidationState>({
    combine: values => values[0] || { offset: 0, totalPage: 0, labels: null, insertPos: 0 }
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
        const { offset, totalPage, labels, insertPos } = view.state.facet(pageValidationConfig);
        if (totalPage <= 0) return Decoration.none;

        const builder = new RangeSetBuilder<Decoration>();

        // We need a resolver config
        const resolverConfig = {
            labels: labels,
            offset: offset,
            insertPos: insertPos
        };

        for (const { from, to } of view.visibleRanges) {
            for (let pos = from; pos <= to;) {
                const line = view.state.doc.lineAt(pos);
                const text = line.text;
                const parsed = parseTocLine(text);

                if (parsed) {
                    // Determine the target link string (either explicit <...> or display page)
                    const targetStr = parsed.hasExplicitLink ? parsed.linkTarget : parsed.displayPage;
                    
                    // Resolve to physical index (0-based)
                    const result = resolveLinkTarget(targetStr, resolverConfig);
                    
                    if (result) {
                        const idx = result.index;
                        // Check bounds
                        // If targeting original (isOriginalDoc=true): idx must be within original doc (0 to totalPage-1)
                        // If targeting absolute (isOriginalDoc=false): idx depends on merged doc size...
                        // But wait, totalPage passed here is likely the ORIGINAL doc page count ($docStore.pageCount).
                        // If we are targeting TOC (isOriginalDoc=false), we can't easily validate against original count.
                        // However, usually absolute links (#15) are meant for the final doc.
                        // Let's assume validation is primarily for "Original Doc" targets.
                        
                        if (result.isOriginalDoc) {
                             if (idx < 0 || idx >= totalPage) {
                                 builder.add(line.from, line.from, invalidPageDecoration);
                             }
                        } else {
                            // For absolute links (#15), we warn if it seems excessively large compared to original doc?
                            // Or just skip validation because user knows what they are doing.
                            // Let's validate against a loose upper bound if possible, or just skip.
                            // Skipping is safer to avoid false positives.
                        }
                    } else {
                        // Unresolvable link? Maybe mark as warning?
                        // For now, if we can't resolve it (e.g. invalid label), let's mark it as invalid.
                        // But be careful with partial inputs.
                        // If it's a number and we failed (impossible with fallback), but if it's a label "iv" and labels are missing...
                        if (!labels && !/^\d+$/.test(targetStr)) {
                             // If we have no labels loaded, we can't validate non-numeric labels. Don't mark as error.
                        } else {
                             // Mark as invalid if unresolvable
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
                            Decoration.line({attributes: {class: "cm-flex-line"}})
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