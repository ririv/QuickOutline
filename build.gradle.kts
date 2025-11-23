import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.mp)
    alias(libs.plugins.compose.compiler)
    idea
    java // 确保应用 Java 插件
}

version = "2.2.0"
group = "com.ririv"

repositories {
    google()
    gradlePluginPortal()
    mavenLocal()
    mavenCentral()
    maven("https.maven.pkg.jetbrains.space/public/p/compose/dev")
}

// ⚠️ 注意：JDK 25 比较新，确保你的环境和 CI 支持。
// jpackage 会使用这个 toolchain 指向的 JDK 来打包 JRE。
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    // ------------------- 核心修正 START -------------------
    // 1. 移除所有安卓相关的 compose 依赖。
    //    val composeBom = platform("androidx.compose:compose-bom:2.0.0") // -> 移除
    //    implementation(composeBom) // -> 移除
    //    implementation("androidx.compose.ui:ui") // -> 移除
    //    implementation("androidx.compose.ui:ui-tooling-preview") // -> 移除
    //    implementation("androidx.compose.foundation:foundation") // -> 移除

    // 2. 只保留这两行由 org.jetbrains.compose 插件提供的依赖。
    //    它会自动引入所有桌面平台所需的 ui, foundation, material, icons 等库。
    implementation(compose.desktop.currentOs)
    implementation(compose.components.resources)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // --- 工具库 ---
    implementation(libs.coroutines.swing)
    implementation(libs.kermit) // 日志
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.simple)

    // --- PDF 处理 (这两个库很大，ProGuard 压缩很有必要) ---
    implementation(libs.bundles.itext)
    implementation(libs.pdfbox)
    implementation(libs.fontbox)

    // --- 依赖注入 ---
    implementation(platform(libs.koin.bom))
    implementation(libs.bundles.koin)
    // 引入 JNA，用于调用 dll/dylib
    implementation("net.java.dev.jna:jna:5.13.0")
    implementation("net.java.dev.jna:jna-platform:5.13.0")// 引入 JNA，用于调用 dll/dylib
    implementation("net.java.dev.jna:jna:5.13.0")
    implementation("net.java.dev.jna:jna-platform:5.13.0")

    // --- 测试 ---
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit)
}

// 使用标准的 compose DSL 配置
compose {
    desktop {
        application {
            mainClass = "com.ririv.quickoutline.view.AppKt"

            // 1. 压缩与混淆配置 (仅在运行 packageRelease... 任务时生效)
//            buildTypes.release.proguard {
//                version = "7.8.1"
//                obfuscate.set(true)
//                optimize.set(true)
//                // ⚠️ 必须在项目根目录创建此文件，否则 PDFBox/Koin 等库会报错
//                configurationFiles.from(project.file("compose-desktop.pro"))
//            }

            nativeDistributions {
                // 2. 基础元数据
                targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
                packageName = "QuickOutline"
                packageVersion = "2.2.0"
                description = "QuickOutline PDF Tool"
                copyright = "© 2025 Ririv"
                vendor = "Ririv Inc."

                // 3. 包含所有模块 (防止因模块缺失导致的运行错误，配合 ProGuard 使用)
                includeAllModules = false

                // 4. Windows 专属设置
                windows {
                    // 需在 src/main/resources 下放置 icon.ico
                    iconFile.set(project.file("src/main/resources/icon.ico"))
                    menu = true
                    shortcut = true
                    // 生成唯一的 UUID 用于覆盖安装，不要频繁更改
                    upgradeUuid = "12345678-1234-1234-1234-123456789012"
                }

                // 5. macOS 专属设置
                macOS {
                    // 需在 src/main/resources 下放置 icon.icns
//                    iconFile.set(project.file("src/main/resources/icon.icns"))
                    bundleID = "com.ririv.quickoutline"
                    dockName = "QuickOutline"
                }

                // 6. Linux 专属设置
                linux {
                    iconFile.set(project.file("src/main/resources/icon.png"))
                    shortcut = true
                }
            }
        }
    }
}

tasks.test {
    enabled = false
}