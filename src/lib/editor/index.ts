import { EditorState, Prec } from '@codemirror/state';
import { EditorView, keymap, placeholder, showTooltip } from '@codemirror/view';
import { markdown, markdownLanguage } from '@codemirror/lang-markdown';
import { syntaxHighlighting } from '@codemirror/language';
import { languages } from '@codemirror/language-data';
import { defaultKeymap, history, historyKeymap } from '@codemirror/commands';
import { searchKeymap } from '@codemirror/search';
import { autocompletion, closeBrackets, closeBracketsKeymap } from '@codemirror/autocomplete';
import { GFM } from '@lezer/markdown';

import { myHighlightStyle, baseTheme } from './theme';
import { livePreview, MathExtension, mathTooltip } from './extensions';
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

    constructor(options: MarkdownEditorOptions) {
        const startState = EditorState.create({
            doc: options.initialValue || '',
            extensions: [
                history(),
                // Use Prec.high for table keymap to ensure it overrides default behaviors like indent
                Prec.high(keymap.of(tableKeymap)),
                keymap.of([
                    ...markdownKeymap, 
                    ...closeBracketsKeymap,
                    ...defaultKeymap, 
                    ...historyKeymap, 
                    ...searchKeymap
                ]),
                placeholder(options.placeholder || ''),
                EditorView.lineWrapping,
                autocompletion({ override: [linkHeadingCompletion] }), // Custom completion source
                closeBrackets(),
                showTooltip.compute(['selection'], mathTooltip), // Enable Math Tooltip (computed from selection)
                markdown({ 
                    base: markdownLanguage, 
                    codeLanguages: languages,
                    extensions: [GFM, MathExtension] 
                }),
                syntaxHighlighting(myHighlightStyle),
                baseTheme,
                livePreview,
                EditorView.domEventHandlers({
                    focus: (e, v) => v.dispatch({ effects: [] }),
                    blur: (e, v) => v.dispatch({ effects: [] })
                })
            ]
        });

        this.view = new EditorView({
            state: startState,
            parent: options.parent
        });
    }    getValue(): string {
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
