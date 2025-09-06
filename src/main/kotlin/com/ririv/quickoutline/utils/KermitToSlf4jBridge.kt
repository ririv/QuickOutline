// src/main/kotlin/com/example/KermitToSlf4jBridge.kt

package com.example

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import org.slf4j.LoggerFactory
import org.slf4j.Logger as Slf4jLogger // 使用别名避免和 Kermit 的 Logger 混淆

/**
 * 这个 LogWriter 是一个桥梁。
 * 它将 Kermit 的日志调用转发到 SLF4J。
 */
class Slf4jWriter : LogWriter() {

    // 为不同的 tag 缓存 Slf4jLogger 实例，提高性能
    private val loggerCache = mutableMapOf<String, Slf4jLogger>()

    // 根据 tag 获取或创建一个 SLF4J logger
    private fun getLogger(tag: String): Slf4jLogger {
        return loggerCache.getOrPut(tag) { LoggerFactory.getLogger(tag) }
    }

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        val logger = getLogger(tag)

        // 将 Kermit 的日志级别映射到 SLF4J 的级别
        when (severity) {
            Severity.Verbose -> logger.trace(message, throwable)
            Severity.Debug -> logger.debug(message, throwable)
            Severity.Info -> logger.info(message, throwable)
            Severity.Warn -> logger.warn(message, throwable)
            Severity.Error -> logger.error(message, throwable)
            Severity.Assert -> logger.error(message, throwable)
        }
    }
}