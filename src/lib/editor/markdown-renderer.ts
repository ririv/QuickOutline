import MarkdownIt from 'markdown-it';
import hljs from 'highlight.js';
import texmath from 'markdown-it-texmath';
import katex from 'katex';
import mdMark from 'markdown-it-mark';
import mdFootnote from 'markdown-it-footnote';
import mdSub from 'markdown-it-sub';
import mdSup from 'markdown-it-sup';
import mdAbbr from 'markdown-it-abbr';
import mdContainer from 'markdown-it-container';
import mdAttrs from 'markdown-it-attrs';
import mdBracketedSpans from 'markdown-it-bracketed-spans'; // Import new plugin

// Import CSS content directly for offline usage (Vite feature)
import katexCssContent from 'katex/dist/katex.min.css?inline';
// highlightJsVsCssContent will be derived from CodeMirror's theme

// Custom task list plugin implementation
function customTaskListPlugin(md: MarkdownIt) {
    md.core.ruler.after('inline', 'custom-task-lists', (state) => {
        const tokens = state.tokens;
        for (let i = 2; i < tokens.length; i++) {
            if (tokens[i].type !== 'inline') continue;
            if (tokens[i - 1].type !== 'paragraph_open') continue;
            if (tokens[i - 2].type !== 'list_item_open') continue;

            const inlineToken = tokens[i];
            const children = inlineToken.children;
            if (!children || children.length === 0) continue;

            const firstChild = children[0];
            if (firstChild.type !== 'text') continue;

            const content = firstChild.content;
            // Match [ ] or [x] or [X] at start
            const match = content.match(/^\ \[([ xX])\] /);
            if (match) {
                const isChecked = match[1].toLowerCase() === 'x';
                
                // Add class to list item
                tokens[i - 2].attrJoin('class', 'task-list-item');

                // Remove the marker from the text
                firstChild.content = content.slice(match[0].length);

                // Create checkbox token
                const checkboxToken = new state.Token('html_inline', '', 0);
                checkboxToken.content = `<span class="custom-checkbox ${isChecked ? 'checked' : ''}"></span> `;
                
                // Insert checkbox at the start of inline children
                children.unshift(checkboxToken);
            }
        }
        return true;
    });
}

// Function to create container plugin configuration
function createContainerConfig(name: string, defaultTitle: string) {
    return {
        validate: (params: string) => {
            return params.trim().match(new RegExp(`^${name}\s*(.*)$`));
        },
        render: (tokens: any[], idx: number) => {
            const m = tokens[idx].info.trim().match(new RegExp(`^${name}\s*(.*)$`));
            if (tokens[idx].nesting === 1) {
                // opening tag
                const title = m && m[1] ? mdParser.utils.escapeHtml(m[1]) : defaultTitle;
                return `<div class="custom-container ${name}">
<p class="custom-container-title">${title}</p>
`;
            } else {
                // closing tag
                return '</div>\n';
            }
        }
    };
}

// Configure markdown-it instance
export const mdParser: MarkdownIt = new MarkdownIt({
    html: true, // Enable HTML tags in source
    linkify: true, // Autoconvert URL-like text to links
    typographer: true, // Enable some language-neutral replacement + quotes beautification

    highlight: function (str, lang) {
        if (lang && hljs.getLanguage(lang)) {
            try {
                return '<pre class="hljs"><code>' +
                       hljs.highlight(str, { language: lang, ignoreIllegals: true }).value +
                       '</code></pre>';
            } catch (__) {}
        }
        // Fallback for languages not supported by highlight.js, or for non-highlighted code blocks
        return '<pre class="hljs"><code>' + mdParser.utils.escapeHtml(str) + '</code></pre>';
    }
})
.use(texmath, {
    engine: katex,
    delimiters: 'dollars', // Default: 'dollars'
    katexOptions: { macros: { "\RR": "\mathbb{R}" } } // Example KaTeX options
})
.use(customTaskListPlugin)
.use(mdMark)
.use(mdFootnote)
.use(mdSub)
.use(mdSup)
.use(mdAbbr)
.use(mdBracketedSpans) // Use Bracketed Spans FIRST (conceptually handles [] syntax)
.use(mdAttrs, {
    leftDelimiters: '{',
    rightDelimiters: '}',
    allowedAttrs: ['class', 'style', /^data-.*$/] // Allow class, style, and data-* attributes
})
.use(mdContainer, 'tip', createContainerConfig('tip', 'TIP'))
.use(mdContainer, 'warning', createContainerConfig('warning', 'WARNING'))
.use(mdContainer, 'danger', createContainerConfig('danger', 'DANGER'))
.use(mdContainer, 'info', createContainerConfig('info', 'INFO'));

// Export CSS strings for injection into preview
export const katexCss = katexCssContent;
