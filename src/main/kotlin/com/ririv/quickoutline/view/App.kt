package com.ririv.quickoutline.view

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.Slf4jWriter
import com.ririv.quickoutline.di.appModule
import com.ririv.quickoutline.view.theme.QuickOutlineTheme
import com.ririv.quickoutline.view.ui.MainView
import org.koin.core.context.startKoin
import co.touchlab.kermit.Logger

fun main() = application {
    startKoin { modules(appModule) }

    // 关键步骤：在应用启动时，配置 Kermit 使用我们自定义的 Slf4jWriter
    // 这样所有 Kermit 的日志都会被重定向到 SLF4J
    Logger.setLogWriters(Slf4jWriter())

    Window(onCloseRequest = ::exitApplication) {
        QuickOutlineTheme { MainView() }
    }
}
