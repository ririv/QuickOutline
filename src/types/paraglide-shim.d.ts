/**
 * Paraglide-JS Shim
 * 
 * This file is required to silence TypeScript errors regarding the 'async_hooks' module
 * in pure client-side environments (like Tauri/Vite SPA).
 * 
 * Paraglide-JS generates a 'server.js' file that imports 'async_hooks' for SSR support.
 * Since we are running in a browser/Webview environment where Node.js modules are unavailable,
 * and we don't include Node.js types in our tsconfig.json to avoid conflicts (e.g. setTimeout return types),
 * we simply declare this module as 'any' to satisfy the compiler for the unused server code.
 */
declare module "async_hooks";
