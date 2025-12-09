import { EditorView } from '@codemirror/view';
import { HighlightStyle } from '@codemirror/language';
import { tags } from '@lezer/highlight';

// --- Theme Variables (CSS Custom Properties) ---
// These are the source of truth for colors.
export const defaultThemeVars = {
    // Syntax Highlighting (GitHub Light)
    "--syntax-comment": "#6a737d",
    "--syntax-keyword": "#d73a49",
    "--syntax-string": "#032f62",
    "--syntax-regexp": "#032f62",
    "--syntax-number": "#005cc5",
    "--syntax-bool": "#005cc5",
    "--syntax-function": "#6f42c1", // Definition, Function, Type
    "--syntax-variable": "#24292e", // Atom, Label, Namespace, Variable
    "--syntax-property": "#005cc5",
    "--syntax-punctuation": "#24292e",

    // Editor UI
    "--editor-bg": "#ffffff",
    "--editor-text": "#24292e",
    "--code-block-bg": "#f6f8fa",
    "--blockquote-border": "#dfe2e5",
    "--blockquote-text": "#6a737d",
    
    // Table UI
    "--table-border": "#ddd",
    "--table-header-bg": "#fcfcfc",
    "--table-row-alt-bg": "#f8f9fa",
    "--table-header-underline": "#000",
    "--table-row-underline": "#eee"
};

// Apply variables to Editor
export const themeVariables = EditorView.theme({
    "&": defaultThemeVars
});

export const myHighlightStyle = HighlightStyle.define([
    { tag: tags.heading1, fontSize: "2em", fontWeight: "bold", borderBottom: "1px solid #eee", display: "inline-block", width: "100%", paddingBottom: "0.3em", marginBottom: "0.5em" },
    { tag: tags.heading2, fontSize: "1.5em", fontWeight: "bold", borderBottom: "1px solid #eee", display: "inline-block", width: "100%", paddingBottom: "0.3em" },
    { tag: tags.heading3, fontSize: "1.25em", fontWeight: "bold" },
    { tag: tags.heading, fontWeight: "bold" },
    { tag: tags.strong, fontWeight: "bold" },
    { tag: tags.emphasis, fontStyle: "italic" },
    { tag: tags.monospace, backgroundColor: "rgba(27, 31, 35, 0.05)", borderRadius: "3px", padding: "0.2em 0.4em", fontFamily: "monospace" },
    { tag: tags.link, color: "#0366d6", textDecoration: "underline" },
    { tag: tags.list },
    { tag: tags.quote, borderLeft: "4px solid var(--blockquote-border)", paddingLeft: "1em", color: "var(--blockquote-text)", fontStyle: "italic" } // Blockquote
]);

