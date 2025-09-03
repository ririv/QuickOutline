import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.compose.ComposeExtension

plugins {
    kotlin("jvm") version "2.0.0"
    id("org.jetbrains.compose") version "1.8.2"
    kotlin("plugin.compose") version "2.0.0"
    idea
    java
}

// 项目版本
version = "2.2.0"

// 使用 val/var 定义变量
val itextVersion = "9.2.0"
val koinVersion = "4.1.0"

// 仓库配置，建议放在前面
repositories {
    // 使用 https，http 会报警告
    google()
    gradlePluginPortal()
    mavenLocal()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

// 注意: Compose 资源配置块已移除。
// 在较新的插件版本中，许多配置都有了智能的默认值，
// 不再需要像旧版本那样进行显式配置。
// 如果你需要自定义，请查阅相应插件版本的官方文档。

// Java 版本选择 21（LTS）
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
kotlin.jvmToolchain(21)


// 依赖配置
// 在 kts 中，字符串使用双引号 ""
dependencies {
    // compose 扩展由插件自动提供，无需手动获取
    implementation(compose.desktop.currentOs) // 修复: 使用 currentOs 引入平台特定的桌面依赖
    implementation(compose.components.resources)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.0")

    // 使用该依赖打包会出现 错误: 当前不支持已签名模块化 JAR ..\QuickOutline\build\jlinkbase\jlinkjars\bcpkix-jdk18on-1.78.jar,
    // https://stackoverflow.com/questions/50597926/signed-modular-jar-with-crypto-provider-cannot-be-linked-into-run-time-image
    implementation("com.itextpdf:bouncy-castle-adapter:${itextVersion}") //签名用，加密的PDF必须添加
    implementation("com.itextpdf:kernel:${itextVersion}")
    implementation("com.itextpdf:io:${itextVersion}")
    implementation("com.itextpdf:layout:${itextVersion}")
    implementation("com.itextpdf:font-asian:${itextVersion}") // 亚洲字体支持

    implementation("javax.validation:validation-api:2.0.1.Final")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")

    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("org.slf4j:slf4j-simple:2.0.16") // Or another implementation

    // Apache PDFBox
    implementation("org.apache.pdfbox:pdfbox:3.0.3")
    implementation("org.apache.pdfbox:fontbox:3.0.3")

    // Koin for Dependency Injection
    implementation(platform("io.insert-koin:koin-bom:$koinVersion"))
    implementation("io.insert-koin:koin-core")
    implementation("io.insert-koin:koin-compose")
}

// Compose for Desktop 应用配置
// 修复: 使用 ComposeExtension 并在内部访问 desktop 块
extensions.configure<ComposeExtension> {
    desktop {
        application {
            mainClass = "com.ririv.quickoutline.view.AppKt"

            nativeDistributions {
                targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
                packageName = "QuickOutline"
                packageVersion = "2.2.0"

                // --- 图标配置 ---
                // 你的注释中提到路径不对，这里提供了正确的 Kotlin DSL 语法。
                // 请在确认路径正确后取消下面的注释。
                // val iconPath = "src/main/resources/com/ririv/quickoutline/view/icon/"
                //
                // val osName = System.getProperty("os.name").toLowerCase()
                // if (osName.contains("win")) {
                //     windows.iconFile.set(project.file("${iconPath}icon.ico"))
                // } else if (osName.contains("mac")) {
                //     macOS.iconFile.set(project.file("${iconPath}icon.icns"))
                // } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
                //     linux.iconFile.set(project.file("${iconPath}icon.png"))
                // }
            }
        }
    }
}

