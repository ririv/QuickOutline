import { createElement, Fragment } from '@/lib/utils/jsx.ts';

interface MarkdownPrintTemplateProps {
    styles: string;
    pageCss: string;
    headerHtml: string;
    footerHtml: string;
    contentHtml: string;
    baseUrl: string;
}

export function MarkdownPrintTemplate({ 
    styles, 
    pageCss, 
    headerHtml, 
    footerHtml, 
    contentHtml,
    baseUrl
}: MarkdownPrintTemplateProps) {
    return (
        <html>
            <head>
                <base href={baseUrl} />
                <meta charset="UTF-8" />
                <link rel="stylesheet" href="libs/katex.min.css" />
                <style>{styles}</style>
                <style>{pageCss}</style>
                {/* Inject Paged.js for consistent pagination layout */}
                <script src="libs/paged.polyfill.min.js"></script>
                {/* Inject UnoCSS Runtime */}
                <script src="libs/unocss-runtime.bundle.js"></script>
            </head>
            <body class="markdown-body">
                <div class="print-header">{headerHtml}</div>
                <div class="print-footer">{footerHtml}</div>
                {contentHtml}
            </body>
        </html>
    );
}
