import {Facet, RangeSetBuilder} from "@codemirror/state";
import {Decoration, EditorView, ViewPlugin, type ViewUpdate} from "@codemirror/view";
import {validatePageTarget} from "@/lib/services/PageLinkResolver";

export interface ValidationState {
    offset: number;
    totalPage: number;
    pageLabels: string[] | null;
}

export const pageValidationConfig = Facet.define<ValidationState, ValidationState>({
    combine: values => values[0] || { offset: 0, totalPage: 0, pageLabels: null }
});

const invalidPageDecoration = Decoration.line({ class: "cm-invalid-page-line" });

/**
 * Extracts the potential page target from a bookmark line.
 * Bookmark format: "Indent Title    PageTarget"
 * We look for the last whitespace-separated token.
 */
function getBookmarkTarget(lineText: string): string | null {
    const trimmed = lineText.trim();
    if (!trimmed) return null;
    
    const lastSpaceIndex = trimmed.lastIndexOf(' ');
    if (lastSpaceIndex === -1) return null; // No page found
    
    return trimmed.substring(lastSpaceIndex + 1);
}

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
        const stateChanged = update.docChanged || update.viewportChanged || 
                           update.state.facet(pageValidationConfig) !== update.startState.facet(pageValidationConfig);
        if (stateChanged) {
            this.decorations = this.compute(update.view);
        }
    }

    compute(view: EditorView) {
        const config = view.state.facet(pageValidationConfig);
        if (config.totalPage <= 0) return Decoration.none;

        const builder = new RangeSetBuilder<Decoration>();

        for (const { from, to } of view.visibleRanges) {
            for (let pos = from; pos <= to;) {
                const line = view.state.doc.lineAt(pos);
                const targetStr = getBookmarkTarget(line.text);

                if (targetStr) {
                    const isValid = validatePageTarget(targetStr, {
                        offset: config.offset,
                        totalPage: config.totalPage,
                        pageLabels: config.pageLabels,
                        insertPos: 0 // Bookmarks don't have insertPos context
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

export const lineTheme = EditorView.theme({
    ".cm-line": {
        transition: "background-color 0.1s ease"
    },
    ".cm-line:hover": {
        backgroundColor: "rgba(0, 0, 0, 0.04)"
    }
});