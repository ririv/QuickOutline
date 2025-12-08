import { EditorView } from '@codemirror/view';
import { HighlightStyle } from '@codemirror/language';
import { tags } from '@lezer/highlight';

export const myHighlightStyle = HighlightStyle.define([
    { tag: tags.heading1, fontSize: "2em", fontWeight: "bold", borderBottom: "1px solid #eee", display: "inline-block", width: "100%", paddingBottom: "0.3em", marginBottom: "0.5em" },
    { tag: tags.heading2, fontSize: "1.5em", fontWeight: "bold", borderBottom: "1px solid #eee", display: "inline-block", width: "100%", paddingBottom: "0.3em" },
    { tag: tags.heading3, fontSize: "1.25em", fontWeight: "bold" },
    { tag: tags.heading, fontWeight: "bold" },
    { tag: tags.strong, fontWeight: "bold" },
    { tag: tags.emphasis, fontStyle: "italic" },
    { tag: tags.monospace, backgroundColor: "rgba(27, 31, 35, 0.05)", borderRadius: "3px", padding: "0.2em 0.4em", fontFamily: "monospace" },
    { tag: tags.link, color: "#0366d6", textDecoration: "underline" },
    { tag: tags.list, paddingLeft: "1em" },
    { tag: tags.quote, borderLeft: "4px solid #dfe2e5", paddingLeft: "1em", color: "#6a737d", fontStyle: "italic" }, // Blockquote
]);

export const baseTheme = EditorView.theme({
    "&": {
        height: "100%",
        fontSize: "16px",
        fontFamily: "'Inter', sans-serif"
    },
    ".cm-scroller": { fontFamily: "inherit" },
    ".cm-content": {
        padding: "40px 60px", // Typora-like padding
        maxWidth: "900px",
        margin: "0 auto",
    },
    "&.cm-focused": { outline: "none" },
    ".cm-blockquote-line": {
        borderLeft: "4px solid #dfe2e5",
        paddingLeft: "1em",
        color: "#6a737d",
        fontStyle: "italic"
    },
    ".cm-fenced-code-line": {
        backgroundColor: "#f6f8fa",
        padding: "0.5em 1em",
        borderRadius: "4px",
        fontFamily: "monospace",
        fontSize: "0.9em",
        lineHeight: "1.5"
    }
});
