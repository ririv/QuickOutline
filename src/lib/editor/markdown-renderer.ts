import MarkdownIt from 'markdown-it';
import hljs from 'highlight.js';
import texmath from 'markdown-it-texmath';
import katex from 'katex';

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
            const match = content.match(/^\[([ xX])\] /);
            if (match) {
                const isChecked = match[1].toLowerCase() === 'x';
                
                // Add class to list item
                tokens[i - 2].attrJoin('class', 'task-list-item');

                // Remove the marker from the text
                firstChild.content = content.slice(match[0].length);

                // Create checkbox token
                const checkboxToken = new state.Token('html_inline', '', 0);
                checkboxToken.content = `<input type="checkbox" class="task-list-item-checkbox" ${isChecked ? 'checked' : ''} disabled> `;
                
                // Insert checkbox at the start of inline children
                children.unshift(checkboxToken);
            }
        }
        return true;
    });
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
.use(customTaskListPlugin);

// Export CSS strings for injection into preview
export const katexCss = katexCssContent;