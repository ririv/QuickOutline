import { createElement, Fragment } from '@/lib/utils/jsx.ts';
import type { SectionConfig } from '@/lib/types/page';

export function PageSectionTemplate(config: SectionConfig) {
    const processContent = (text: string) => {
        if (!text) return '';
        
        // Split by regex capturing the placeholder to keep it in the array
        // Matches: {p}, {p R}, {p r}, {p A}, {p a}
        const parts = text.split(/(\{p(?: [RrAa])?\})/g);
        
        return parts.map((part) => {
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
