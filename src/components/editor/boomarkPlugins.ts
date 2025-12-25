import {Facet, RangeSetBuilder} from "@codemirror/state";
import {Decoration, EditorView, ViewPlugin, type ViewUpdate} from "@codemirror/view";
import {parseTocLine} from "@/lib/templates/toc/toc-gen/parser.ts";

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
                const parsed = parseTocLine(text);

                if (parsed) {
                    // Use displayPage for basic numeric validation
                    // This is a "best effort" validation for simple cases
                    const pageNum = parseInt(parsed.displayPage, 10);
                    if (!isNaN(pageNum)) {
                        // Check if page + offset exceeds total pages
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