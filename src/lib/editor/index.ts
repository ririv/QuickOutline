import {EditorState, Prec, Compartment, type Extension, type StateEffect} from '@codemirror/state';
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

import { myHighlightStyle, baseTheme, codeBlockSyntaxHighlighting, gridTableTheme, academicTableTheme, vsCodeLightHighlightStyle } from './theme';
import { livePreviewState, livePreviewView, MathExtension, mathTooltip, focusState, setFocusState, previewModeState, setPreviewMode } from './extensions';
import { markdownKeymap } from './commands';
import { tableKeymap } from './table-helper';
import { linkHeadingCompletion } from './autocomplete';
import { mdParser } from './markdown-renderer'; // Import the pre-configured mdParser

export interface MarkdownEditorOptions {
    initialValue?: string;
    placeholder?: string;
    initialMode?: EditorMode; // Add initialMode option
    tableStyle?: 'grid' | 'academic'; // New option for table styling
    contentPadding?: string; // New: optional padding for the content area, e.g., "10px 20px"
    onChange?: (doc: string) => void; // Callback when document changes
    parent: HTMLElement;
}

export type EditorMode = 'live' | 'source' | 'rich-source' | 'preview';

export class MarkdownEditor {
    view: EditorView;
    private extensionCompartment = new Compartment();
    private styleCompartment = new Compartment();
    private paddingCompartment = new Compartment(); // New: Compartment for dynamic padding
    private editableCompartment = new Compartment(); // New: Compartment for editable state
    private currentMode: EditorMode;
    private mdParser = mdParser; // Use the imported mdParser

    constructor(options: MarkdownEditorOptions) {
        this.currentMode = options.initialMode || 'live'; // Set initial mode

        // Determine initial extensions based on mode (Live Preview)
        const initialLivePreviewExtensions = (this.currentMode === 'live' || this.currentMode === 'preview') 
            ? [livePreviewState, livePreviewView] 
            : [];

        // Determine initial syntax highlighting based on mode
        const initialSyntaxHighlighting = this.currentMode === 'source'
            ? [
                syntaxHighlighting(vsCodeLightHighlightStyle),
                syntaxHighlighting(classHighlighter) // Enable CSS classes for scoped styling
            ]
            : [
                syntaxHighlighting(myHighlightStyle),
                syntaxHighlighting(classHighlighter) // Enable CSS classes for scoped styling
            ];

        // Select table theme (default to 'grid')
        const tableTheme = options.tableStyle === 'academic' ? academicTableTheme : gridTableTheme;
        
        // --- New: Dynamic Content Padding ---
        const initialContentPadding = options.contentPadding === undefined
            ? EditorView.theme({}) // Default: no padding
            : EditorView.theme({
                ".cm-content": { padding: options.contentPadding }
            });
        // --- End Dynamic Content Padding ---

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
                previewModeState, // Track preview mode state
                
                // Initial editable state (false if starting in preview mode)
                this.editableCompartment.of(EditorView.editable.of(this.currentMode !== 'preview')),

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
                this.paddingCompartment.of(initialContentPadding), // Inject dynamic padding theme

                // Dynamic Live Preview Extensions
                this.extensionCompartment.of(initialLivePreviewExtensions),

                EditorView.domEventHandlers({
                    focus: (e, v) => v.dispatch({ effects: setFocusState.of(true) }),
                    blur: (e, v) => v.dispatch({ effects: setFocusState.of(false) })
                }),
                
                // OnChange Listener
                EditorView.updateListener.of((update) => {
                    if (update.docChanged && options.onChange) {
                        options.onChange(update.state.doc.toString());
                    }
                })
            ]
        });

        this.view = new EditorView({
            state: startState,
            parent: options.parent
        });
        
        // If initial mode is preview, enforce it immediately (though state above sets editable, we need setPreviewMode for decorators)
        if (this.currentMode === 'preview') {
             this.view.dispatch({ effects: setPreviewMode.of(true) });
        }
    }

    setMode(mode: EditorMode) {
        this.currentMode = mode;
        const effects: StateEffect<unknown>[] = []; // Explicitly type the effects array

        // Configure Extensions (Preview logic)
        if (mode === 'live' || mode === 'preview') {
            effects.push(this.extensionCompartment.reconfigure([livePreviewState, livePreviewView]));
        } else {
            effects.push(this.extensionCompartment.reconfigure([]));
        }

        // Configure Styles
        const newSyntaxHighlighting = mode === 'source'
            ? [
                syntaxHighlighting(vsCodeLightHighlightStyle),
                syntaxHighlighting(classHighlighter)
            ]
            : [
                syntaxHighlighting(myHighlightStyle),
                syntaxHighlighting(classHighlighter)
            ];

        effects.push(this.styleCompartment.reconfigure(newSyntaxHighlighting));
        
        // Handle Preview Mode Specifics
        if (mode === 'preview') {
            effects.push(setPreviewMode.of(true));
            effects.push(this.editableCompartment.reconfigure(EditorView.editable.of(false)));
        } else {
            effects.push(setPreviewMode.of(false));
            effects.push(this.editableCompartment.reconfigure(EditorView.editable.of(true)));
        }

        this.view.dispatch({ effects });
    }

    getValue(): string {
        return this.view.state.doc.toString();
    }
    
    getHTML(): string {
        return this.mdParser.render(this.getValue());
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
