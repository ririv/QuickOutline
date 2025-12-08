import { EditorState, Prec, Compartment } from '@codemirror/state';
import { EditorView, keymap, placeholder, showTooltip, drawSelection, dropCursor } from '@codemirror/view';
import { markdown, markdownLanguage } from '@codemirror/lang-markdown';
import { syntaxHighlighting, defaultHighlightStyle } from '@codemirror/language';
import { languages } from '@codemirror/language-data';
import { defaultKeymap, history, historyKeymap } from '@codemirror/commands';
import { searchKeymap, highlightSelectionMatches } from '@codemirror/search';
import { autocompletion, closeBrackets, closeBracketsKeymap } from '@codemirror/autocomplete';
import { GFM } from '@lezer/markdown';
import { bracketMatching } from '@codemirror/language';

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

export type EditorMode = 'live' | 'source' | 'rich-source';

export class MarkdownEditor {
    view: EditorView;
    private extensionCompartment = new Compartment();
    private styleCompartment = new Compartment();
    private currentMode: EditorMode = 'source'; // Default mode

    constructor(options: MarkdownEditorOptions) {
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
                this.styleCompartment.of(syntaxHighlighting(defaultHighlightStyle)),
                baseTheme,
                
                // Dynamic Live Preview Extensions
                this.extensionCompartment.of([]),
                
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
        if (mode === 'source') {
            effects.push(this.styleCompartment.reconfigure(syntaxHighlighting(defaultHighlightStyle)));
        } else {
            // Both 'live' and 'rich-source' use the rich styling
            effects.push(this.styleCompartment.reconfigure(syntaxHighlighting(myHighlightStyle)));
        }

        this.view.dispatch({ effects });
    }

    toggleSourceMode() {
        // Simple toggle between 'live' and 'source' for compatibility
        const newMode = this.currentMode === 'live' ? 'source' : 'live';
        this.setMode(newMode);
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
