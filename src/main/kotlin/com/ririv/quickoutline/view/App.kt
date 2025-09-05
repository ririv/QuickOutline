package com.ririv.quickoutline.view

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.ririv.quickoutline.di.appModule
import com.ririv.quickoutline.view.theme.QuickOutlineTheme
import com.ririv.quickoutline.view.ui.MainView
import org.koin.core.context.startKoin

fun main() = application {
    startKoin { modules(appModule) }

    Window(onCloseRequest = ::exitApplication) {
        QuickOutlineTheme { MainView() }
    }
}
