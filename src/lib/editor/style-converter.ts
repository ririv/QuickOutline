import { gridTableStyles, academicTableStyles, baseThemeStyles, codeBlockStyles, defaultThemeVars } from './theme';

// src/lib/editor/style-converter.ts

/**
 * Converts a JavaScript style object (camelCase properties) to a CSS property string (kebab-case).
 * @param rules The style object, e.g., { backgroundColor: "red", fontSize: "12px" }
 * @returns A CSS property string, e.g., "background-color: red; font-size: 12px;"
 */
function objToCssProps(rules: Record<string, string | number>): string {
    return Object.entries(rules).map(([k, v]) => {
        // Convert camelCase to kebab-case
        const prop = k.replace(/[A-Z]/g, m => "-" + m.toLowerCase());
        return `${prop}: ${v};`;
    }).join(" ");
}

/**
 * Converts a CodeMirror-like JavaScript style object (selector -> rules) to a CSS string.
 * This function is designed to map CodeMirror's internal theme selectors to generic HTML tags
 * for use in external Markdown renderers (like Paged.js).
 *
 * @param styles The style object, e.g., { "&": { fontSize: "16px" }, ".cm-table-widget": { ... } }
 * @param rootSelector The CSS selector to prepend to all generated rules (e.g., ".markdown-body" or "").
 *                     If an empty string, styles apply globally.
 * @returns A CSS string.
 */
export function stylesToCss(styles: Record<string, any>, rootSelector = ""): string {
    let css = "";
    
    // Ensure rootSelector is not null/undefined
    rootSelector = rootSelector || "";

    for (const [selector, rules] of Object.entries(styles)) {
        // Handle nested selectors if rules is also an object
        if (typeof rules === 'object' && !Array.isArray(rules) && Object.values(rules).some(v => typeof v === 'object' && v !== null && !Array.isArray(v))) {
             // This indicates a nested selector, like in .cm-tooltip-autocomplete > ul
             // Simple recursive handling for this specific case
             for (const [subSelector, subRules] of Object.entries(rules)) {
                 const fullSelector = `${rootSelector} ${selector.startsWith('&') ? selector.substring(1) : selector} ${subSelector}`;
                 // Assert subRules type for objToCssProps
                 css += `${fullSelector.trim()} { ${objToCssProps(subRules as Record<string, string | number>)} } \n`;
             }
             continue;
        }

        let targetSelector = selector;
        
        // --- Selector Mapping for Preview ---
        if (selector === "&") {
            // Root styles in CM are for the editor itself. For preview, apply to rootSelector
            targetSelector = rootSelector || "body"; 
        } else if (selector === ".cm-table-widget") {
            targetSelector = `${rootSelector} table`;
        } else if (selector.startsWith(".cm-table-widget ")) {
            targetSelector = selector.replace(".cm-table-widget", `${rootSelector} table`);
        } else if (selector === ".cm-blockquote-line") {
            targetSelector = `${rootSelector} blockquote`;
        } else if (selector === ".cm-fenced-code-line") {
            targetSelector = `${rootSelector} pre, ${rootSelector} code`;
        } else if (selector.startsWith(".cm-")) {
            // Ignore other editor-specific classes that don't map directly to HTML tags
            // E.g., .cm-scroller, .cm-focused, .cm-tooltip, .cm-completionIcon
            continue;
        } else if (selector.startsWith(".")) {
            // Generic class selectors, prepend rootSelector
            targetSelector = `${rootSelector} ${selector}`;
        }
        // For direct tag selectors (e.g., "h1"), they are often handled by rules directly.

        // Add the rule
        if (Object.keys(rules).length > 0) {
            css += `${targetSelector.trim()} { ${objToCssProps(rules)} } 
`;
        }
    }
    return css;
}

export function getTableThemeCss(tableStyle: 'grid' | 'academic' | undefined, rootSelector = ""): string {
    const selectedTableStyles = tableStyle === 'academic' ? academicTableStyles : gridTableStyles;
    return stylesToCss(selectedTableStyles, rootSelector);
}

// Mapping from CodeMirror classHighlighter (.tok-*) to Highlight.js (.hljs-*)
const hljsMapping: Record<string, string> = {
    ".tok-comment": ".hljs-comment",
    ".tok-keyword": ".hljs-keyword",
    ".tok-operatorKeyword": ".hljs-keyword", 
    ".tok-modifier": ".hljs-keyword",
    ".tok-string": ".hljs-string",
    ".tok-regexp": ".hljs-regexp",
    ".tok-number": ".hljs-number",
    ".tok-bool": ".hljs-literal", // or built_in
    ".tok-null": ".hljs-literal",
    ".tok-definition": ".hljs-title",
    ".tok-function": ".hljs-function",
    ".tok-className": ".hljs-class",
    ".tok-typeName": ".hljs-type",
    ".tok-atom": ".hljs-symbol",
    ".tok-labelName": ".hljs-symbol",
    ".tok-propertyName": ".hljs-attr",
    ".tok-attributeName": ".hljs-attr",
    ".tok-variableName": ".hljs-variable",
    // highlight.js doesn't usually color braces/punctuation by default, but we can map if needed
    ".tok-squareBracket": ".hljs-punctuation",
    ".tok-brace": ".hljs-punctuation",
    ".tok-punctuation": ".hljs-punctuation"
};

function convertCodeBlockStylesToHljs(styles: Record<string, any>, rootSelector = ""): string {
    let css = "";
    
    // We iterate through our defined codeBlockStyles
    // selector is like: ".cm-fenced-code-line .tok-comment"
    // OR grouped: ".cm-fenced-code-line .tok-keyword, .cm-fenced-code-line .tok-operatorKeyword"
    for (const [selector, rules] of Object.entries(styles)) {
        
        // Split grouped selectors
        const parts = selector.split(',');

        for (const part of parts) {
            const trimmedPart = part.trim();
            // Extract the .tok-* part
            const tokPartMatch = trimmedPart.match(/(\.tok-[\w\d-]+)/);
            
            if (!tokPartMatch) continue;
            
            const tokClass = tokPartMatch[1];
            const hljsClass = hljsMapping[tokClass];
            
            if (hljsClass) {
                // Construct target selector: .markdown-body .hljs .hljs-comment
                // Note: highlight.js puts classes on <span> inside <pre><code> which has class .hljs
                const targetSelector = `${rootSelector} pre code ${hljsClass}, ${rootSelector} .hljs ${hljsClass}`;
                css += `${targetSelector} { ${objToCssProps(rules)} } \n`;
            }
        }
    }
    
    return css;
}

/**
 * Generates the complete CSS for the Markdown preview, mirroring the editor's theme.
 * Includes base theme styles and the selected table theme.
 * 
 * @param tableStyle The selected table style ('grid' | 'academic').
 * @param rootSelector The root CSS selector for the preview container (default: ".markdown-body").
 * @returns A complete CSS string.
 */
export function getEditorPreviewCss(tableStyle: 'grid' | 'academic' | undefined, rootSelector = ".markdown-body"): string {
    // 1. Generate CSS Variables Block
    let varsCss = `${rootSelector} {\n`;
    for (const [key, value] of Object.entries(defaultThemeVars)) {
        varsCss += `    ${key}: ${value};\n`;
    }
    varsCss += "}\n";

    const baseCss = stylesToCss(baseThemeStyles, rootSelector);
    const tableCss = getTableThemeCss(tableStyle, rootSelector);
    const hljsCss = convertCodeBlockStylesToHljs(codeBlockStyles, rootSelector);
    
    return varsCss + "\n" + baseCss + "\n" + tableCss + "\n" + hljsCss;
}
