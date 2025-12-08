import { EditorState } from '@codemirror/state';
import { EditorView, keymap, placeholder } from '@codemirror/view';
import { markdown, markdownLanguage } from '@codemirror/lang-markdown';
import { syntaxHighlighting } from '@codemirror/language';
import { languages } from '@codemirror/language-data';
import { defaultKeymap, history, historyKeymap } from '@codemirror/commands';
import { searchKeymap } from '@codemirror/search';
import { GFM } from '@lezer/markdown';

import { myHighlightStyle, baseTheme } from './theme';
import { livePreview, MathExtension } from './extensions';

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
                keymap.of([...defaultKeymap, ...historyKeymap, ...searchKeymap]),
                placeholder(options.placeholder || ''),
                EditorView.lineWrapping,
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
