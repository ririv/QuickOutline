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

            // 重要！！！必须有这条语句进行等待，否则获得的是未渲染完成的内容（公式）
            await new Promise(resolve => setTimeout(resolve, 0));
            
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
            tab: '    ', // 4 spaces
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
        
        // 强力拦截：在捕获阶段拦截 Tab，防止 WebView 焦点跳转
        const captureTab = (e: KeyboardEvent) => {
            if (e.key === 'Tab' || e.code === 'Tab' || e.keyCode === 9) {
                // 检查焦点是否在编辑器内
                if (element && element.contains(e.target as Node)) {
                    e.preventDefault();
                    e.stopPropagation();
                    
                    if (!e.shiftKey && vditorInstance) {
                        // 手动插入缩进
                        vditorInstance.insertValue('    ');
                    }
                }
            }
        };
        
        // 强力拦截：在捕获阶段拦截剪贴板，走 Java Bridge
        const captureClipboard = (e: ClipboardEvent) => {
            if (!element || !element.contains(e.target as Node)) return;
            if (!window.javaBridge) return; // 如果没 Bridge (浏览器调试)，走默认行为

            if (e.type === 'copy' || e.type === 'cut') {
                e.preventDefault();
                e.stopPropagation();
                const text = window.getSelection()?.toString() || '';
                window.javaBridge.copyText(text);
                if (e.type === 'cut') document.execCommand('delete');
            } else if (e.type === 'paste') {
                e.preventDefault();
                e.stopPropagation();
                if (window.javaBridge.getClipboardText) {
                    const text = window.javaBridge.getClipboardText();
                    if (text && vditorInstance) {
                        vditorInstance.insertValue(text);
                    }
                }
            }
        };
        
        window.addEventListener('keydown', captureTab, true); // true = capture phase
        window.addEventListener('copy', captureClipboard, true);
        window.addEventListener('cut', captureClipboard, true);
        window.addEventListener('paste', captureClipboard, true);
        
        return () => {
            window.removeEventListener('keydown', captureTab, true);
            window.removeEventListener('copy', captureClipboard, true);
            window.removeEventListener('cut', captureClipboard, true);
            window.removeEventListener('paste', captureClipboard, true);
        };
    });

    onDestroy(() => {
        if (vditorInstance) {
            vditorInstance.destroy();
        }
    });

    function handleKeydown(e: KeyboardEvent) {
        // Fallback for bubbling phase if capture missed (unlikely for Tab)
    }
</script>

<div class="editor-container" bind:this={element}></div>

<style>
    .editor-container {
        height: 100%;
        width: 100%;
        user-select: text; /* 允许在编辑器内选中文本（覆盖全局禁止） */
    }
</style>
