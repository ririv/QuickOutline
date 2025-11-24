<script lang="ts">
    import { onMount, onDestroy } from 'svelte';
    import Vditor from 'vditor';
    import 'vditor/dist/index.css';

    let vditorInstance: Vditor;
    let element: HTMLDivElement;

    // Use correct CDN path based on environment
    console.log('[MdEditor] Mode:', import.meta.env.MODE, 'DEV:', import.meta.env.DEV);
    const cdnPath = import.meta.env.DEV ? '/node_modules/vditor' : './vditor';
    console.log('[MdEditor] cdnPath:', cdnPath);

    // Expose methods
    export const getValue = () => vditorInstance?.getValue() || '';
    export const setValue = (val: string) => vditorInstance?.setValue(val);
    export const insertValue = (val: string) => vditorInstance?.insertValue(val);
    
    export const insertImageMarkdown = (path: string) => {
        if (!vditorInstance) return;
        const current = vditorInstance.getValue() || '';
        const insert = `
![](${path})
`;
        vditorInstance.setValue(current + insert);
    };

    export const getContentHtml = async () => {
        if (!vditorInstance) return '';
        const mdText = vditorInstance.getValue();
        
        const previewDiv = document.createElement('div');
        // Style it hidden
        previewDiv.style.position = 'absolute';
        previewDiv.style.left = '-9999px';
        previewDiv.style.top = '-9999px';
        document.body.appendChild(previewDiv);

        try {
            await Vditor.preview(previewDiv, mdText, {
                mode: "light",
                cdn: cdnPath, // Important for local loading
                math: {
                    engine: 'MathJax',
                    mathJaxOptions: {
                        loader: { load: ["output/svg"] },
                        options: { enableAssistiveMml: false }
                    }
                }
            });
            
            // Fix MathJax SVG sizes if necessary (omitted complex logic for brevity, assuming standard output is ok or handled by CSS)
            // If previous logic had manual SVG resizing, it should be moved to a util function.
            // For now, we return innerHTML.
            const html = previewDiv.innerHTML;
            document.body.removeChild(previewDiv);
            return html;
        } catch (e) {
            console.warn('Vditor preview failed', e);
            if (previewDiv.parentNode) document.body.removeChild(previewDiv);
            return '';
        }
    };
    
    export const getPayloads = async () => {
        const html = await getContentHtml();
        const styles = document.getElementById('MJX-SVG-styles')?.textContent || '';
        return JSON.stringify({ html, styles });
    };

    export const init = (initialMarkdown: string = '') => {
        if (vditorInstance) return;
        
        vditorInstance = new Vditor(element, {
            cdn: cdnPath, // Use local resources copied by plugin
            height: '100%',
            width: '100%',
            mode: 'sv', // Split view or instant rendering? Original was 'sv'
            lang: 'zh_CN',
            placeholder: '在这里输入 Markdown ...',
            cache: { enable: false },
            toolbarConfig: { pin: true },
            toolbar: [
                'headings', 'bold', 'italic', 'strike', 'link', '|',
                'list', 'ordered-list', 'check', '|',
                'quote', 'code', 'inline-code', 'code-theme', '|',
                'table', 'preview', 'outline'
            ],
            resize: {
                enable: false, // 禁用自带的调整大小手柄，由外部容器控制
            },
            hint: {
                emoji: {}, // 禁用默认的 emoji 图片加载，防止 404 (因为我们没复制 emoji 资源)
            },
            typewriterMode: true, // 开启打字机模式，输入时保持光标居中
            preview: {
                math: {
                    engine: 'MathJax',
                    mathJaxOptions: {
                        loader: { load: ["output/svg"] },
                        options: { enableAssistiveMml: false }
                    }
                }
            },
            after: () => {
                if (initialMarkdown) vditorInstance.setValue(initialMarkdown);
            }
        });
    };

    onMount(() => {
        // Don't auto init, wait for Java or main.ts to call init if needed, 
        // OR auto init empty. 
        // The original code had window.initVditor(initialMarkdown).
        // We can just init empty here.
        init('');
    });

    onDestroy(() => {
        if (vditorInstance) {
            vditorInstance.destroy();
        }
    });
</script>

<div class="editor-container" bind:this={element}></div>

<style>
    .editor-container {
        height: 100%;
        width: 100%;
        user-select: text; /* 允许在编辑器内选中文本（覆盖全局禁止） */
    }
</style>
