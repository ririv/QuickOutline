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
      receiveSuccess: (data: string) => void;
      receiveError: (error: string) => void;
      // Toc Tab specific
      previewToc: (json: string) => void;
      generateToc: (json: string) => void;
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

// --- JavaFX WebView Keyboard Event Polyfill ---
// 修复 JavaFX WebView 中 e.key 为空的问题
(function polyfillKeyboardEvent() {
    const keyMap: Record<number, string> = {
        8: 'Backspace', 9: 'Tab', 13: 'Enter', 27: 'Escape',
        32: ' ', 33: 'PageUp', 34: 'PageDown', 35: 'End', 36: 'Home',
        37: 'ArrowLeft', 38: 'ArrowUp', 39: 'ArrowRight', 40: 'ArrowDown',
        45: 'Insert', 46: 'Delete',
        189: '-', 187: '=', 219: '[', 221: ']', 220: '\\',
        186: ';', 222: "'", 188: ',', 190: '.', 191: '/'
    };

    window.addEventListener('keydown', (e) => {
        if (!e.key) {
            let key = keyMap[e.keyCode];
            
            // Fallback for alphanumeric keys (A-Z, 0-9)
            if (!key) {
                // A-Z
                if (e.keyCode >= 65 && e.keyCode <= 90) {
                    key = String.fromCharCode(e.keyCode);
                    if (!e.shiftKey) key = key.toLowerCase();
                } 
                // 0-9
                else if (e.keyCode >= 48 && e.keyCode <= 57) {
                    key = String.fromCharCode(e.keyCode);
                }
            }
            
            if (key) {
                try {
                    Object.defineProperty(e, 'key', { get: () => key });
                } catch (err) {
                    console.warn('[Polyfill] Failed to patch e.key', err);
                }
            }
        }
    }, true); // Capture phase
})();

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

// --- 兜底初始化 (Stubs) ---
// 防止 Java 在 Svelte 组件挂载前调用报错
(function initStubs() {
    if (!window.getPayloads) {
        window.getPayloads = async () => {
            console.warn('[Bridge Stub] getPayloads called before initialization.');
            return JSON.stringify({ html: '', styles: '' });
        };
    }
    if (!window.getContent) {
        window.getContent = () => '';
    }
    if (!window.initVditor) {
        window.initVditor = (md) => console.warn('[Bridge Stub] initVditor called early with:', md);
    }
    // 其他 void 函数不需要 stub，undefined 调用会报错，但通常这些是单向通知，Java 不会立即调用
    // 除非 Java 真的在加载完成后毫秒级调用 updateSvgPages
    if (!window.updateSvgPages) window.updateSvgPages = () => {};
    if (!window.updateImagePages) window.updateImagePages = () => {};
})();
