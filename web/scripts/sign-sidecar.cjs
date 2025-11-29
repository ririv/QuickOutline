/**
 * macOS 独立签名脚本
 * 作用：对 src-tauri/resources/java-runtime 下的二进制文件进行 Ad-hoc 签名
 * 用法：node scripts/sign-sidecar.cjs
 */

const { execSync } = require('child_process');
const path = require('path');
const os = require('os');
const fs = require('fs');

// === 引入统一路径配置 ===
const { TAURI_RESOURCES_DIR } = require('./paths.cjs');

// === 工具函数 ===
function run(command) {
    try {
        console.log(`\x1b[36m[Exec]\x1b[0m ${command}`);
        execSync(command, { stdio: 'inherit' });
    } catch (error) {
        console.error(`\x1b[31m[Error]\x1b[0m 签名失败: ${command}`);
        process.exit(1);
    }
}

// === 主流程 ===
(function main() {
    console.log('\n🔐 开始执行 macOS 二进制签名检查...\n');

    // 1. 系统检测
    if (os.platform() !== 'darwin') {
        console.log('⏭️  检测到非 macOS 系统，跳过签名步骤。');
        return;
    }

    // 2. 目录检测
    if (!fs.existsSync(TAURI_RESOURCES_DIR)) {
        console.error(`\x1b[31m[Error]\x1b[0m 找不到资源目录: ${TAURI_RESOURCES_DIR}`);
        console.error('请先执行 npm run update:sidecar 生成资源文件。');
        process.exit(1);
    }

    // 3. 定义需要签名的关键文件
    // 注意：除了 java 和 libjvm，有些情况下其他的 dylib 也可能导致问题，
    // 这里我们先签最核心的两个。如果之后还报 Killed:9，可以改为递归签所有文件。
    const filesToSign = [
        path.join(TAURI_RESOURCES_DIR, 'bin', 'java'),
        path.join(TAURI_RESOURCES_DIR, 'lib', 'server', 'libjvm.dylib')
    ];

    // 4. 执行签名
    let signedCount = 0;
    for (const file of filesToSign) {
        if (fs.existsSync(file)) {
            // --force: 覆盖原有签名
            // --sign -: 使用 Ad-hoc 本地签名
            run(`codesign --force --sign - "${file}"`);
            signedCount++;
        } else {
            console.warn(`\x1b[33m[Warn]\x1b[0m 文件不存在，跳过: ${file}`);
        }
    }

    if (signedCount > 0) {
        console.log(`\n✅ 成功对 ${signedCount} 个核心文件进行了签名修复。`);
    } else {
        console.warn('\n⚠️ 未找到任何可签名的文件，请检查目录结构。');
    }
})();