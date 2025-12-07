package com.ririv.quickoutline.service.pdfpreview;

import com.ririv.quickoutline.pdfProcess.PdfRenderSession;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
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
    private PdfRenderSession session;
    private int totalPages = 0;

    public synchronized void openFile(File file) {
        close();
        try {
            session = new PdfRenderSession(file);
            totalPages = session.getPageCount();
        } catch (Exception e) {
            log.error("Failed to open PDF session for file: {}", file, e);
        }
    }

    public synchronized void close() {
        if (session != null) {
            session.close();
            session = null;
        }
        cache.clear();
        thumbCache.clear();
        totalPages = 0;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public CompletableFuture<byte[]> getImage(int pageIndex) {
        // 1. Check Cache
        byte[] cached = cache.get(pageIndex);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        // 2. Render from Session
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
        if (session != null) {
            if (pageIndex < 0 || pageIndex >= totalPages) {
                return CompletableFuture.completedFuture(null);
            }
            // Use THUMBNAIL_SCALE (1.0) or even smaller if needed
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
        return cache.get(pageIndex);
        // We don't implement sync render here to avoid blocking, 
        // and currently Diff logic is self-contained in PreviewService.
    }
}
