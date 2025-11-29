/**
 * Sidecar 自动更新脚本
 * 作用：清理 -> 构建 Java -> 复制 -> 修复权限 -> 签名(Mac)
 */

const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const os = require('os');

// === 配置路径 ===
const PROJECT_ROOT = path.resolve(__dirname, '..');
// const SERVER_DIR = path.join(PROJECT_ROOT, 'server');
const SERVER_DIR = PROJECT_ROOT
const TAURI_RESOURCES_DIR = path.join(PROJECT_ROOT, 'web', 'src-tauri', 'resources', 'java-runtime');

console.log(`\x1b[34m[Project]\x1b[0m ${PROJECT_ROOT}`);
console.log(`\x1b[34m[Server]\x1b[0m ${SERVER_DIR}`);
console.log(`\x1b[34m[Tauri Resources]\x1b[0m ${TAURI_RESOURCES_DIR}`);

// === 工具函数：执行命令 ===
function run(command, cwd = PROJECT_ROOT) {
    try {
        console.log(`\x1b[36m[Exec]\x1b[0m ${command}`);
        execSync(command, { cwd, stdio: 'inherit' }); // stdio: 'inherit' 让日志直接打印到控制台
    } catch (error) {
        console.error(`\x1b[31m[Error]\x1b[0m 命令执行失败: ${command}`);
        process.exit(1);
    }
}

// === 工具函数：删除文件夹 ===
function cleanDir(dirPath) {
    if (fs.existsSync(dirPath)) {
        console.log(`\x1b[33m[Clean]\x1b[0m 删除旧资源: ${dirPath}`);
        fs.rmSync(dirPath, { recursive: true, force: true });
    }
}

// === 主流程 ===
(function main() {
    console.log('\n🚀 开始更新 Java Sidecar...\n');

    // 1. 清理旧资源
    cleanDir(TAURI_RESOURCES_DIR);

    // 2. 构建 Java (Gradle jlink)
    console.log('\n📦 Step 1: 构建 Java 镜像...');

    // 判断系统选择 gradlew 脚本
    const isWin = os.platform() === 'win32';
    const gradlew = isWin ? 'gradlew.bat' : './gradlew';

    // Mac/Linux 下确保 gradlew 有权限
    if (!isWin) run(`chmod +x gradlew`, SERVER_DIR);

    // 执行构建
    run(`${gradlew} clean jlink`, SERVER_DIR);

    // 3. 复制资源
    console.log('\n🚚 Step 2: 复制镜像到 Tauri 资源目录...');
    // 确保父目录存在
    const resourceParent = path.dirname(TAURI_RESOURCES_DIR);
    if (!fs.existsSync(resourceParent)) fs.mkdirSync(resourceParent, { recursive: true });

    // 复制文件夹 (Node 16.7+ 支持 cpSync)
    const sourceImage = path.join(SERVER_DIR, 'build', 'image');
    fs.cpSync(sourceImage, TAURI_RESOURCES_DIR, { recursive: true });

    // 4. 修复权限 (解决 os error 13)
    console.log('\n🔓 Step 3: 修复文件权限 (755)...');
    if (!isWin) {
        run(`chmod -R 755 "${TAURI_RESOURCES_DIR}"`);
    }

    // 5. macOS 签名 (解决 Killed: 9)
    if (os.platform() === 'darwin') {
        console.log('\n✍️ Step 4: 执行 macOS 签名...');
        const javaBin = path.join(TAURI_RESOURCES_DIR, 'bin', 'java');
        const libJvm = path.join(TAURI_RESOURCES_DIR, 'lib', 'server', 'libjvm.dylib');

        run(`codesign --force --sign - "${javaBin}"`);
        run(`codesign --force --sign - "${libJvm}"`);
    } else {
        console.log('\n⏭️ 非 macOS 系统，跳过签名步骤。');
    }

    console.log('\n✅✅✅ Sidecar 更新成功！一切就绪。\n');
})();