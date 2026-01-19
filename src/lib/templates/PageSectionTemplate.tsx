import { createElement, Fragment } from '@/lib/utils/jsx.ts';
import type { SectionConfig } from '@/lib/types/page';

export function PageSectionTemplate(config: SectionConfig) {
    const formatDate = (date: Date, format: string, locale?: string): string => {
        if (!format || format === 'default') {
            return date.toLocaleDateString(locale);
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
            'dddd': date.toLocaleDateString(locale, { weekday: 'long' }),
            'ddd': date.toLocaleDateString(locale, { weekday: 'short' }),
        };

        return pattern.replace(/YYYY|YY|MM|M|DD|D|HH|H|mm|m|ss|s|dddd|ddd/g, (match) => {
            return String(replacements[match]);
        });
    };

    const processContent = (text: string) => {
        if (!text) return '';
        
        // Split by regex capturing the placeholder.
        const parts = text.split(/(\{p(?: [RrAa])?\}|\{d(?: [^}]+)?\})/g);
        
        return parts.map((part) => {
            if (part.startsWith('{d')) {
                // 1. Extract raw content inside {d ... }
                const rawMatch = part.match(/^\{d(?: +(.+))?\}$/);
                let content = rawMatch && rawMatch[1] ? rawMatch[1].trim() : '';
                
                // 2. Extract locale if present at the end (e.g. " zh-CN" or " en")
                // Looks for space + 2 letters + optional (- + 2-4 letters)
                let locale: string | undefined = undefined;
                const localeMatch = content.match(/\s+([a-zA-Z]{2}(?:-[a-zA-Z]{2,4})?)$/);
                
                if (localeMatch) {
                    locale = localeMatch[1];
                    content = content.substring(0, localeMatch.index).trim();
                }

                // 3. Extract format from the remaining content (handle quotes)
                let format = 'default';
                if (content) {
                    // Check for quotes
                    const quoteMatch = content.match(/^(?:"([^"]*)"|'([^']*)'|([^"']+))$/);
                    if (quoteMatch) {
                        format = quoteMatch[1] || quoteMatch[2] || quoteMatch[3];
                    }
                }
                
                return <span>{formatDate(new Date(), format, locale)}</span>;
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
