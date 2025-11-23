# ---------------------------------------------------------
#  Compose Desktop ProGuard Rules (QuickOutline - Final)
# ---------------------------------------------------------

# 1. Compose & Kotlin 基础
-keep class androidx.compose.** { *; }
-keep class org.jetbrains.compose.** { *; }
-keep class androidx.lifecycle.** { *; }
-keep class kotlinx.coroutines.** { *; }

# 2. Koin (依赖注入)
-keep class org.koin.** { *; }

# 3. PDFBox & FontBox
-keep class org.apache.pdfbox.** { *; }
-keep class org.apache.fontbox.** { *; }
-keep class org.apache.commons.logging.** { *; }
# 【新增】解决 PDFBox 的 invokeExact 报错
-dontwarn org.apache.pdfbox.io.IOUtils

# 4. iText (保留代码但忽略可选依赖警告)
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**
-dontwarn com.fasterxml.jackson.**
-dontwarn org.bouncycastle.**

# 5. 忽略常用库的缺失警告
-dontwarn org.apache.commons.logging.**
-dontwarn org.apache.log4j.**
-dontwarn org.slf4j.**
-dontwarn ch.qos.logback.**
-dontwarn javax.servlet.**
-dontwarn java.awt.**
-dontwarn javax.annotation.**
-dontwarn sun.misc.Unsafe

# 6. 你的应用代码
-keep class com.ririv.quickoutline.** { *; }

# 7. 一般性规则
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,EnclosingMethod,InnerClasses
-dontnote **