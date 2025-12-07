package com.ririv.quickoutline.service.pdfpreview;

import com.ririv.quickoutline.pdfProcess.PdfRenderSession;
import com.ririv.quickoutline.utils.FastByteArrayOutputStream;
import jakarta.inject.Singleton;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service dedicated to managing images derived from temporary preview streams (e.g. TOC generation).
 * Lifecycle: Transient, exists only during a preview operation.
 */
@Singleton
public class PreviewImageService {
    private static final Logger log = LoggerFactory.getLogger(PreviewImageService.class);

    private final Map<Integer, byte[]> cache = new ConcurrentHashMap<>();
    private volatile boolean active = false;
    private int totalPages = 0;

    /**
     * Updates the preview state with a new PDF stream.
     * Renders images and calculates diffs against the PREVIOUS preview state.
     */
    public synchronized List<ImagePageUpdate> updatePreview(FastByteArrayOutputStream pdfStream) {
        List<ImagePageUpdate> updates = new ArrayList<>();
        if (pdfStream == null || pdfStream.size() == 0) return updates;

        active = true;
        int previousTotalPages = this.totalPages;

        try (PdfRenderSession tempSession = new PdfRenderSession(new RandomAccessReadBuffer(pdfStream.getBuffer()))) {
            int currentTotalPages = tempSession.getPageCount();
            this.totalPages = currentTotalPages;

            for (int i = 0; i < currentTotalPages; i++) {
                PDPage page = tempSession.getPage(i);

                // 1. Get Dimensions
                PDRectangle cropBox = page.getCropBox();
                int rotation = page.getRotation();
                float displayW = (rotation==90||rotation==270) ? cropBox.getHeight() : cropBox.getWidth();
                float displayH = (rotation==90||rotation==270) ? cropBox.getWidth() : cropBox.getHeight();

                // 2. Render Image
                byte[] currentImg = tempSession.renderToPngWithScale(i, PdfRenderSession.PREVIEW_SCALE);

                // 3. Compare with Cache (Previous Preview)
                // We simplify by comparing only against what we currently hold in cache.
                // If it's the first time (cache empty), everything is an update.
                byte[] oldImg = cache.get(i);
                
                boolean changed = oldImg == null || !Arrays.equals(oldImg, currentImg);

                if (changed) {
                    cache.put(i, currentImg);
                    updates.add(new ImagePageUpdate(i, System.currentTimeMillis(), currentTotalPages, displayW, displayH));
                }
            }

            // Handle page count reduction
            if (currentTotalPages < previousTotalPages) {
                // Remove stale pages from cache
                for (int k = currentTotalPages; k < previousTotalPages; k++) {
                    cache.remove(k);
                }
                // Notify frontend if only page count changed but no content changed (rare)
                if (updates.isEmpty() && currentTotalPages > 0) {
                    updates.add(new ImagePageUpdate(0, System.currentTimeMillis(), currentTotalPages, 0, 0));
                }
            }

        } catch (Exception e) {
            log.error("Preview Render Error", e);
        }
        return updates;
    }

    public void clear() {
        active = false;
        cache.clear();
        totalPages = 0;
    }

    public boolean isActive() {
        return active;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public CompletableFuture<byte[]> getImage(int pageIndex) {
        if (!active || pageIndex >= totalPages) {
            return CompletableFuture.completedFuture(null);
        }
        byte[] data = cache.get(pageIndex);
        return CompletableFuture.completedFuture(data);
    }
}
