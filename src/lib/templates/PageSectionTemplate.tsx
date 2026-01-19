import { createElement, Fragment } from '@/lib/utils/jsx.ts';
import type { SectionConfig } from '@/lib/types/page';

export function PageSectionTemplate(config: SectionConfig) {
    const formatDate = (date: Date, format: string): string => {
        if (!format || format === 'default') {
            return date.toLocaleDateString();
        }

        const formatMap: Record<string, string> = {
            'iso': 'YYYY-MM-DD',
            'cn': 'YYYY年M月D日',
            'cn_long': 'YYYY年M月D日 dddd',
            'us': 'MM/DD/YYYY',
            'eu': 'DD/MM/YYYY'
        };

        const pattern = formatMap[format] || format;

        const replacements: Record<string, string | number> = {
            'YYYY': date.getFullYear(),
            'YY': String(date.getFullYear()).slice(-2),
            'MM': String(date.getMonth() + 1).padStart(2, '0'),
            'M': date.getMonth() + 1,
            'DD': String(date.getDate()).padStart(2, '0'),
            'D': date.getDate(),
            'HH': String(date.getHours()).padStart(2, '0'),
            'H': date.getHours(),
            'mm': String(date.getMinutes()).padStart(2, '0'),
            'm': date.getMinutes(),
            'ss': String(date.getSeconds()).padStart(2, '0'),
            's': date.getSeconds(),
            'dddd': date.toLocaleDateString(undefined, { weekday: 'long' }),
            'ddd': date.toLocaleDateString(undefined, { weekday: 'short' }),
        };

        return pattern.replace(/YYYY|YY|MM|M|DD|D|HH|H|mm|m|ss|s|dddd|ddd/g, (match) => {
            return String(replacements[match]);
        });
    };

    const processContent = (text: string) => {
        if (!text) return '';
        
        // Split by regex capturing the placeholder.
        // Matches: {p}, {p R}, {p r}, {p A}, {p a}
        // Matches: {d}, {d <custom format string>}
        // Note: The regex for {d ...} needs to be greedy enough to capture spaces in the format string.
        const parts = text.split(/(\{p(?: [RrAa])?\}|\{d(?: [^}]+)?\})/g);
        
        return parts.map((part) => {
            if (part.startsWith('{d')) {
                // Extract content: supports {d}, {d format}, {d "format"}, {d 'format'}
                // Handles multiple spaces and optional quotes.
                const match = part.match(/^\{d(?: +(?:"([^"]*)"|'([^']*)'|([^}]+)))?\}$/);
                
                let format = 'default';
                if (match) {
                    // match[1] is double quoted, [2] is single quoted, [3] is unquoted
                    format = (match[1] || match[2] || match[3] || 'default').trim();
                }
                
                return <span>{formatDate(new Date(), format)}</span>;
            }

            switch (part) {
                case '{p}': return <span class="page-num"></span>;
                case '{p R}': return <span class="page-num-upper-roman"></span>;
                case '{p r}': return <span class="page-num-lower-roman"></span>;
                case '{p A}': return <span class="page-num-upper-alpha"></span>;
                case '{p a}': return <span class="page-num-lower-alpha"></span>;
                default: return part;
            }
        });
    };

    return (
        <>
            <div class="section-left">
                <div class="content-outer">{processContent(config.outer)}</div>
                <div class="content-inner">{processContent(config.inner)}</div>
                <div class="content-left">{processContent(config.left)}</div>
            </div>
            <div class="section-center">{processContent(config.center)}</div>
            <div class="section-right">
                <div class="content-right">{processContent(config.right)}</div>
                <div class="content-inner">{processContent(config.inner)}</div>
                <div class="content-outer">{processContent(config.outer)}</div>
            </div>
        </>
    );
}
