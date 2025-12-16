// src/lib/preview-engine/PrintTemplate.tsx

import { createElement, Fragment } from '@/lib/utils/jsx';

interface PrintTemplateProps {
    styles: string;
    pageCss: string;
    headerHtml: string;
    footerHtml: string;
    tocHtml: string;
}

export function PrintTemplate({ styles, pageCss, headerHtml, footerHtml, tocHtml }: PrintTemplateProps) {
    return (
        <html>
            <head>
                <meta charset="UTF-8" />
                <title>Table of Contents</title>
                <style>{styles}</style>
                <style>{pageCss}</style>
                <script src="libs/paged.polyfill.min.js"></script>
            </head>
            <body class="markdown-body">
                <div class="print-header">{headerHtml}</div>
                <div class="print-footer">{footerHtml}</div>
                {tocHtml}
            </body>
        </html>
    );
}
