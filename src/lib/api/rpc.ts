import {PageLabelNumberingStyle} from "@/lib/styleMaps";

function convertForRust(bookmark: any): any {
    const copy = { ...bookmark };
    // Convert pageNum to integer
    if (copy.pageNum !== null && copy.pageNum !== undefined) {
        const num = parseInt(String(copy.pageNum), 10);
        copy.pageNum = isNaN(num) ? null : num;
    } else {
        copy.pageNum = null;
    }
    
    // Process children recursively
    if (copy.children && Array.isArray(copy.children)) {
        copy.children = copy.children.map((child: any) => convertForRust(child));
    }
    return copy;
}

function convertFromRust(bookmark: any): any {
    const copy = { ...bookmark };
    // Convert pageNum to string
    if (copy.pageNum !== null && copy.pageNum !== undefined) {
        copy.pageNum = String(copy.pageNum);
    } else {
        copy.pageNum = null;
    }

    if (copy.children && Array.isArray(copy.children)) {
        copy.children = copy.children.map((child: any) => convertFromRust(child));
    }
    return copy;
}

/**
 * RPC 请求结构
 */
export interface RpcRequest {
    id: string;
    method: string;
    params: any[];
}

/**
 * RPC 响应结构
 */
export interface RpcResponse {
    id: string;
    result: any;
    error: string | null;
}

// --- Data Models ---

export interface HeaderFooterConfig {
    left: string | null;
    center: string | null;
    right: string | null;
    inner: string | null;
    outer: string | null;
    drawLine: boolean;
}

// TocConfig removed. Use src/lib/api/rust_pdf.ts

export interface PageLabelRuleDto {
    fromPage: number;
    numberingStyle: PageLabelNumberingStyle;
    prefix: string;
    start: number;
}

/**
 * 定义 Java 端 ApiService 对应的方法
 */
export interface QuickOutlineApi {
    openFile(filePath: string): Promise<string>;
    getCurrentFilePath(): Promise<string>;
    
    // Outline
    getOutlineAsBookmark(srcFilePath: string, offset: number): Promise<any>;
    saveOutline(srcFilePath: string, bookmarkRoot: any, destFilePath: string | null, offset: number, viewMode?: string): Promise<string>; // Corrected signature

    // TOC
    // generateTocPage moved to rust_pdf.ts
    generateTocPreview(config: any): Promise<string>; // Returns JSON of ImagePageUpdate[]

    // Page Labels
    setPageLabels(rules: PageLabelRuleDto[], destFilePath: string | null): Promise<string>;
    simulatePageLabels(rules: PageLabelRuleDto[]): Promise<string[]>;


    // Sync Utils
    updateOffset(offset: number): Promise<void>;
}


class RpcClient implements QuickOutlineApi {
    private ws: WebSocket | null = null;
    private pendingRequests = new Map<string, { resolve: Function, reject: Function }>();
    private eventListeners = new Map<string, Function[]>(); // For unsolicited messages
    public port: number = 0; // Make port public
    private isAndroid: boolean = false;

    constructor() {
        // @ts-ignore
        this.isAndroid = typeof window['AndroidRpc'] !== 'undefined';
    }

    /**
     * Registers an event handler for unsolicited messages from the backend.
     * @param eventType The type of the event (e.g., "external-editor-update")
     * @param handler The callback function
     */
    public on(eventType: string, handler: Function) {
        if (!this.eventListeners.has(eventType)) {
            this.eventListeners.set(eventType, []);
        }
        this.eventListeners.get(eventType)?.push(handler);
    }

    /**
     * Removes an event handler.
     * @param eventType The type of the event
     * @param handler The callback function to remove
     */
    public off(eventType: string, handler: Function) {
        const handlers = this.eventListeners.get(eventType);
        if (handlers) {
            this.eventListeners.set(eventType, handlers.filter(h => h !== handler));
        }
    }

    private emit(eventType: string, payload: any) {
        this.eventListeners.get(eventType)?.forEach(handler => {
            try {
                handler(payload);
            } catch (e) {
                console.error(`RPC: Error in event handler for ${eventType}:`, e);
            }
        });
    }