// Export raw styles for reuse in Preview (Paged.js)
export const baseThemeStyles = {
    "&": {
        height: "100%",
        fontSize: "16px",
        fontFamily: "'Inter', sans-serif",
        color: "var(--editor-text)",
        backgroundColor: "var(--editor-bg)"
    },
    ".cm-scroller": { fontFamily: "inherit" },
    ".cm-content": {
        padding: "16px 22px",
        // maxWidth: "900px",    // Removed: now dynamic or full width
        // margin: "0 auto",     // Removed: now dynamic or full width
        minHeight: "100%", // Ensure content area expands to fill available height
        flexGrow: "1" // Allow content area to fill available horizontal space
    },
    "&.cm-focused": { outline: "none" },
    ".cm-blockquote-line": {
        borderLeft: "4px solid var(--blockquote-border)",
        paddingLeft: "1em",
        color: "var(--blockquote-text)",
        fontStyle: "italic"
    },
    ".cm-fenced-code-line": {
        backgroundColor: "var(--code-block-bg)",
        padding: "0.5em 1em",
        borderRadius: "4px",
        fontFamily: "monospace",
        fontSize: "0.9em",
        lineHeight: "1.5"
    },
    
    // 2. Edit Mode (Source View) - Always present in base theme
    ".cm-table-edit-mode": {
        fontFamily: "monospace", // Critical for pipe alignment
        whiteSpace: "pre",       // Critical for preserving spaces
        color: "var(--editor-text)",
        backgroundColor: "rgba(0,0,0,0.02)" // Subtle background for context
    },

    // --- Autocomplete Tooltip Styling ---
    ".cm-tooltip": {
        border: "none",
        backgroundColor: "var(--editor-bg)",
        borderRadius: "8px",
        boxShadow: "0 4px 12px rgba(0, 0, 0, 0.15), 0 0 1px rgba(0,0,0,0.1)",
        padding: "4px 0",
        overflow: "hidden"
    },
    ".cm-tooltip-autocomplete": {
        "& > ul": {
            fontFamily: "inherit",
            whiteSpace: "nowrap",
            overflow: "hidden auto",
            maxWidth: "700px",
            minWidth: "250px",
            maxHeight: "10em",
            "& > li": {
                padding: "6px 12px",
                lineHeight: "1.4",
                cursor: "pointer",
                display: "flex",
                alignItems: "center",
                gap: "8px"
            },
            "& > li[aria-selected]": {
                backgroundColor: "#e8f0fe", // Light blue selection
                color: "#1a73e8"
            },
            "& > li:hover": {
                backgroundColor: "#f5f5f5"
            }
        }
    },
    ".cm-completionLabel": {
        fontWeight: "500"
    },
    ".cm-completionDetail": {
        color: "#888",
        fontStyle: "normal",
        fontSize: "0.85em",
        marginLeft: "auto" // Push detail to the right
    },
    
    // --- Completion Icons (Heading Levels) ---
    ".cm-completionIcon": {
        marginRight: "8px",
        width: "20px",
        textAlign: "center",
        display: "inline-block"
    },
    
    // Custom icons for Heading Levels (generated by type: "H1", "H2" etc)
    ".cm-completionIcon-H1": { 
        "&:after": { content: "'H1'" },
        backgroundColor: "#e53935", color: "white", 
        fontSize: "9px", borderRadius: "3px", padding: "2px 4px", fontWeight: "bold", verticalAlign: "middle"
    },
    ".cm-completionIcon-H2": { 
        "&:after": { content: "'H2'" },
        backgroundColor: "#fb8c00", color: "white", 
        fontSize: "9px", borderRadius: "3px", padding: "2px 4px", fontWeight: "bold", verticalAlign: "middle"
    },
    ".cm-completionIcon-H3": { 
        "&:after": { content: "'H3'" },
        backgroundColor: "#fdd835", color: "black", 
        fontSize: "9px", borderRadius: "3px", padding: "2px 4px", fontWeight: "bold", verticalAlign: "middle"
    },
    ".cm-completionIcon-H4": { 
        "&:after": { content: "'H4'" },
        backgroundColor: "#43a047", color: "white", 
        fontSize: "9px", borderRadius: "3px", padding: "2px 4px", fontWeight: "bold", verticalAlign: "middle"
    },
    ".cm-completionIcon-H5": { 
        "&:after": { content: "'H5'" },
        backgroundColor: "#1e88e5", color: "white", 
        fontSize: "9px", borderRadius: "3px", padding: "2px 4px", fontWeight: "bold", verticalAlign: "middle"
    },
    ".cm-completionIcon-H6": { 
        "&:after": { content: "'H6'" },
        backgroundColor: "#8e24aa", color: "white", 
        fontSize: "9px", borderRadius: "3px", padding: "2px 4px", fontWeight: "bold", verticalAlign: "middle"
    },

    ".cm-completionIcon-keyword": { // Fallback
        display: "none"
    }
};

export const baseTheme = EditorView.theme(baseThemeStyles);

// --- Scoped Syntax Highlighting (Physical Scope via CSS) ---
// These rules ONLY apply inside code block lines (.cm-fenced-code-line)
// This guarantees strict isolation from Markdown prose.
export const codeBlockStyles = {
    ".cm-fenced-code-line .tok-comment": { color: "var(--syntax-comment)", fontStyle: "italic" },
    ".cm-fenced-code-line .tok-keyword, .cm-fenced-code-line .tok-operatorKeyword, .cm-fenced-code-line .tok-modifier": { color: "var(--syntax-keyword)" },
    ".cm-fenced-code-line .tok-string, .cm-fenced-code-line .tok-regexp": { color: "var(--syntax-string)" },
    ".cm-fenced-code-line .tok-number, .cm-fenced-code-line .tok-bool, .cm-fenced-code-line .tok-null": { color: "var(--syntax-number)" },
    
    // For composite tags like definition(variableName), classHighlighter adds multiple classes.
    // We target the specific combinations or the base classes.
    ".cm-fenced-code-line .tok-definition, .cm-fenced-code-line .tok-function, .cm-fenced-code-line .tok-className, .cm-fenced-code-line .tok-typeName": { color: "var(--syntax-function)" },
    
    ".cm-fenced-code-line .tok-atom, .cm-fenced-code-line .tok-labelName, .cm-fenced-code-line .tok-namespace": { color: "var(--syntax-variable)" },
    ".cm-fenced-code-line .tok-propertyName, .cm-fenced-code-line .tok-attributeName": { color: "var(--syntax-property)" },
    ".cm-fenced-code-line .tok-variableName": { color: "var(--syntax-variable)" },
    ".cm-fenced-code-line .tok-squareBracket, .cm-fenced-code-line .tok-brace, .cm-fenced-code-line .tok-punctuation": { color: "var(--syntax-punctuation)" },
};
export const codeBlockSyntaxHighlighting = EditorView.theme(codeBlockStyles);

