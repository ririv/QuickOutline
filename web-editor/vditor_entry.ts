import Vditor from 'vditor';
import 'vditor/dist/index.css';

// Define a custom interface for the Window object to add Vditor-related functions
declare global {
  interface Window {
    initVditor: (initialMarkdown: string) => Vditor | undefined;
    getContent: () => string;
    setContent: (markdown: string) => void;
    insertImageMarkdown: (relativePath: string) => void;
    getContentHtml: () => Promise<string>;
    getMathJaxStyles: () => string;
    getPayloads: () => Promise<string>;
  }
}

let vditorInstance: Vditor | null = null;

window.initVditor = function (initialMarkdown: string): Vditor | undefined {
  if (vditorInstance) {
    return vditorInstance;
  }
  const el = document.getElementById('vditor');
  if (!el) {
    console.error('[Vditor] container #vditor not found');
    return;
  }
  vditorInstance = new Vditor('vditor', {
    cdn: './vditor',
    height: '100%',
    width: '100%',
    mode: 'sv',
    lang: 'zh_CN',
    placeholder: '在这里输入 Markdown ...',
    cache: { enable: false },
    toolbarConfig: { pin: true },
    toolbar: [
      'headings', 'bold', 'italic', 'strike', 'link', '|',
      'list', 'ordered-list', 'check', '|',
      'quote', 'code', 'inline-code', 'code-theme', '|',
      'table', 'upload', 'preview', 'outline', 'fullescreen'
    ],
    preview: {
      math: {
        engine: 'MathJax',
        mathJaxOptions: {
          loader: { load: ["output/svg"] },
          options: {
            enableAssistiveMml: false
          }
        }
      }
    },
    after: () => {
      if (initialMarkdown) {
        vditorInstance?.setValue(initialMarkdown);
      }
    },
  });
  return vditorInstance;
};

window.getContent = function (): string {
  if (!vditorInstance) return '';
  return vditorInstance.getValue();
};

window.setContent = function (markdown: string): void {
  if (!vditorInstance) {
    window.initVditor(markdown || '');
  } else {
    vditorInstance.setValue(markdown || '');
  }
};



window.insertImageMarkdown = function (relativePath: string): void {
  if (!vditorInstance) return;
  const path = relativePath || '';
  const current = vditorInstance.getValue() || '';
  const insert = `\n![](${path})\n`;
  vditorInstance.setValue(current + insert);
};


// 返回预览区域的 HTML，用于 Java 侧 HTML→PDF
window.getContentHtml = async function (): Promise<string> {
  if (!vditorInstance) return Promise.resolve('');
  const mdText = vditorInstance.getValue();
  console.log('[Vditor] mdText', mdText);

  const element = document.createElement('div');
  element.setAttribute('id', 'preview');

  try {
    await Vditor.preview(element, mdText, {
      mode: "light",
      cdn: './vditor',
      math: {
        engine: 'MathJax',
        mathJaxOptions: {
          loader: { load: ["output/svg"] },
          options: {
            enableAssistiveMml: false
          }
        }
      }
    });
    await new Promise(resolve => setTimeout(resolve, 0));
    console.log(element);

    // resetMathjaxSvgActualSize(element);

    return element.innerHTML;
  } catch (e) {
    console.warn('[Vditor] getHTML failed', e);
    return '';
  }
};


let resetMathjaxSvgActualSize = function (element: HTMLElement): void {
  element.style.position = 'absolute';
  element.style.top = '-9999px';
  element.style.left = '-9999px';
  element.style.visibility = 'hidden';
  document.body.appendChild(element);

  const mjxContainers = element.querySelectorAll('mjx-container');
  if (mjxContainers.length > 0) {
    for (const mjxContainer of mjxContainers) {
      const svgElements = mjxContainer.querySelectorAll('svg');
      console.log('[Vditor] allSvgParagraphs', svgElements);
      const svgElement = svgElements[0];
      if (svgElement) { // Ensure svgElement exists
        console.log('[Vditor] svgElement', svgElement);

        const rect = svgElement.getBoundingClientRect();
        const computedStyle = window.getComputedStyle(svgElement);

        // 拿到数据

        // 没有带单位，但单位是px，需要补上 px
        const rectWidth = rect.width + 'px';
        const rectHeight = rect.height + 'px';

        // 带了 px 单位
        const cssWidth = computedStyle.width
        const cssHeight = computedStyle.height
        if (cssWidth && cssWidth.includes('px')) {
          svgElement.setAttribute('width', cssWidth);
          svgElement.style.width = cssWidth; // 双重保险
        }

        if (cssHeight && cssHeight.includes('px')) {
          svgElement.setAttribute('height', cssHeight);
          svgElement.style.height = cssHeight;
        }
      }

    }
  }
  document.body.removeChild(element); // Clean up
}


window.getMathJaxStyles = function (): string {
  return document.getElementById('MJX-SVG-styles')?.textContent || '';
};

window.getPayloads = async function (): Promise<string> {
  try {
    const html = await window.getContentHtml();
    const styles = window.getMathJaxStyles();
    console.log('[Vditor] getPayloads completed.');
    return JSON.stringify({ html: html, styles: styles });
  } catch (e) {
    console.warn('[Vditor] getPayloads failed', e);
    return JSON.stringify({ html: '', styles: '' });
  }
};


window.initVditor('');