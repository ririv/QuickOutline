import { listen } from '@tauri-apps/api/event';

type DropHandler = (paths: string[]) => void;

class DragDropManager {
    private static instance: DragDropManager;
    private currentTarget: HTMLElement | null = null;
    private unlisten: (() => void) | null = null;
    
    private constructor() {
        this.init();
    }

    public static getInstance(): DragDropManager {
        if (!DragDropManager.instance) {
            DragDropManager.instance = new DragDropManager();
        }
        return DragDropManager.instance;
    }

    private async init() {
        // @ts-ignore
        if (window.__TAURI__) {
            try {
                this.unlisten = await listen<{ paths: string[] }>('tauri://file-drop', (event) => {
                    console.log("[DragDropManager] Global Drop Event:", event);
                    if (this.currentTarget) {
                        console.log("[DragDropManager] Target found:", this.currentTarget);
                        const handler = (this.currentTarget as any).__dropHandler as DropHandler;
                        if (handler) {
                            handler(event.payload.paths);
                        }
                    } else {
                        console.log("[DragDropManager] No active target found.");
                    }
                });
                
                // Also listen for cancellation to clear state
                await listen('tauri://file-drop-cancelled', () => {
                    this.currentTarget = null;
                });

            } catch (e) {
                console.error("[DragDropManager] Failed to init:", e);
            }
        }
    }

    public register(element: HTMLElement, handler: DropHandler) {
        (element as any).__dropHandler = handler;
        
        // Use standard events to track "active" target
        element.addEventListener('dragenter', (e) => {
            // e.preventDefault(); // Don't prevent default here, let it bubble if needed
            this.currentTarget = element;
            console.log("[DragDropManager] Entered:", element);
        });

        element.addEventListener('dragover', (e) => {
             e.preventDefault(); // Necessary to allow drop visually
             if (this.currentTarget !== element) {
                 this.currentTarget = element;
             }
        });

        element.addEventListener('dragleave', (e) => {
            if (this.currentTarget === element) {
                // verify we really left (not just entered child)
                const rect = element.getBoundingClientRect();
                const x = e.clientX;
                const y = e.clientY;
                if (x < rect.left || x >= rect.right || y < rect.top || y >= rect.bottom) {
                    this.currentTarget = null;
                    console.log("[DragDropManager] Left:", element);
                }
            }
        });
    }

    public cleanup() {
        if (this.unlisten) this.unlisten();
    }
}

export const dragDropManager = DragDropManager.getInstance();