// --- Table Themes (Selectable) ---

// 1. Grid/Spreadsheet Style (Default)
export const gridTableStyles = {
    ".cm-table-widget": {
        borderCollapse: "collapse",
        width: "100%",
        margin: "1em 0",
        fontSize: "0.9em",
        border: "1px solid var(--table-border)",
    },
    ".cm-table-widget th, .cm-table-widget td": {
        border: "1px solid var(--table-border)",
        padding: "8px 12px",
        textAlign: "left",
        verticalAlign: "top",
    },
    ".cm-table-widget th": {
        fontWeight: "bold",
        backgroundColor: "var(--table-header-bg)",
    },
    ".cm-table-widget tr:nth-child(even)": {
        backgroundColor: "var(--table-row-alt-bg)",
    },
};
export const gridTableTheme = EditorView.theme(gridTableStyles);

// 2. Academic Paper Style (Clean, no vertical borders)
export const academicTableStyles = {
    ".cm-table-widget": {
        borderCollapse: "collapse",
        width: "100%",
        margin: "1em 0",
        fontSize: "0.9em",
        borderTop: "2px solid var(--table-header-underline)",
        borderBottom: "2px solid var(--table-header-underline)",
    },
    ".cm-table-widget th, .cm-table-widget td": {
        border: "none",
        padding: "8px 12px",
        textAlign: "left",
        verticalAlign: "top",
    },
    ".cm-table-widget th": {
        fontWeight: "bold",
        backgroundColor: "inherit",
        borderBottom: "1px solid var(--table-header-underline)", // Header underline
        paddingBottom: "10px",
        paddingTop: "10px",
    },
    ".cm-table-widget td": {
        borderBottom: "1px solid var(--table-row-underline)", // Light row separator
        backgroundColor: "inherit",
    },
    ".cm-table-widget tr:last-child td": {
        borderBottom: "none",
    },
};
export const academicTableTheme = EditorView.theme(academicTableStyles);

// --- VS Code Light Highlight Style (Markdown Optimized) ---
export const vsCodeLightHighlightStyle = HighlightStyle.define([
    // Markdown Specifics
    { tag: tags.heading, fontWeight: "bold", color: "#005cc5" }, // Blue headings (Keep hardcoded or define new vars if needed, sticking to hardcoded for specific Markdown style)
    { tag: tags.list, color: "#735c0f" }, // Yellow/Orange bullets
    { tag: tags.quote, color: "var(--blockquote-text)", fontStyle: "italic" }, // Match vars
    { tag: tags.link, color: "#032f62", textDecoration: "underline" },
    { tag: tags.url, color: "#032f62", textDecoration: "underline" },
    { tag: tags.strong, fontWeight: "bold", color: "var(--editor-text)" },
    { tag: tags.emphasis, fontStyle: "italic", color: "var(--editor-text)" },
    { tag: tags.monospace, color: "var(--editor-text)" }, 
    
    // Markdown Markers (hash, star, bracket)
    { tag: tags.processingInstruction, color: "#005cc5" }, 
    { tag: tags.meta, color: "var(--syntax-comment)" },

    // General Code Syntax (Use Variables for Consistency)
    { tag: tags.keyword, color: "var(--syntax-keyword)" },
    { tag: tags.atom, color: "var(--syntax-variable)" },
    { tag: tags.number, color: "var(--syntax-number)" },
    { tag: tags.string, color: "var(--syntax-string)" },
    { tag: tags.comment, color: "var(--syntax-comment)", fontStyle: "italic" },
    { tag: [tags.definition(tags.variableName), tags.function(tags.variableName)], color: "var(--syntax-function)" },
    { tag: tags.variableName, color: "var(--syntax-variable)" },
]);
