declare module 'pagedjs' {
    export class Previewer {
        constructor(options?: any); // constructor accepts options object
        preview(content: string, css: any[], container: HTMLElement, options?: any): Promise<any>;
        // Add other methods/properties if needed, but this is enough to resolve the error
    }
}