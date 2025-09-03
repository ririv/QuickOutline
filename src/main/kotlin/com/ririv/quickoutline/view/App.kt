package com.ririv.quickoutline.view

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.ririv.quickoutline.di.appModule
import com.ririv.quickoutline.view.theme.QuickOutlineTheme
import com.ririv.quickoutline.view.ui.MainView
import com.ririv.quickoutline.view.viewmodel.BookmarkViewModel
import com.ririv.quickoutline.view.viewmodel.MainViewModel
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.getKoin
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDropEvent
import java.io.File

fun main() = application {
    startKoin {
        modules(appModule)
    }

    val mainViewModel: MainViewModel = getKoin().get()

    Window(onCloseRequest = ::exitApplication) {
        window.contentPane.dropTarget = object : DropTarget() {
            override fun drop(dtde: DropTargetDropEvent) {
                dtde.acceptDrop(dtde.dropAction)
                val transferable = dtde.transferable
                if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    val transferData = transferable.getTransferData(DataFlavor.javaFileListFlavor)
                    if (transferData is List<*>) {
                        for (item in transferData) {
                            if (item is File && item.extension.equals("pdf", ignoreCase = true)) {
                                mainViewModel.openPdf(item.absolutePath)
                                break // Process only the first PDF file
                            }
                        }
                    }
                }
                dtde.dropComplete(true)
            }
        }

        QuickOutlineTheme {
            MainView()
        }
    }
}
