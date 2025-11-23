import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.compose.ComposeExtension

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.mp)
    alias(libs.plugins.compose.compiler)
    idea
    java
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

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
kotlin.jvmToolchain(21)

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
    // ------------------- 核心修正 END ---------------------

    implementation(libs.coroutines.swing)

    // --- Kermit (用于新的 Kotlin 代码) ---
    // Kermit 核心库
    implementation(libs.kermit)

    implementation(libs.bundles.itext)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit)

    implementation(libs.slf4j.api)
    implementation(libs.slf4j.simple)

    implementation(libs.pdfbox)
    implementation(libs.fontbox)

    implementation(platform(libs.koin.bom))
    implementation(libs.bundles.koin)
}

extensions.configure<ComposeExtension> {
    // 自定义资源生成 Res 类所在包，避免默认重复 quickoutline 段 (原: com.ririv.quickoutline.quickoutline.generated.resources)
    // resources { 自定义生成包名 (当前 Compose 版本 API 暂未识别 packageName / packageClassName 属性, 暂时注释)
    // }
    desktop {
        application {
            mainClass = "com.ririv.quickoutline.view.AppKt"

            nativeDistributions {
                targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
                packageName = "QuickOutline"
                packageVersion = "2.2.0"
            }
        }
    }
}

tasks.test {
    enabled = false
}


