import MarkdownIt from 'markdown-it';
import hljs from 'highlight.js';
import texmath from 'markdown-it-texmath';
import katex from 'katex';

// Import CSS content directly for offline usage (Vite feature)
import katexCssContent from 'katex/dist/katex.min.css?inline';
// highlightJsVsCssContent will be derived from CodeMirror's theme

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
});

// Export CSS strings for injection into preview
export const katexCss = katexCssContent;
