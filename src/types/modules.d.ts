declare module 'markdown-it-texmath';
declare const __APP_VERSION__: string;
declare module 'markdown-it-mark';
declare module 'markdown-it-footnote';
declare module 'markdown-it-sub';
declare module 'markdown-it-sup';
declare module 'markdown-it-abbr';
declare module 'markdown-it-attrs';
declare module 'markdown-it-bracketed-spans';

declare module '*?worker' {
    const workerConstructor: {
        new (): Worker;
    };
    export default workerConstructor;
}
// markdown-it-container has @types package installed