import { mount } from 'svelte';
import App from './App.svelte';
import './assets/global.css'; // Import global styles
import { setLocale } from './lib/paraglide/runtime.js';

// Detect system language: set to 'zh-CN' if it matches, otherwise default to 'en'
const systemLang = navigator.language;
const defaultLang = systemLang === 'zh-CN' ? 'zh-CN' : 'en';
setLocale(defaultLang);

const app = mount(App, {
    target: document.getElementById('app')!,
});

export default app;
