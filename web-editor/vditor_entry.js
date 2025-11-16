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
      'headings','bold','italic','strike','link','|',
      'list','ordered-list','check','|',
      'quote','code','inline-code','code-theme','|',
      'table','upload','preview','outline','fullscreen'
    ],
    input: (value) => {
      // 当前架构下，Java 侧轮询 window.getContent()
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
window.getHtml = function () {
  if (!vditorInstance) return '';
  // 在 sv 模式下，Vditor 内部会维护预览 HTML
  try {
    return vditorInstance.getHTML();
  } catch (e) {
    console.warn('[Vditor] getHTML failed', e);
    return '';
  }
};
