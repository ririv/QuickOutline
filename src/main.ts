import { mount } from 'svelte';
import App from './App.svelte';
import './assets/global.css'; // Import global styles
import { setLocale } from './lib/paraglide/runtime.js';
import { selectAppLocale } from './lib/appLocale';

// 根据系统语言显式选择：中文环境使用中文，其余环境使用英文。
setLocale(selectAppLocale(navigator.language), { reload: false });

const app = mount(App, {
    target: document.getElementById('app')!,
});

export default app;
