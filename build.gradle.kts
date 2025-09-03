import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.compose.ComposeExtension

plugins {
    kotlin("jvm") version "2.0.0"
    id("org.jetbrains.compose") version "1.8.2"
    // 核心修正: 根据 Kotlin 2.0.0 的要求，必须显式应用 Compose 编译器插件。
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
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
    // ------------------- 核心修正 END ---------------------

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.0")

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

