import { Previewer } from 'pagedjs';

interface PagedPayload {
    html: string;
    styles: string;
    header: any;
    footer: any;
}

let currentPreviewer: Previewer | null = null;
let isRendering = false;
let pendingPayload: PagedPayload | null = null;

// Double Buffering State
let bufferA: HTMLDivElement | null = null;
let bufferB: HTMLDivElement | null = null;
let activeBuffer: 'A' | 'B' = 'B'; // Start assuming B is hidden, so first render goes to A (flipped logic below)

export async function handlePagedUpdate(
    payload: PagedPayload,
    container: HTMLElement,
    onRenderComplete?: () => void
) {
    // 1. Queue handling (Simple Debounce/Lock)
    if (isRendering) {
        pendingPayload = payload;
        return;
    }

    isRendering = true;
    
    try {
        await renderToBuffer(payload, container);
        onRenderComplete?.();

        // Process queue
        while (pendingPayload) {
            const next = pendingPayload;
            pendingPayload = null;
            await renderToBuffer(next, container);
            onRenderComplete?.();
        }
    } catch (e) {
        console.error("Paged Engine Render Error:", e);
    } finally {
        isRendering = false;
    }
}

async function renderToBuffer(payload: PagedPayload, container: HTMLElement) {
    // Initialize buffers if needed
    if (!bufferA || !bufferB) {
        bufferA = createBuffer();
        bufferB = createBuffer();
        // Initially hide both
        bufferA.style.display = 'none';
        bufferB.style.display = 'none';
        container.appendChild(bufferA);
        container.appendChild(bufferB);
    }

    // Determine target buffer (Render to the HIDDEN one)
    // If active is A, we render to B.
    const targetBuffer = activeBuffer === 'A' ? bufferB : bufferA;
    const targetBufferName = activeBuffer === 'A' ? 'B' : 'A';

    // Clear target buffer
    targetBuffer!.innerHTML = '';
    // Ensure it's hidden but part of DOM for Paged.js to work (Paged.js needs DOM, but display:none might affect calculation?)
    // Paged.js actually needs to measure elements. display:none prevents measurement.
    // Strategy: Use visibility:hidden or position:absolute off-screen for calculation?
    // Paged.js usually attaches its own styles.
    // Let's try: Make it visible but z-index lower or absolute positioned?
    // Better: Keep it display:block but hidden via opacity or z-index during render?
    // Actually, for Double Buffering to work without flash, the new buffer must be laid out BEFORE we show it.
    // So we set logic: 
    // 1. activeBuffer is Visible.
    // 2. targetBuffer is processing (must be visible to DOM for layout, but hidden from user).
    
    targetBuffer!.style.opacity = '0';
    targetBuffer!.style.position = 'absolute';
    targetBuffer!.style.top = '0';
    targetBuffer!.style.left = '0';
    targetBuffer!.style.zIndex = '-1'; // Behind
    targetBuffer!.style.display = 'block'; // Must be block for layout

    // Prepare Content
    const { html, styles, header, footer } = payload;
    const pageCss = generatePageCss(header, footer);
    const fullCss = `${styles}\n${pageCss}`;
    
    const contentWithStyle = `
      <style>${fullCss}</style>
      <div class="markdown-body">
          ${html}
      </div>
    `;

    // Create new Previewer
    // Paged.js hooks attach to the previewer instance.
    const previewer = new Previewer();
    
    console.log('[PagedEngine] Starting preview...');
    // Render
    // We pass flow: [] to use default flow
    try {
        await previewer.preview(contentWithStyle, [], targetBuffer);
        console.log('[PagedEngine] Preview finished.');
    } catch (err) {
        console.error('[PagedEngine] Preview failed:', err);
        throw err;
    }

    // SWAP BUFFERS
    console.log('[PagedEngine] Swapping buffers. Showing:', targetBufferName);
    // 1. Hide old active
    const oldActive = activeBuffer === 'A' ? bufferA : bufferB;
    if (oldActive) {
        oldActive!.style.display = 'none';
    }

    // 2. Show new target
    targetBuffer!.style.opacity = '1';
    targetBuffer!.style.position = 'static'; // Restore flow
    targetBuffer!.style.zIndex = 'auto';
    targetBuffer!.style.display = 'block';

    // Update state
    activeBuffer = targetBufferName;
    currentPreviewer = previewer;
}

function createBuffer(): HTMLDivElement {
    const div = document.createElement('div');
    div.className = 'paged-buffer';
    div.style.width = '100%';
    // Paged.js pages are usually centered or fixed width, so width 100% is fine for wrapper
    return div;
}

function generatePageCss(header: any, footer: any) {
    const getContent = (val: string) => {
       if (!val) return '""';
       const parts = val.split('{p}');
       const escapedParts = parts.map(part => {
           if (part === '') return null;
           const escaped = part.replace(/\\/g, '\\\\').replace(/"/g, '\"');
           return `"${escaped}"`;
       });
       let cssContent = '';
       for (let i = 0; i < escapedParts.length; i++) {
           if (escapedParts[i]) cssContent += escapedParts[i];
           if (i < escapedParts.length - 1) {
               if (cssContent.length > 0) cssContent += ' ';
               cssContent += 'counter(page)';
               if (i < escapedParts.length - 1) cssContent += ' ';
           }
       }
       return cssContent || '""';
    };

    return `
      @page {
          size: A4;
          margin: 20mm;
          @top-left { content: ${getContent(header?.left)}; }
          @top-center { content: ${getContent(header?.center)}; }
          @top-right { content: ${getContent(header?.right)}; }
          @bottom-left { content: ${getContent(footer?.left)}; }
          @bottom-center { content: ${getContent(footer?.center)}; }
          @bottom-right { content: ${getContent(footer?.right)}; }
      }
    `;
}
