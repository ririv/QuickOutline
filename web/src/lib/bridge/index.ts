// --- 全局类型定义 ---
declare global {
  interface Window {
    // Java -> JS (Preview)
    updateSvgPages: (jsonString: string) => void;
    updateImagePages: (jsonString: string) => void;
    setSvgDoubleBuffering: (enable: boolean) => void;

    // Java -> JS (Editor)
    insertContent: (text: string) => void;
    initVditor: (initialMarkdown: string) => any; // Vditor type is complex, using any for bridge
    getContent: () => string;
    setContent: (markdown: string) => void;
    insertImageMarkdown: (relativePath: string) => void;
    getContentHtml: () => Promise<string>;
    getMathJaxStyles: () => string;
    getPayloads: () => Promise<string>;
    
    // Bridge Objects injected by Java
    javaBridge?: {
      // Define methods if you call Java from JS directly via this object
      // e.g., log: (msg: string) => void;
    };
    debugBridge?: any;
  }
}

export type BridgeHandlers = {
  onUpdateSvg?: (json: string) => void;
  onUpdateImage?: (json: string) => void;
  onSetSvgDoubleBuffering?: (enable: boolean) => void;
  
  onInsertContent?: (text: string) => void;
  onInitVditor?: (md: string) => void;
  onGetContent?: () => string;
  onSetContent?: (md: string) => void;
  onInsertImageMarkdown?: (path: string) => void;
  onGetContentHtml?: () => Promise<string>;
  onGetPayloads?: () => Promise<string>;
};

/**
 * 初始化桥接：将全局 Window 方法路由到具体的组件逻辑
 */
export function initBridge(handlers: BridgeHandlers) {
  console.log('[Bridge] Initializing...');

  if (handlers.onUpdateSvg) {
    window.updateSvgPages = handlers.onUpdateSvg;
  }
  if (handlers.onUpdateImage) {
    window.updateImagePages = handlers.onUpdateImage;
  }
  if (handlers.onSetSvgDoubleBuffering) {
    window.setSvgDoubleBuffering = handlers.onSetSvgDoubleBuffering;
  }

  if (handlers.onInsertContent) {
    window.insertContent = handlers.onInsertContent;
  }
  if (handlers.onInitVditor) {
    // Vditor init might return the instance, but for bridge we usually just trigger it
    window.initVditor = (md) => {
        handlers.onInitVditor!(md);
        return undefined; // Return value usually not used by Java in this async way
    };
  }
  if (handlers.onGetContent) {
    window.getContent = handlers.onGetContent;
  }
  if (handlers.onSetContent) {
    window.setContent = handlers.onSetContent;
  }
  if (handlers.onInsertImageMarkdown) {
    window.insertImageMarkdown = handlers.onInsertImageMarkdown;
  }
  if (handlers.onGetContentHtml) {
    window.getContentHtml = handlers.onGetContentHtml;
  }
  
  // GetPayloads combines HTML and Styles
  if (handlers.onGetPayloads) {
    window.getPayloads = handlers.onGetPayloads;
  }
  
  // Helper for MathJax styles (usually global)
  window.getMathJaxStyles = () => {
      return document.getElementById('MJX-SVG-styles')?.textContent || '';
  };

  console.log('[Bridge] Initialized.');
}
