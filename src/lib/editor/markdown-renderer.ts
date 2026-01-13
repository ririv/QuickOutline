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
import mdBracketedSpans from 'markdown-it-bracketed-spans';

// No longer importing katexCssContent here as it's globally imported via widgets.ts

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
            const match = content.match(/^\[([ xX])\] /);
            if (match) {
                const isChecked = match[1].toLowerCase() === 'x';

                tokens[i - 2].attrJoin('class', 'task-list-item');

                firstChild.content = content.slice(match[0].length);

                const checkboxToken = new state.Token('html_inline', '', 0);
                checkboxToken.content = `<span class="custom-checkbox ${isChecked ? 'checked' : ''}"></span> `;

                children.unshift(checkboxToken);
            }
        }
        return true;
    });
}

// Helper function to create container configurations with titles (e.g., Tip, Warning)
function createContainerConfig(md: MarkdownIt, name: string, defaultTitle: string) {
    return {
        validate: (params: string) => params.trim().match(new RegExp(`^${name}\\s*(.*)$`)),
        render: (tokens: any[], idx: number) => {
            const m = tokens[idx].info.trim().match(new RegExp(`^${name}\\s*(.*)$`));
            if (tokens[idx].nesting === 1) {
                const title = m && m[1] ? md.utils.escapeHtml(m[1]) : defaultTitle;
                return `<div class="custom-container ${name}">\n<p class="custom-container-title">${title}</p>\n`;
            } else {
                return '</div>\n';
            }
        }
    };
}

// Helper function to create simple layout containers (div with classes)
// This is not strictly needed anymore since the generic 'div' container covers this
/*
function createSimpleContainer(md: MarkdownIt, name: string, classes: string) {
    return {
        validate: (params: string) => params.trim().match(new RegExp(`^${name}\\s*(.*)$`)),
        render: (tokens: any[], idx: number) => {
            if (tokens[idx].nesting === 1) {
                return `<div class="${classes}">\n`;
            } else {
                return '</div>\n';
            }
        }
    };
}
*/

export interface MarkdownParserOptions {
    enableIndentedCodeBlocks?: boolean;
}

export function createMdParser(options: MarkdownParserOptions = {}): MarkdownIt {
    const md: MarkdownIt = new MarkdownIt({
        html: true,
        linkify: true,
        typographer: true,
        highlight: function (str, lang) {
            if (lang && hljs.getLanguage(lang)) {
                try {
                    return '<pre class="hljs"><code>' +
                        hljs.highlight(str, { language: lang, ignoreIllegals: true }).value +
                        '</code></pre>';
                } catch (__) {}
            }
            // Use the current md instance's utils for escaping
            return '<pre class="hljs"><code>' + md.utils.escapeHtml(str) + '</code></pre>';
        }
    });

    if (!options.enableIndentedCodeBlocks) {
        md.disable('code');
    }

    return md
        .use(texmath, {
            engine: katex,
            delimiters: 'dollars',
            // katexOptions: { macros: { "\\RR": "\\mathbb{R}" } }
        })
        .use(customTaskListPlugin)
        .use(mdMark)
        .use(mdFootnote)
        .use(mdSub)
        .use(mdSup)
        .use(mdAbbr)
        .use(mdBracketedSpans)
        .use(mdAttrs, {
            leftDelimiters: '{',
            rightDelimiters: '}',
            allowedAttrs: ['class', 'style', /^data-.*$/]
        })
        .use(mdContainer, 'tip', createContainerConfig(md, 'tip', 'TIP'))
        .use(mdContainer, 'warning', createContainerConfig(md, 'warning', 'WARNING'))
        .use(mdContainer, 'danger', createContainerConfig(md, 'danger', 'DANGER'))
        .use(mdContainer, 'info', createContainerConfig(md, 'info', 'INFO'))
        // Generic Div Container for arbitrary Tailwind classes
        // Usage: ::: div grid grid-cols-2 gap-4
        .use(mdContainer, 'div', {
            validate: (params: string) => params.trim().match(/^div\s+(.*)$/),
            render: (tokens: any[], idx: number) => {
                const m = tokens[idx].info.trim().match(/^div\s+(.*)$/);
                if (tokens[idx].nesting === 1) {
                    // Use the current md instance's utils for escaping
                    const classes = (m && m[1]) ? md.utils.escapeHtml(m[1]) : '';
                    return `<div class="${classes}">\n`;
                } else {
                    return '</div>\n';
                }
            }
        });
}

// No longer exporting katexCss as it's globally imported