    /**
     * 初始化连接
     * @param port Tauri 模式下 Java Sidecar 的端口（从 Rust 获取）
     */
    public async connect(port?: number): Promise<void> {
        if (this.isAndroid) {
            console.log("RPC: Running in Android mode");
            return Promise.resolve();
        }

        if (!port) {
            throw new Error("Port required for Tauri mode");
        }
        this.port = port; // Store the port

        return new Promise((resolve, reject) => {
            this.ws = new WebSocket(`ws://127.0.0.1:${port}/ws/tauri`);
            
            this.ws.onopen = () => {
                console.log("RPC: WebSocket connected");
                resolve();
            };

            this.ws.onerror = (event) => {
                console.error("RPC: WebSocket error", event);
                reject(new Error("WebSocket connection failed. The backend service might be unavailable."));
            };

            this.ws.onmessage = (event) => {
                try {
                    const message = JSON.parse(event.data);
                    
                    // Check if it's an RPC response
                    const handler = this.pendingRequests.get(message.id);
                    if (handler) {
                        if (message.error) {
                            console.error("RPC: Response error", message.error);
                            handler.reject(new Error(message.error));
                        } else {
                            handler.resolve(message.result);
                        }
                        this.pendingRequests.delete(message.id);
                    } else if (message.type) { // It's an unsolicited event from backend
                        console.log(`RPC: Received event [${message.type}]`, message.payload);
                        this.emit(message.type, message.payload);
                    } else {
                        console.warn("RPC: Unknown message format or no handler found", message);
                    }
                } catch (e) {
                    console.error("RPC: Failed to parse message or handle", e);
                }
            };
            
            this.ws.onclose = (event) => {
                 console.log("RPC: WebSocket closed", event);
                 // Emit a disconnection event
                 this.emit('rpc-disconnected', { 
                     reason: event.reason || "Connection closed", 
                     code: event.code 
                 });
            };
        });
    }

    private send(method: string, params: any[]): Promise<any> {
        const id = crypto.randomUUID();
        const request: RpcRequest = { id, method, params };
        const json = JSON.stringify(request);
        console.log(`RPC: Sending [${method}]`, json);

        if (this.isAndroid) {
            return new Promise((resolve, reject) => {
                try {
                    // @ts-ignore
                    const responseStr = window.AndroidRpc.handle(json);
                    const response: RpcResponse = JSON.parse(responseStr);
                    
                    if (response.error) {
                        reject(new Error(response.error));
                    } else {
                        resolve(response.result);
                    }
                } catch (e) {
                    reject(e);
                }
            });
        } else {
            // Tauri / WebSocket Mode
            return new Promise((resolve, reject) => {
                if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
                    return reject(new Error("WebSocket not connected"));
                }
                this.pendingRequests.set(id, { resolve, reject });
                this.ws.send(json);
            });
        }
    }

    // --- API 实现 ---

    public openFile(filePath: string): Promise<string> {
        return this.send("openFile", [filePath]);
    }

    public getCurrentFilePath(): Promise<string> {
        return this.send("getCurrentFilePath", []);
    }

    public getOutlineAsBookmark(srcFilePath: string, offset: number): Promise<any> {
        // When using Rust backend, we invoke the Tauri command
        return import("@tauri-apps/api/core").then(({ invoke }) => 
            invoke("get_outline_as_bookmark", { path: srcFilePath, offset })
        ).then(result => convertFromRust(result));
    }

    public saveOutline(srcFilePath: string, bookmarkRoot: any, destFilePath: string | null, offset: number, viewMode: string = 'NONE'): Promise<string> {
        const rustRoot = convertForRust(bookmarkRoot);
        return import("@tauri-apps/api/core").then(({ invoke }) => 
            invoke("save_outline", { 
                srcPath: srcFilePath, 
                bookmarkRoot: rustRoot, 
                destPath: destFilePath, 
                offset, 
                viewMode 
            })
        ).then(() => "Success");
    }

    public generateTocPreview(config: any): Promise<string> {
        return this.send("generateTocPreview", [config]);
    }


    public setPageLabels(rules: PageLabelRuleDto[], destFilePath: string | null): Promise<string> {
        return this.send("setPageLabels", [rules, destFilePath]);
    }

    public simulatePageLabels(rules: PageLabelRuleDto[]): Promise<string[]> {
        return this.send("simulatePageLabels", [rules]);
    }


    public updateOffset(offset: number): Promise<void> {
        return this.send("updateOffset", [offset]);
    }

    public openExternalEditor(textContent: string): Promise<void> {
        return this.send("openExternalEditor", [textContent]);
    }
}

export const rpc = new RpcClient();
