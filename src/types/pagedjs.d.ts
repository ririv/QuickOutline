declare module 'pagedjs' {
    export class Previewer {
        constructor(options?: any); // constructor accepts options object
        preview(content: string, css: any[], container: HTMLElement, options?: any): Promise<any>;
        // Add other methods/properties if needed, but this is enough to resolve the error
    }

    export class Handler {
        constructor(chunker: any, polisher: any, caller: any);
        afterPageLayout(pageElement: HTMLElement, page: any, breakToken: any): void;
    }

    export function registerHandlers(handler: any): void;
}