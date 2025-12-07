package com.ririv.quickoutline.service.pdfpreview;

import com.ririv.quickoutline.pdfProcess.PdfRenderSession;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service dedicated to managing images derived from the currently opened physical PDF file.
 * Lifecycle: Tied to the opened file session.
 */
@Singleton
public class FileImageService {
    private static final Logger log = LoggerFactory.getLogger(FileImageService.class);

    private final Map<Integer, byte[]> cache = new ConcurrentHashMap<>();
    private final Map<Integer, byte[]> thumbCache = new ConcurrentHashMap<>();


    // PDFBox 的 `PDDocument` 实例不是线程安全的
    // 这意味着，你不能在多个线程中同时操作同一个 PDDocument 对象（例如同时调用
    // renderer.renderImage(...)）。如果这样做，可能会导致内部状态损坏、异常或者渲染错误
    // 如果我们想要并行渲染（例如同时渲染第 1 页和第 2 页）以提高速度，我们必须：
    // 1. 为每个线程打开一个独立的 PDDocument 实例（意味着多次 Loader.loadPDF(file)，这会消耗更多内存）。
    // 2. 或者使用一个 PDDocument 对象池。
    // 我们目前采用方案1，足以应付当前场景

    // Use multiple sessions to allow parallel rendering
    private final List<PdfRenderSession> sessions = new ArrayList<>();
    private static final int CONCURRENCY_LEVEL = 3;
    
    private int totalPages = 0;

    public synchronized void openFile(File file) {
        close();
        try {
            // Open multiple sessions for concurrency
            for (int i = 0; i < CONCURRENCY_LEVEL; i++) {
                sessions.add(new PdfRenderSession(file));
            }
            
            if (!sessions.isEmpty()) {
                totalPages = sessions.get(0).getPageCount();
            }
        } catch (Exception e) {
            log.error("Failed to open PDF session for file: {}", file, e);
            close(); // Ensure partial failures are cleaned up
        }
    }

    public synchronized void close() {
        for (PdfRenderSession session : sessions) {
            if (session != null) {
                session.close();
            }
        }
        sessions.clear();
        cache.clear();
        thumbCache.clear();
        totalPages = 0;
    }

    public int getTotalPages() {
        return totalPages;
    }
    
    private PdfRenderSession getSessionForPage(int pageIndex) {
        if (sessions.isEmpty()) return null;
        // Simple load balancing based on page index
        return sessions.get(pageIndex % sessions.size());
    }

    public CompletableFuture<byte[]> getImage(int pageIndex) {
        // 1. Check Cache
        byte[] cached = cache.get(pageIndex);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        // 2. Render from Session
        PdfRenderSession session = getSessionForPage(pageIndex);
        if (session != null) {
            if (pageIndex < 0 || pageIndex >= totalPages) {
                return CompletableFuture.completedFuture(null);
            }
            return session.renderToPngWithDPIAsync(pageIndex, PdfRenderSession.PREVIEW_DPI)
                    .thenApply(data -> {
                        if (data != null) {
                            cache.put(pageIndex, data);
                        }
                        return data;
                    });
        }

        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<byte[]> getThumbnail(int pageIndex) {
        // 1. Check Cache
        byte[] cached = thumbCache.get(pageIndex);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        // 2. Render from Session
        PdfRenderSession session = getSessionForPage(pageIndex);
        if (session != null) {
            if (pageIndex < 0 || pageIndex >= totalPages) {
                return CompletableFuture.completedFuture(null);
            }
            // Use THUMBNAIL_SCALE (1.0)
            return session.renderToPngWithScaleAsync(pageIndex, PdfRenderSession.THUMBNAIL_SCALE)
                    .thenApply(data -> {
                        if (data != null) {
                            thumbCache.put(pageIndex, data);
                        }
                        return data;
                    });
        }

        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Synchronous get (helper for diff logic if needed, though PreviewService shouldn't depend on it ideally)
     */
    public byte[] getImageSync(int pageIndex) {
        byte[] cached = cache.get(pageIndex);
        if (cached != null) return cached;
        return null;
    }
}
