package com.ririv.quickoutline.view

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.ririv.quickoutline.di.appModule
import org.koin.core.context.startKoin

fun main() = application {
    startKoin {
        modules(appModule)
    }

    Window(onCloseRequest = ::exitApplication) {
        MaterialTheme {
            MainView()
        }
    }
}
