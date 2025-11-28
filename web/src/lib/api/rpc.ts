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

export enum PageLabelNumberingStyle {
    DECIMAL_ARABIC_NUMERALS = "DECIMAL_ARABIC_NUMERALS",
    UPPERCASE_ROMAN_NUMERALS = "UPPERCASE_ROMAN_NUMERALS",
    LOWERCASE_ROMAN_NUMERALS = "LOWERCASE_ROMAN_NUMERALS",
    UPPERCASE_LETTERS = "UPPERCASE_LETTERS",
    LOWERCASE_LETTERS = "LOWERCASE_LETTERS",
    NONE = "NONE"
}

export interface SectionConfig {
    left: string | null;
    center: string | null;
    right: string | null;
    inner: string | null;
    outer: string | null;
    drawLine: boolean;
}

export interface TocConfig {
    tocContent: string;
    title: string;
    insertPos: number;
    style: PageLabelNumberingStyle;
    header: SectionConfig | null;
    footer: SectionConfig | null;
}

export interface PageLabelRule {
    fromPage: number;
    style: PageLabelNumberingStyle;
    prefix: string;
    start: number;
}

/**
 * 定义 Java 端 QuickOutlineApiService 对应的方法
 */
export interface QuickOutlineApi {
    openFile(filePath: string): Promise<string>;
    getCurrentFilePath(): Promise<string>;
    
    // Outline
    getOutline(offset: number): Promise<string>;
    getOutlineAsBookmark(offset: number): Promise<any>;
    // bookmarkRoot 是 JSON 对象, destFilePath 可选
    saveOutline(bookmarkRoot: any, destFilePath: string | null, offset: number): Promise<string>;
    autoFormat(text: string): Promise<string>;

    // TOC
    generateTocPage(config: TocConfig, destFilePath: string | null): Promise<string>;
    generateTocPreview(config: TocConfig): Promise<string>; // Returns Base64 string

    // Page Labels
    getPageLabels(srcFilePath: string | null): Promise<string[]>;
    setPageLabels(rules: PageLabelRule[], destFilePath: string | null): Promise<string>;
    simulatePageLabels(rules: PageLabelRule[]): Promise<string[]>;
}

class RpcClient implements QuickOutlineApi {
    private ws: WebSocket | null = null;
    private pendingRequests = new Map<string, { resolve: Function, reject: Function }>();
    private port: number = 0;
    private isAndroid: boolean = false;

    constructor() {
        // @ts-ignore
        this.isAndroid = typeof window['AndroidRpc'] !== 'undefined';
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
        this.port = port;

        return new Promise((resolve, reject) => {
            this.ws = new WebSocket(`ws://127.0.0.1:${port}/ws/tauri`);
            
            this.ws.onopen = () => {
                console.log("RPC: WebSocket connected");
                resolve();
            };

            this.ws.onerror = (err) => {
                console.error("RPC: WebSocket error", err);
                reject(err);
            };

            this.ws.onmessage = (event) => {
                try {
                    const response: RpcResponse = JSON.parse(event.data);
                    const handler = this.pendingRequests.get(response.id);
                    if (handler) {
                        if (response.error) {
                            handler.reject(new Error(response.error));
                        } else {
                            handler.resolve(response.result);
                        }
                        this.pendingRequests.delete(response.id);
                    }
                } catch (e) {
                    console.error("RPC: Failed to parse response", e);
                }
            };
            
            this.ws.onclose = () => {
                 console.log("RPC: WebSocket closed");
            };
        });
    }

    private send(method: string, params: any[]): Promise<any> {
        const id = crypto.randomUUID();
        const request: RpcRequest = { id, method, params };
        const json = JSON.stringify(request);

        if (this.isAndroid) {
            return new Promise((resolve, reject) => {
                try {
                    // @ts-ignore
                    // 假设 Android 端注入的对象名为 AndroidRpc，方法名为 handle
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

    public getOutline(offset: number): Promise<string> {
        return this.send("getOutline", [offset]);
    }

    public getOutlineAsBookmark(offset: number): Promise<any> {
        return this.send("getOutlineAsBookmark", [offset]);
    }

    public saveOutline(bookmarkRoot: any, destFilePath: string | null, offset: number): Promise<string> {
        return this.send("saveOutline", [bookmarkRoot, destFilePath, offset]);
    }

    public autoFormat(text: string): Promise<string> {
        return this.send("autoFormat", [text]);
    }

    public generateTocPage(config: TocConfig, destFilePath: string | null): Promise<string> {
        return this.send("generateTocPage", [config, destFilePath]);
    }

    public generateTocPreview(config: TocConfig): Promise<string> {
        return this.send("generateTocPreview", [config]);
    }

    public getPageLabels(srcFilePath: string | null): Promise<string[]> {
        return this.send("getPageLabels", [srcFilePath]);
    }

    public setPageLabels(rules: PageLabelRule[], destFilePath: string | null): Promise<string> {
        return this.send("setPageLabels", [rules, destFilePath]);
    }

    public simulatePageLabels(rules: PageLabelRule[]): Promise<string[]> {
        return this.send("simulatePageLabels", [rules]);
    }
}

export const rpc = new RpcClient();