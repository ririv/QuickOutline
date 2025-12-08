import { EditorView, type KeyBinding } from '@codemirror/view';
import { EditorState, Transaction, type StateCommand, Text, type ChangeSpec, EditorSelection } from '@codemirror/state';

// --- Helper Functions ---

/**
 * Wraps the selection with the given prefix and suffix.
 * If the selection is already wrapped, it unwraps it.
 */
function toggleWrapper(prefix: string, suffix: string): StateCommand {
    return ({ state, dispatch }) => {
        const changes: ChangeSpec[] = [];
        const newSelections = state.selection.ranges.map(range => {
            const from = range.from, to = range.to;
            const text = state.sliceDoc(from, to);
            
            // Check if already wrapped
            // We need to check context around the selection too for 0-length selection
            const doc = state.doc;
            const isWrapped = 
                (from - prefix.length >= 0 && doc.sliceString(from - prefix.length, from) === prefix) &&
                (to + suffix.length <= doc.length && doc.sliceString(to, to + suffix.length) === suffix);

            if (isWrapped) {
                // Unwrap
                changes.push({
                    from: from - prefix.length,
                    to: to + suffix.length,
                    insert: text
                });
                // Adjust selection: keep the text selected, but shift back by prefix length
                return EditorSelection.range(from - prefix.length, to - prefix.length);
            } else {
                // Wrap
                changes.push({
                    from: from,
                    to: to,
                    insert: prefix + text + suffix
                });
                // Adjust selection: select the inner text
                return EditorSelection.range(from + prefix.length, to + prefix.length);
            }
        });

        if (changes.length > 0) {
            dispatch(state.update({
                changes,
                selection: EditorSelection.create(newSelections, state.selection.mainIndex), // Use EditorSelection.create for multiple ranges
                scrollIntoView: true,
                userEvent: "input.format"
            }));
            return true;
        }
        return false;
    };
}

// --- Commands ---

export const toggleBold: StateCommand = toggleWrapper('**', '**');
export const toggleItalic: StateCommand = toggleWrapper('*', '*');
export const toggleCode: StateCommand = toggleWrapper('`', '`');
export const toggleStrikethrough: StateCommand = toggleWrapper('~~', '~~');

export const insertLink: StateCommand = ({ state, dispatch }) => {
    const changes: ChangeSpec[] = [];
    const newSelections = state.selection.ranges.map(range => {
        const text = state.sliceDoc(range.from, range.to);
        if (range.empty) {
            // No selection: insert []() and put cursor inside []
            changes.push({ from: range.from, to: range.to, insert: '[]()' });
            return EditorSelection.cursor(range.from + 1); // inside []
        } else {
            // Selection: wrap as [text]() and put cursor inside ()
            changes.push({ from: range.from, to: range.to, insert: `[${text}]()` });
            return EditorSelection.cursor(range.to + 3); // [text]() -> cursor at end-1
        }
    });

    dispatch(state.update({
        changes,
        selection: EditorSelection.create(newSelections, state.selection.mainIndex),
        scrollIntoView: true,
        userEvent: "input.link"
    }));
    return true;
};

// --- Keymap ---

export const markdownKeymap: KeyBinding[] = [
    { key: "Mod-b", run: toggleBold, preventDefault: true },
    { key: "Mod-i", run: toggleItalic, preventDefault: true },
    { key: "Mod-`", run: toggleCode, preventDefault: true },
    { key: "Mod-u", run: toggleWrapper('<u>', '</u>'), preventDefault: true }, // Not standard MD but requested
    { key: "Mod-k", run: insertLink, preventDefault: true },
    { key: "Mod-Alt-s", run: toggleStrikethrough, preventDefault: true },
];
