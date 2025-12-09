import {EditorState, Prec, Compartment, type Extension} from '@codemirror/state';
import { EditorView, keymap, placeholder, showTooltip, drawSelection, dropCursor } from '@codemirror/view';
import { markdown, markdownLanguage } from '@codemirror/lang-markdown';
import { syntaxHighlighting, defaultHighlightStyle } from '@codemirror/language';
import { languages } from '@codemirror/language-data';
import { defaultKeymap, history, historyKeymap } from '@codemirror/commands';
import { searchKeymap, highlightSelectionMatches } from '@codemirror/search';
import { autocompletion, closeBrackets, closeBracketsKeymap } from '@codemirror/autocomplete';
import { GFM } from '@lezer/markdown';
import { bracketMatching } from '@codemirror/language';
import { classHighlighter } from '@lezer/highlight';

import { myHighlightStyle, baseTheme, codeBlockSyntaxHighlighting, gridTableTheme, academicTableTheme } from './theme';
import { livePreviewState, livePreviewView, MathExtension, mathTooltip, focusState, setFocusState } from './extensions';
import { markdownKeymap } from './commands';
import { tableKeymap } from './table-helper';
import { linkHeadingCompletion } from './autocomplete';

export interface MarkdownEditorOptions {
    initialValue?: string;
    placeholder?: string;
    initialMode?: EditorMode; // Add initialMode option
    tableStyle?: 'grid' | 'academic'; // <--- 新增这行
    parent: HTMLElement;
}

export type EditorMode = 'live' | 'source' | 'rich-source';

export class MarkdownEditor {
    view: EditorView;
    private extensionCompartment = new Compartment();
    private styleCompartment = new Compartment();
    private currentMode: EditorMode;

    constructor(options: MarkdownEditorOptions) {
        this.currentMode = options.initialMode || 'live'; // Set initial mode

        // Determine initial extensions based on mode (Live Preview)
        const initialLivePreviewExtensions = (this.currentMode === 'live') ? [livePreviewState, livePreviewView] : [];

        // Determine initial syntax highlighting based on mode
        const initialSyntaxHighlighting = this.currentMode === 'source'
            ? [
                syntaxHighlighting(defaultHighlightStyle),
                syntaxHighlighting(classHighlighter) // Enable CSS classes for scoped styling
            ]
            : [
                syntaxHighlighting(myHighlightStyle),
                syntaxHighlighting(classHighlighter) // Enable CSS classes for scoped styling
            ];
        const tableTheme = options.tableStyle === 'academic' ? academicTableTheme : gridTableTheme;

        const startState = EditorState.create({
            doc: options.initialValue || '',
            extensions: [
                history(),
                keymap.of([
                    ...tableKeymap, // Ensure table keymap is evaluated first for Tab/Enter
                    ...markdownKeymap,
                    ...closeBracketsKeymap,
                    ...defaultKeymap,
                    ...historyKeymap,
                    ...searchKeymap
                ]),
                placeholder(options.placeholder || ''),
                EditorView.lineWrapping,
                drawSelection(), // Fix cursor artifacts by using custom selection drawing
                dropCursor(), // Add dropCursor
                highlightSelectionMatches(), // Add highlightSelectionMatches
                bracketMatching(), // Add bracketMatching
                autocompletion({ override: [linkHeadingCompletion] }), // Custom completion source
                closeBrackets(),
                showTooltip.compute(['selection'], mathTooltip), // Enable Math Tooltip
                focusState, // Track focus state
                markdown({
                    base: markdownLanguage,
                    codeLanguages: languages,
                    extensions: [GFM, MathExtension]
                }),
                // Dynamic Styling
                this.styleCompartment.of(initialSyntaxHighlighting),
                baseTheme,
                codeBlockSyntaxHighlighting,
                tableTheme,

                // Dynamic Live Preview Extensions
                this.extensionCompartment.of(initialLivePreviewExtensions),
                EditorView.domEventHandlers({
                    focus: (e, v) => v.dispatch({ effects: setFocusState.of(true) }),
                    blur: (e, v) => v.dispatch({ effects: setFocusState.of(false) })
                })
            ]
        });

        this.view = new EditorView({
            state: startState,
            parent: options.parent
        });
    }

    setMode(mode: EditorMode) {
        this.currentMode = mode;
        const effects = [];

        // Configure Extensions (Preview logic)
        if (mode === 'live') {
            effects.push(this.extensionCompartment.reconfigure([livePreviewState, livePreviewView]));
        } else {
            effects.push(this.extensionCompartment.reconfigure([]));
        }

        // Configure Styles
        const newSyntaxHighlighting = mode === 'source'
            ? [
                syntaxHighlighting(defaultHighlightStyle),
                syntaxHighlighting(classHighlighter)
            ]
            : [
                syntaxHighlighting(myHighlightStyle),
                syntaxHighlighting(classHighlighter)
            ];

        effects.push(this.styleCompartment.reconfigure(newSyntaxHighlighting));

        this.view.dispatch({ effects });
    }

    getValue(): string {
        return this.view.state.doc.toString();
    }

    setValue(val: string) {
        this.view.dispatch({
            changes: { from: 0, to: this.view.state.doc.length, insert: val }
        });
    }

    insertValue(val: string) {
        const range = this.view.state.selection.main;
        this.view.dispatch({
            changes: { from: range.from, to: range.to, insert: val },
            selection: { anchor: range.from + val.length }
        });
    }

    insertImageMarkdown(path: string) {
        this.insertValue(`\n![](${path})\n`);
    }

    destroy() {
        this.view.destroy();
    }
}
