/**
 * Sidecar 自动更新脚本 (CommonJS 版)
 * 流程：清理 -> Gradle构建 -> 复制 -> 修复权限 -> 调用签名脚本
 */

const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const os = require('os');

// === 引入统一路径配置 (关键修改) ===
const {
    PROJECT_ROOT,
    SERVER_ROOT,
    TAURI_RESOURCES_DIR,
    TAURI_ROOT
} = require('./paths.cjs');


const SIGN_SCRIPT = path.join(TAURI_ROOT, 'scripts', 'sign-sidecar.cjs');

// === 工具函数 ===
function run(command, cwd = PROJECT_ROOT) {
    try {
        console.log(`\x1b[36m[Exec]\x1b[0m ${command}`);
        execSync(command, { cwd, stdio: 'inherit' });
    } catch (error) {
        console.error(`\x1b[31m[Error]\x1b[0m 命令执行失败: ${command}`);
        process.exit(1);
    }
}

function cleanDir(dirPath) {
    if (fs.existsSync(dirPath)) {
        console.log(`\x1b[33m[Clean]\x1b[0m 删除旧资源: ${dirPath}`);
        try {
            fs.rmSync(dirPath, { recursive: true, force: true });
        } catch (e) {
            console.error(`\x1b[31m[Error]\x1b[0m 删除失败！请尝试 sudo rm -rf ${dirPath}`);
            process.exit(1);
        }
    }
}

// === 主流程 ===
console.log('\n🚀 开始更新 Java Sidecar (CJS)...\n');

try {
    // 1. 清理旧资源
    cleanDir(TAURI_RESOURCES_DIR);

    // 2. 构建 Java
    console.log('\n📦 Step 1: 构建 Java 镜像 (Gradle)...');
    const isWin = os.platform() === 'win32';
    const gradlew = isWin ? 'gradlew.bat' : './gradlew';

    // Mac/Linux 下确保 gradlew 有权限
    if (!isWin) run(`chmod +x gradlew`, SERVER_ROOT);

    // 执行构建
    run(`${gradlew} clean jlink`, SERVER_ROOT);

    // 3. 复制资源
    console.log('\n🚚 Step 2: 复制镜像到 Tauri 资源目录...');
    const resourceParent = path.dirname(TAURI_RESOURCES_DIR);
    if (!fs.existsSync(resourceParent)) fs.mkdirSync(resourceParent, { recursive: true });

    const sourceImage = path.join(SERVER_ROOT, 'build', 'image');
    fs.cpSync(sourceImage, TAURI_RESOURCES_DIR, { recursive: true });

    // 4. 修复权限
    console.log('\n🔓 Step 3: 修复文件权限 (755)...');
    if (!isWin) {
        run(`chmod -R 755 "${TAURI_RESOURCES_DIR}"`);
    }

    // 5. 调用独立签名脚本 (使用统一配置中的路径)
    console.log('\n✍️ Step 4: 调用签名脚本...');
    run(`node "${SIGN_SCRIPT}"`);

    console.log('\n✅✅✅ Sidecar 更新全部完成！\n');

} catch (e) {
    console.error(e);
    process.exit(1);
}