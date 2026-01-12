import { generatePageCss } from '@/lib/preview-engine/css-generator';
import { PageSectionTemplate } from '@/lib/templates/PageSectionTemplate';

// 定义输入消息类型
interface WorkerInput {
    id: string;
    header: any;
    footer: any;
    pageLayout?: any;
    hfLayout?: any;
    html: string;      // New input
    styles: string;    // New input
}

// 定义输出消息类型
interface WorkerOutput {
    type: 'success' | 'error';
    id: string;
    payload?: {
        pageCss: string;
        contentWithStyle: string; // New output replacing separate header/footer HTMLs
    };
    error?: string;
}

self.onmessage = (e: MessageEvent<WorkerInput>) => {
    const { id, header, footer, pageLayout, hfLayout, html, styles } = e.data;

    try {
        // 执行耗时的字符串生成和模板渲染
        const pageCss = generatePageCss(header, footer, pageLayout, hfLayout);
        const headerHtml = PageSectionTemplate(header);
        const footerHtml = PageSectionTemplate(footer);

        // 拼接最终的 HTML 字符串 (原本在主线程做的)
        const contentWithStyle = `
        <style>${styles}</style>
        <div class="print-header">${headerHtml}</div>
        <div class="print-footer">${footerHtml}</div>
        ${html}
        `;

        const response: WorkerOutput = {
            type: 'success',
            id,
            payload: {
                pageCss,
                contentWithStyle
            }
        };
        self.postMessage(response);
    } catch (err: any) {
        const response: WorkerOutput = {
            type: 'error',
            id,
            error: err.message || 'Unknown error in paged-prepare worker'
        };
        self.postMessage(response);
    }
};
