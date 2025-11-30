/**
 * 统一路径配置文件
 * 作用：管理项目中的关键目录路径，供其他脚本引用
 */
const path = require('path');

// === 配置路径 ===
// 项目根目录
const PROJECT_ROOT = path.resolve(__dirname, '..');

// Java Server 源码目录
const SERVER_ROOT =  path.join(PROJECT_ROOT, 'server');
// Tauri 资源目录 (Java Runtime 存放位置)
const TAURI_ROOT = PROJECT_ROOT
const TAURI_RESOURCES_DIR = path.join(TAURI_ROOT, 'src-tauri', 'resources', 'java-runtime');

console.log(`\x1b[34m[Project]\x1b[0m ${PROJECT_ROOT}`);
console.log(`\x1b[34m[Server]\x1b[0m ${SERVER_ROOT}`);
console.log(`\x1b[34m[Tauri Resources]\x1b[0m ${TAURI_RESOURCES_DIR}`);

module.exports = {
    PROJECT_ROOT,
    SERVER_ROOT,
    TAURI_ROOT,
    TAURI_RESOURCES_DIR,
};