import { evaluate, compile } from '@mdx-js/mdx';
import * as runtime from '@/lib/utils/jsx-runtime';
import { createElement } from '@/lib/utils/jsx'; 
import { TocPrintTemplate } from '@/lib/templates/toc/TocPrintTemplate';
import { TestComponent } from '@/lib/templates/toc/TestComponent';

// 定义我们支持的组件库
const components = {
    Toc: TocPrintTemplate, 
    TestComponent,
};

export async function renderMdx(content: string): Promise<string> {
    try {
        // Debug output (can be removed later)
        // const compiled = await compile(content, {
        //     baseUrl: import.meta.url,
        //     jsxRuntime: 'automatic',
        //     development: false,
        // });
        // console.log('MDX Compiled Code:', String(compiled));

        const { default: Content } = await evaluate(content, {
            ...runtime,
            // createElement, // Pass createElement if fallback needed, though automatic should use jsx
            baseUrl: import.meta.url,
        });

        // Content returns string in our custom runtime
        return Content({ components });
    } catch (error) {
        console.error('MDX Compilation Error:', error);
        return `<div class="mdx-error">MDX Error: ${String(error)}</div>`;
    }
}
