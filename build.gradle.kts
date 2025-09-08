import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.compose.ComposeExtension

plugins {
    // 说明: Kotlin 与 Compose 编译器插件版本需保持一致, 否则会出现不匹配警告或编译异常。
    kotlin("jvm") version "2.2.10"
    id("org.jetbrains.compose") version "1.8.2" // 当前稳定版 (保持, 后续可再评估是否升级 >=1.9.x)
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" // 与 kotlin 插件对齐 (之前是 2.0.0 -> 不兼容)
    idea
    java
}

version = "2.2.0"

val itextVersion = "9.2.0"
val koinVersion = "4.1.0"

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

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.0")

    // --- Kermit (用于新的 Kotlin 代码) ---
    // Kermit 核心库
    implementation("co.touchlab:kermit:2.0.3")

    implementation("com.itextpdf:bouncy-castle-adapter:${itextVersion}")
    implementation("com.itextpdf:kernel:${itextVersion}")
    implementation("com.itextpdf:io:${itextVersion}")
    implementation("com.itextpdf:layout:${itextVersion}")
    implementation("com.itextpdf:font-asian:${itextVersion}")

    implementation("javax.validation:validation-api:2.0.1.Final")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")

    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("org.slf4j:slf4j-simple:2.0.16")

    implementation("org.apache.pdfbox:pdfbox:3.0.3")
    implementation("org.apache.pdfbox:fontbox:3.0.3")

    implementation(platform("io.insert-koin:koin-bom:$koinVersion"))
    implementation("io.insert-koin:koin-core")
    implementation("io.insert-koin:koin-compose")
}

extensions.configure<ComposeExtension> {
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

