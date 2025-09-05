package com.ririv.quickoutline.view

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.ririv.quickoutline.di.appModule
import com.ririv.quickoutline.view.theme.QuickOutlineTheme
import com.ririv.quickoutline.view.ui.MainView
import com.ririv.quickoutline.view.viewmodel.MainViewModel
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.getKoin
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.dnd.DnDConstants
import java.io.File
import java.net.URI
import javax.swing.SwingUtilities
import javax.swing.JFrame
import javax.swing.TransferHandler
import javax.swing.JComponent
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.net.URL
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicInteger
import java.awt.Container
import java.awt.event.ContainerAdapter
import java.awt.event.ContainerEvent
import java.beans.PropertyChangeListener

fun main() = application {
    startKoin {
        modules(appModule)
    }

    val mainViewModel: MainViewModel = getKoin().get()

    Window(onCloseRequest = ::exitApplication) {
    val frame = window as? JFrame
        // Helper utilities for TransferHandler
    val uriListFlavor = DataFlavor("text/uri-list;class=java.lang.String")
    val javaUrlFlavor = DataFlavor("application/x-java-url; class=java.net.URL")

        fun flavorsToString(t: Transferable): String =
            t.transferDataFlavors.joinToString { it.humanPresentableName + " (" + it.mimeType + ")" }

        fun parseUriList(text: String): List<File> =
            text.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .mapNotNull { runCatching { URI(it) }.getOrNull() }
                .filter { it.scheme == "file" }
                .mapNotNull { runCatching { File(it) }.getOrNull() }
                .toList()

        fun extractPdf(t: Transferable): File? {
            // 1) javaFileListFlavor
            if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                val data = t.getTransferData(DataFlavor.javaFileListFlavor)
                if (data is List<*>) {
                    val f = data.firstOrNull { it is File && (it as File).extension.equals("pdf", true) } as? File
                    if (f != null) return f
                }
            }
            // 1.5) application/x-java-url
            if (t.isDataFlavorSupported(javaUrlFlavor)) {
                val url = runCatching { t.getTransferData(javaUrlFlavor) as? URL }.getOrNull()
                val f = url?.let { runCatching { File(it.toURI()) }.getOrNull() }
                if (f != null && f.extension.equals("pdf", true)) return f
            }
            // 2) text/uri-list
            if (t.isDataFlavorSupported(uriListFlavor)) {
                val s = runCatching { t.getTransferData(uriListFlavor) as? String }.getOrNull()
                val files = s?.let { parseUriList(it) } ?: emptyList()
                val f = files.firstOrNull { it.extension.equals("pdf", true) }
                if (f != null) return f
            }
            // 3) stringFlavor (try URIs first, then plain paths)
            if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                val s = runCatching { t.getTransferData(DataFlavor.stringFlavor) as? String }.getOrNull()
                if (!s.isNullOrBlank()) {
                    val filesFromUris = parseUriList(s)
                    val f1 = filesFromUris.firstOrNull { it.extension.equals("pdf", true) }
                    if (f1 != null) return f1
                    val f2 = s.lineSequence()
                        .map { it.trim() }
                        .filter { it.isNotEmpty() && !it.startsWith("#") }
                        .map { File(it) }
                        .firstOrNull { it.extension.equals("pdf", true) && it.exists() }
                    if (f2 != null) return f2
                }
            }
            return null
        }

    val handler = object : TransferHandler() {
            override fun canImport(support: TransferSupport): Boolean {
                val t = support.transferable
                val ok = t.isDataFlavorSupported(DataFlavor.javaFileListFlavor) ||
                        t.isDataFlavorSupported(uriListFlavor) ||
                        t.isDataFlavorSupported(DataFlavor.stringFlavor)
                support.dropAction = COPY
                println("[DnD][TH] canImport=$ok isDrop=${support.isDrop} flavors=${flavorsToString(t)}")
                return ok
            }

            override fun importData(support: TransferSupport): Boolean {
                val t = support.transferable
                println("[DnD][TH] importData isDrop=${support.isDrop} action=${support.dropAction} flavors=${flavorsToString(t)}")
                val pdf = extractPdf(t)
                return if (pdf != null) {
                    println("[DnD][TH] opening PDF: ${pdf.absolutePath}")
                    SwingUtilities.invokeLater { mainViewModel.openPdf(pdf.absolutePath) }
                    true
                } else {
                    println("[DnD][TH] no PDF found")
                    false
                }
            }
        }

        // Prefer attaching to the rootPane or contentPane (which are JComponents)
        // Attach to rootPane and glassPane; plus dynamic re-attachment for Compose replacing panes
        val attached = mutableListOf<String>()
        frame?.rootPane?.let { rp ->
            rp.transferHandler = handler
            attached.add("rootPane")
        }
        (frame?.glassPane as? javax.swing.JComponent)?.let { gp ->
            gp.transferHandler = handler
            attached.add("glassPane")
        }
        (frame?.contentPane as? javax.swing.JComponent)?.let { cp ->
            cp.transferHandler = handler
            attached.add("contentPane")
        }
        (frame?.layeredPane as? javax.swing.JComponent)?.let { lp ->
            lp.transferHandler = handler
            attached.add("layeredPane")
        }
        println("[DnD][TH] handler attached to: ${attached.joinToString()}")

        // Recursively attach handler to all existing children (covers Compose inner panel)
        fun attachRecursively(c: java.awt.Component, counter: AtomicInteger) {
            if (c is JComponent) {
                c.transferHandler = handler
                counter.incrementAndGet()
            }
            if (c is Container) {
                c.components.forEach { attachRecursively(it, counter) }
            }
        }

        val cp = frame?.contentPane as? JComponent
        if (cp != null) {
            val cnt = AtomicInteger(0)
            attachRecursively(cp, cnt)
            println("[DnD][TH] recursively attached to ${cnt.get()} components under contentPane")

            // Keep attaching to components added later (Compose may add/replace panels)
            cp.addContainerListener(object : ContainerAdapter() {
                override fun componentAdded(e: ContainerEvent) {
                    val added = e.child ?: return
                    val localCnt = AtomicInteger(0)
                    attachRecursively(added, localCnt)
                    println("[DnD][TH] attached to newly added ${added.javaClass.name} (+${localCnt.get()})")
                }
            })
        }

        // Re-attach if panes get replaced by Compose/Swing
        frame?.addPropertyChangeListener(PropertyChangeListener { evt ->
            when (evt.propertyName) {
                "glassPane", "layeredPane", "contentPane", "rootPane" -> {
                    val comp = evt.newValue
                    if (comp is JComponent) {
                        comp.transferHandler = handler
                        println("[DnD][TH] reattached handler to ${evt.propertyName}")
                        if (comp is Container) {
                            val cnt = AtomicInteger(0)
                            attachRecursively(comp, cnt)
                            println("[DnD][TH] reattached recursively to ${cnt.get()} components under ${evt.propertyName}")
                        }
                    }
                }
            }
        })

    // Fallback AWT DropTarget directly on frame (lowest-common-denominator)
        val lastOpened = AtomicReference<String>("")
        frame?.dropTarget = object : DropTarget() {
            override fun dragEnter(dtde: java.awt.dnd.DropTargetDragEvent) {
                val t = dtde.transferable
                val ok = t.isDataFlavorSupported(DataFlavor.javaFileListFlavor) ||
                        t.isDataFlavorSupported(uriListFlavor) ||
                        t.isDataFlavorSupported(javaUrlFlavor) ||
                        t.isDataFlavorSupported(DataFlavor.stringFlavor)
                if (ok) dtde.acceptDrag(DnDConstants.ACTION_COPY) else dtde.rejectDrag()
        println("[DnD][DT] dragEnter ok=$ok action=${dtde.dropAction}")
            }

            override fun dragOver(dtde: DropTargetDragEvent) {
                val t = dtde.transferable
                val ok = t.isDataFlavorSupported(DataFlavor.javaFileListFlavor) ||
                        t.isDataFlavorSupported(uriListFlavor) ||
                        t.isDataFlavorSupported(javaUrlFlavor) ||
                        t.isDataFlavorSupported(DataFlavor.stringFlavor)
        if (ok) dtde.acceptDrag(DnDConstants.ACTION_COPY) else dtde.rejectDrag()
            }

            override fun drop(dtde: DropTargetDropEvent) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY)
                    val t = dtde.transferable
                    val pdf = extractPdf(t)
                    if (pdf != null) {
                        val path = pdf.absolutePath
                        if (lastOpened.getAndSet(path) != path) {
                            println("[DnD][DT] opening PDF: $path")
                            SwingUtilities.invokeLater { mainViewModel.openPdf(path) }
                        }
                        dtde.dropComplete(true)
                        return
                    }
                    dtde.dropComplete(false)
                } catch (e: Exception) {
                    println("[DnD][DT] drop error: ${e.message}")
                    dtde.dropComplete(false)
                }
            }
        }

        QuickOutlineTheme {
            MainView()
        }
    }
}
