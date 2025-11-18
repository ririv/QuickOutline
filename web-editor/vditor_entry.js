import Vditor from 'vditor';
import 'vditor/dist/index.css';

let vditorInstance = null;

window.initVditor = function (initialMarkdown) {
  if (vditorInstance) {
    return vditorInstance;
  }
  const el = document.getElementById('vditor');
  if (!el) {
    console.error('[Vditor] container #vditor not found');
    return;
  }
  vditorInstance = new Vditor('vditor', {
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
      'table', 'upload', 'preview', 'outline', 'fullscreen'
    ],
    preview: {
      math: {
        engine: 'MathJax',
        mathJaxOptions: {
          loader: { load: ["output/svg"] }
        }
      }
    },
    after: () => {
      if (initialMarkdown) {
        vditorInstance.setValue(initialMarkdown);
      }
    },
  });
  return vditorInstance;
};

window.getContent = function () {
  if (!vditorInstance) return '';
  return vditorInstance.getValue();
};

window.setContent = function (markdown) {
  if (!vditorInstance) {
    window.initVditor(markdown || '');
  } else {
    vditorInstance.setValue(markdown || '');
  }
};

window.insertImageMarkdown = function (relativePath) {
  if (!vditorInstance) return;
  const path = relativePath || '';
  const current = vditorInstance.getValue() || '';
  const insert = `\n![](${path})\n`;
  vditorInstance.setValue(current + insert);
};


// 返回预览区域的 HTML，用于 Java 侧 HTML→PDF
 async function getContentHtml() {
  if (!vditorInstance) return Promise.resolve('');
  const mdText = vditorInstance.getValue();
  console.log('[Vditor] mdText', mdText);

  const element = document.createElement('div');
  element.setAttribute('id', 'preview');

  element.style.position = 'absolute';
  element.style.top = '-9999px';
  element.style.left = '-9999px';
  element.style.visibility = 'hidden';
  document.body.appendChild(element);


  try {
    await Vditor.preview(element, mdText, {
      hljs: true,
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

    const mjxContainers = element.querySelectorAll('mjx-container')
    if (mjxContainers.length > 0) {
      for (const mjxContainer of mjxContainers) {
              const svgElements = mjxContainer.querySelectorAll('svg')
      console.log('[Vditor] allSvgParagraphs', svgElements);
      const svgElement = svgElements[0];
      const preciseHeight = svgElement.getBoundingClientRect().height;
      const preciseWidth = svgElement.getBoundingClientRect().width;
      console.log('[Vditor] svgElement', svgElement);
      svgElement.setAttribute('width', preciseWidth + 'px');
      svgElement.setAttribute('height', preciseHeight + 'px');
      }
    }
    return element.innerHTML;
  } catch (e) {
    console.warn('[Vditor] getHTML failed', e);
    return '';
  }
};


window.getContentHtml = getContentHtml;

function getMathJaxStyles() {
  return document.getElementById('MJX-SVG-styles')?.textContent || '';
}

window.getPayloads = async function () {
  try {
    const html = await getContentHtml();
    const css = getMathJaxStyles();
    console.log('[Vditor] getHtmlAndStyles completed.');
    return JSON.stringify({ html: html, css: css });
  } catch (e) {
    console.warn('[Vditor] getHtmlAndStyles failed', e);
    return JSON.stringify({ html: '', css: '' });
  }
};