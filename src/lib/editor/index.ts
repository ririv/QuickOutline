import { EditorState, Prec, Compartment } from '@codemirror/state';
import { EditorView, keymap, placeholder, showTooltip, drawSelection, dropCursor } from '@codemirror/view';
import { markdown, markdownLanguage } from '@codemirror/lang-markdown';
import { syntaxHighlighting } from '@codemirror/language';
import { languages } from '@codemirror/language-data';
import { defaultKeymap, history, historyKeymap } from '@codemirror/commands'; // Removed bracketMatching and bracketMatchingKeymap
import { searchKeymap, highlightSelectionMatches } from '@codemirror/search';
import { autocompletion, closeBrackets, closeBracketsKeymap } from '@codemirror/autocomplete';
import { GFM } from '@lezer/markdown';
import { bracketMatching } from '@codemirror/language'; // Correct import for bracketMatching

import { myHighlightStyle, baseTheme } from './theme';
import { livePreviewState, livePreviewView, MathExtension, mathTooltip, focusState, setFocusState } from './extensions';
import { markdownKeymap } from './commands';
import { tableKeymap } from './table-helper';
import { linkHeadingCompletion } from './autocomplete';

export interface MarkdownEditorOptions {
    initialValue?: string;
    placeholder?: string;
    parent: HTMLElement;
}

export class MarkdownEditor {
    view: EditorView;
    private previewCompartment = new Compartment();
    private isSourceMode = false;

    constructor(options: MarkdownEditorOptions) {
        const startState = EditorState.create({
            doc: options.initialValue || '',
            extensions: [
                history(),
                keymap.of([
                    ...markdownKeymap, 
                    ...tableKeymap, // Add bracket matching keymap
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
                syntaxHighlighting(myHighlightStyle),
                baseTheme,
                
                // Dynamic Live Preview Extensions
                this.previewCompartment.of([livePreviewState, livePreviewView]),
                
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

    toggleSourceMode() {
        this.isSourceMode = !this.isSourceMode;
        this.view.dispatch({
            effects: this.previewCompartment.reconfigure(
                this.isSourceMode ? [] : [livePreviewState, livePreviewView]
            )
        });
    }

    getValue(): string {        return this.view.state.doc.toString();
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
