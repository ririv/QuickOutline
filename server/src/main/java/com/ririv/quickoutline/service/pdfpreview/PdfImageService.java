package com.ririv.quickoutline.service.pdfpreview;

import com.ririv.quickoutline.pdfProcess.PdfRenderSession;
import com.ririv.quickoutline.utils.FastByteArrayOutputStream;
import jakarta.inject.Singleton;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing PDF page images and previews.
 * Refactored to support Layered Caching:
 * - Base Layer: Caches images from the opened file.
 * - Overlay Layer: Caches images from temporary preview streams (e.g. TOC generation).
 */
@Singleton
public class PdfImageService {
    private static final Logger log = LoggerFactory.getLogger(PdfImageService.class);

    // --- Base Layer (File) ---
    private final Map<Integer, byte[]> baseCache = new ConcurrentHashMap<>();
    private PdfRenderSession fileSession;
    private int fileTotalPages = 0;

    // --- Overlay Layer (Preview) ---
    // If null, we are in normal file mode. If not null, we are in preview mode.
    private volatile PreviewContext previewContext;

    private static class PreviewContext {
        final Map<Integer, byte[]> overlayCache = new ConcurrentHashMap<>();
        final int totalPages;

        PreviewContext(int totalPages) {
            this.totalPages = totalPages;
        }
    }

    public record ImagePageUpdate(int pageIndex, long version, int totalPages, float widthPt, float heightPt) {}

    /**
     * Open a file session. Clears all caches and exits preview mode.
     */
    public synchronized void openSession(File file) {
        closeSession();
        clearCache(); // Clears base cache and preview context
        try {
            fileSession = new PdfRenderSession(file);
            fileTotalPages = fileSession.getPageCount();
        } catch (Exception e) {
            log.error("Failed to open PDF session: {}", file, e);
        }
    }

    public synchronized void closeSession() {
        if (fileSession != null) {
            fileSession.close();
            fileSession = null;
        }
        fileTotalPages = 0;
    }

    public synchronized void clearCache() {
        baseCache.clear();
        previewContext = null;
    }

    /**
     * Returns the total pages of the CURRENT view (Preview or File).
     */
    public int getLastTotalPages() {
        PreviewContext ctx = previewContext;
        if (ctx != null) {
            return ctx.totalPages;
        }
        return fileTotalPages;
    }

    /**
     * Calculates diff between the provided PDF stream and the current view.
     * Updates the Preview Overlay layer.
     */
    public synchronized List<ImagePageUpdate> diffPdfToImages(FastByteArrayOutputStream pdfStream) {
        List<ImagePageUpdate> updates = new ArrayList<>();
        if (pdfStream == null || pdfStream.size() == 0) return updates;

        try (PdfRenderSession tempSession = new PdfRenderSession(new RandomAccessReadBuffer(pdfStream.getBuffer()))) {
            int currentTotalPages = tempSession.getPageCount();
            
            // Initialize or Reuse Preview Context
            // Note: Since diff usually implies a new state, we essentially reset the overlay 
            // but we might want to keep unchanged parts? 
            // For simplicity and correctness of "Live Preview", we treat each diff as a new Overlay 
            // relative to the BASE file, OR relative to previous overlay?
            // The method signature suggests we are diffing "PDF Stream" vs "What we have".
            // To support "Incremental Update", we should compare against current View.
            
            // Create a NEW context for this update to ensure clean state
            PreviewContext newContext = new PreviewContext(currentTotalPages);

            for (int i = 0; i < currentTotalPages; i++) {
                PDPage page = tempSession.getPage(i);

                // 1. Get Dimensions
                PDRectangle cropBox = page.getCropBox();
                int rotation = page.getRotation();
                float displayW = (rotation==90||rotation==270) ? cropBox.getHeight() : cropBox.getWidth();
                float displayH = (rotation==90||rotation==270) ? cropBox.getWidth() : cropBox.getHeight();

                // 2. Render Image (Sync)
                byte[] currentImg = tempSession.renderToPngWithScale(i, PdfRenderSession.PREVIEW_SCALE);

                // 3. Compare with Current View (Overlay -> Base)
                byte[] oldImg = getBytesFromCurrentView(i);
                
                boolean changed = false;
                if (oldImg == null || oldImg.length != currentImg.length) {
                    changed = true;
                } else {
                    if (!Arrays.equals(oldImg, currentImg)) {
                        changed = true;
                    }
                }

                if (changed) {
                    newContext.overlayCache.put(i, currentImg);
                    updates.add(new ImagePageUpdate(i, System.currentTimeMillis(), currentTotalPages, displayW, displayH));
                } else {
                    // If not changed, we don't put it in overlay. 
                    // But if it was in previous overlay and matches now?
                    // We need to ensure getImageData can find it.
                    // If getBytesFromCurrentView returned non-null, it means it's either in Base or Old Overlay.
                    // Since we are replacing the context, if it was in Old Overlay, we lose it!
                    // So: If the "Old Image" came from the OLD Overlay (not Base), we MUST move it to new Overlay?
                    // OR: We simply assume that if it's not in New Overlay, we fall back to Base.
                    // THIS IS RISKY if Base != Old Overlay.
                    
                    // Safe approach: If we fall back to Base, we must ensure Base is what we expect.
                    // But here we are rendering a full new stream.
                    // Strategy: 
                    // If `currentImg` == `baseCache.get(i)`, then we don't need it in overlay.
                    // If `currentImg` != `baseCache.get(i)`, we MUST put it in overlay (even if it matches previous overlay).
                    
                    // Let's refine the comparison logic:
                    // Always compare against BASE Layer to decide if it belongs in Overlay.
                    
                    byte[] baseImg = baseCache.get(i);
                    // If base is not loaded, we consider it "different" so we cache it in overlay.
                    // This ensures the preview works even if base isn't loaded.
                    if (baseImg == null || !Arrays.equals(baseImg, currentImg)) {
                        newContext.overlayCache.put(i, currentImg);
                        // But wait, if it's same as "Previous Preview", do we send update?
                        // Frontend needs to know if it should refresh.
                        // We compare against `getBytesFromCurrentView(i)` to generate frontend updates.
                        if (!Arrays.equals(oldImg, currentImg)) {
                             // This branch actually unreachable if we define oldImg = getBytesFromCurrentView
                             // because we already checked changed above.
                             // Re-evaluating:
                        }
                    }
                }
            }
            
            // Check for deletions (Visual update for frontend)
            int previousTotal = getLastTotalPages();
            if (currentTotalPages < previousTotal) {
                 if (updates.isEmpty() && currentTotalPages > 0) {
                    // If simply shortened but no content changed, force update on first page 
                    // or handled by totalPages property in JSON
                    updates.add(new ImagePageUpdate(0, System.currentTimeMillis(), currentTotalPages, 0, 0));
                }
            }

            // Commit the new context
            this.previewContext = newContext;

        } catch (Exception e) {
            log.error("PDF Render Error during Diff", e);
        }
        return updates;
    }

