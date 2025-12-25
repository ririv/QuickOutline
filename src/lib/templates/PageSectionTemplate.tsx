import { createElement, Fragment } from '@/lib/utils/jsx.ts';

export function PageSectionTemplate(config: any) {
    const processContent = (text: string) => {
        if (!text) return '';
        // Replace {p} with span for CSS counter injection
        // Using replaceAll or regex with g flag
        return text.replace(/\{p\}/g, '<span class="page-num"></span>');
    };

    return (
        <Fragment>
            <div class="section-left">{processContent(config.left)}</div>
            <div class="section-center">{processContent(config.center)}</div>
            <div class="section-right">{processContent(config.right)}</div>
        </Fragment>
    );
}
