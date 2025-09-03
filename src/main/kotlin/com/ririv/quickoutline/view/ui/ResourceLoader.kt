package com.ririv.quickoutline.view.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Density
import org.jetbrains.compose.resources.decodeToSvgPainter
import java.io.InputStream

@Composable
//@Suppress("DEPRECATION") // 暂时使用弃用的 API，等待新的资源系统成熟
fun loadResourcePainter(resourcePath: String): Painter {
    val classLoader = Thread.currentThread().contextClassLoader
    val inputStream: InputStream = classLoader.getResourceAsStream(resourcePath)
        ?: throw IllegalArgumentException("Resource not found: $resourcePath")

    return inputStream.readAllBytes().decodeToSvgPainter(Density(1f))
}