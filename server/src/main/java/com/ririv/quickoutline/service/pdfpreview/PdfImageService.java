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

@Singleton
public class PdfImageService {
    private static final Logger log = LoggerFactory.getLogger(PdfImageService.class);

    // 缓存：Key=页码, Value=PNG字节
    private final Map<Integer, byte[]> pageCache = new ConcurrentHashMap<>();
    private int lastTotalPages = 0;
    
    private PdfRenderSession currentSession;

    public record ImagePageUpdate(int pageIndex, long version, int totalPages, float widthPt, float heightPt) {}

    public void openSession(File file) {
        closeSession();
        clearCache();
        try {
            currentSession = new PdfRenderSession(file);
            lastTotalPages = currentSession.getPageCount();
        } catch (Exception e) {
            log.error("Failed to open PDF session: {}", file, e);
        }
    }

    public void closeSession() {
        if (currentSession != null) {
            currentSession.close();
            currentSession = null;
        }
    }

    public void clearCache() {
        pageCache.clear();
        lastTotalPages = 0;
    }

    public List<ImagePageUpdate> diffPdfToImages(FastByteArrayOutputStream pdfStream) {
        List<ImagePageUpdate> updates = new ArrayList<>();
        if (pdfStream == null || pdfStream.size() == 0) return updates;

        try (PdfRenderSession tempSession = new PdfRenderSession(new RandomAccessReadBuffer(pdfStream.getBuffer()))) {
            int currentTotalPages = tempSession.getPageCount();

            for (int i = 0; i < currentTotalPages; i++) {
                PDPage page = tempSession.getPage(i);

                // 1. 获取尺寸
                PDRectangle cropBox = page.getCropBox();
                int rotation = page.getRotation();
                float displayW = (rotation==90||rotation==270) ? cropBox.getHeight() : cropBox.getWidth();
                float displayH = (rotation==90||rotation==270) ? cropBox.getWidth() : cropBox.getHeight();

                // 2. 渲染图片 (同步) - Use Scale 2.0 (PREVIEW_SCALE)
                byte[] currentImg = tempSession.renderToPngWithScale(i, PdfRenderSession.PREVIEW_SCALE);

                // 3. 对比
                byte[] cachedImg = pageCache.get(i);
                boolean changed = false;
                if (cachedImg == null || cachedImg.length != currentImg.length) {
                    changed = true;
                } else {
                    if (!Arrays.equals(cachedImg, currentImg)) {
                        changed = true;
                    }
                }

                if (changed) {
                    pageCache.put(i, currentImg);
                    updates.add(new ImagePageUpdate(i, System.currentTimeMillis(), currentTotalPages, displayW, displayH));
                }
            }

            // 处理删页
            if (currentTotalPages < lastTotalPages) {
                for (int k = currentTotalPages; k < lastTotalPages; k++) pageCache.remove(k);
                if (updates.isEmpty() && currentTotalPages > 0) {
                    updates.add(new ImagePageUpdate(0, System.currentTimeMillis(), currentTotalPages, 0, 0));
                }
            }
            lastTotalPages = currentTotalPages;

        } catch (Exception e) {
            log.error("PDF Render Error", e);
        }
        return updates;
    }

    public CompletableFuture<byte[]> getImageData(int pageIndex) {
        byte[] cached = pageCache.get(pageIndex);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        if (currentSession != null) {
            return currentSession.renderToPngWithDPIAsync(pageIndex, PdfRenderSession.PREVIEW_DPI)
                .thenApply(data -> {
                    if (data != null) {
                        pageCache.put(pageIndex, data);
                    }
                    return data;
                });
        }

        return CompletableFuture.completedFuture(null);
    }
}