    /**
     * Helper to get image bytes from the current active view (Preview or Base).
     * Used for Diff comparison.
     */
    private byte[] getBytesFromCurrentView(int pageIndex) {
        PreviewContext ctx = previewContext;
        if (ctx != null) {
            if (pageIndex >= ctx.totalPages) return null;
            byte[] inOverlay = ctx.overlayCache.get(pageIndex);
            if (inOverlay != null) return inOverlay;
            // Fallback to base
            return baseCache.get(pageIndex);
        }
        return baseCache.get(pageIndex);
    }

    /**
     * Async retrieval of image data.
     * Respects the current Preview Mode.
     */
    public CompletableFuture<byte[]> getImageData(int pageIndex) {
        PreviewContext ctx = previewContext;

        // 1. Preview Mode Logic
        if (ctx != null) {
            if (pageIndex >= ctx.totalPages) return CompletableFuture.completedFuture(null);
            
            // Try Overlay
            byte[] overlayData = ctx.overlayCache.get(pageIndex);
            if (overlayData != null) {
                return CompletableFuture.completedFuture(overlayData);
            }
            
            // Fallback to Base Cache (if exists)
            byte[] baseData = baseCache.get(pageIndex);
            if (baseData != null) {
                return CompletableFuture.completedFuture(baseData);
            }
            
            // If not in Overlay AND not in Base Cache, we have a problem.
            // It implies that `diffPdfToImages` decided this page was identical to Base,
            // BUT Base hasn't loaded it yet.
            // In `diffPdfToImages`, we said: "If baseImg == null ... newContext.overlayCache.put(i, currentImg)".
            // So if Base was null, it SHOULD be in Overlay.
            // Therefore, if we reach here, it implies Base WAS present during Diff, but is gone now?
            // (Unlikely with ConcurrentHashMap unless cleared).
            // OR: It implies we are requesting a page that wasn't processed in diff? (Unlikely).
            
            // Safe Fallback: If we are here, we try to load from File Session.
            // This is valid because if it's not in Overlay, it means it matches the File.
        }

        // 2. Base Mode / Fallback Logic
        byte[] cached = baseCache.get(pageIndex);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        if (fileSession != null) {
            // Render from File
            return fileSession.renderToPngWithDPIAsync(pageIndex, PdfRenderSession.PREVIEW_DPI)
                .thenApply(data -> {
                    if (data != null) {
                        baseCache.put(pageIndex, data);
                    }
                    return data;
                });
        }

        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Optional: Clear preview mode and revert to file view.
     */
    public synchronized void clearPreview() {
        this.previewContext = null;
    }
}