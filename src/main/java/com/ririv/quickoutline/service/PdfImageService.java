package com.ririv.quickoutline.service;

import jakarta.inject.Singleton;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class PdfImageService {
    private static final Logger log = LoggerFactory.getLogger(PdfImageService.class);

    // 缓存：Key=页码, Value=PNG字节
    private final Map<Integer, byte[]> pageCache = new HashMap<>();
    private int lastTotalPages = 0;

    // 2.0f = Retina 级别清晰度 (1pt = 2px)
    // 如果觉得还不够清晰，可以设为 2.5f 或 3.0f，但体积会变大
    private static final float RENDER_SCALE = 2.0f;

    // 数据载体
    // version: 用于前端强制刷新缓存 (利用时间戳)
    public record ImagePageUpdate(int pageIndex, long version, int totalPages, float widthPx, float heightPx) {}

    public void clearCache() {
        pageCache.clear();
        lastTotalPages = 0;
    }

    public List<ImagePageUpdate> diffPdfToImages(byte[] pdfBytes) {
        List<ImagePageUpdate> updates = new ArrayList<>();
        if (pdfBytes == null || pdfBytes.length == 0) return updates;

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            int currentTotalPages = document.getNumberOfPages();
            PDFRenderer renderer = new PDFRenderer(document);

            for (int i = 0; i < currentTotalPages; i++) {
                PDPage page = document.getPage(i);

                // 1. 获取尺寸 (逻辑同前)
                PDRectangle cropBox = page.getCropBox();
                int rotation = page.getRotation();
                float displayW = (rotation==90||rotation==270) ? cropBox.getHeight() : cropBox.getWidth();
                float displayH = (rotation==90||rotation==270) ? cropBox.getWidth() : cropBox.getHeight();

                // 2. 渲染图片 (耗时操作)
                byte[] currentImg = renderPageToPng(renderer, i);

                // 3. 简单字节对比 (性能足够，因为图片生成是确定的)
                // 优化：如果上一帧图片存在，且长度一致，大概率没变。
                // 严格对比：Arrays.equals(cache, current)
                byte[] cachedImg = pageCache.get(i);

                boolean changed = false;
                if (cachedImg == null || cachedImg.length != currentImg.length) {
                    changed = true;
                } else {
                    // 长度一样再比对内容，提升性能
                    // 实际生产中，PDFBox 只要输入不变，输出的 PNG 字节流通常是稳定的
                    // 如果为了极致性能，可以只比对长度，或者这里忽略深度比对
                    // 更加保险的做法：总是更新 (反正前端有预加载，不会闪)
                    // 但为了节省带宽，我们还是对比一下
                    if (!java.util.Arrays.equals(cachedImg, currentImg)) {
                        changed = true;
                    }
                }

                if (changed) {
                    pageCache.put(i, currentImg);
                    // version 使用当前时间戳，确保前端 URL 变化
                    updates.add(new ImagePageUpdate(i, System.currentTimeMillis(), currentTotalPages, displayW, displayH));
                }
            }

            // 处理删页
            if (currentTotalPages < lastTotalPages) {
                for (int k = currentTotalPages; k < lastTotalPages; k++) pageCache.remove(k);
                if (updates.isEmpty() && currentTotalPages > 0) {
                    // 强制刷新第一页以重置容器
                    updates.add(new ImagePageUpdate(0, System.currentTimeMillis(), currentTotalPages, 0, 0)); // 宽高在前端已有缓存，此时0可能需要处理
                    // 修正：还是传正确宽高比较好，略
                }
            }
            lastTotalPages = currentTotalPages;

        } catch (Exception e) {
            log.error("PDF Render Error", e);
        }
        return updates;
    }

    private byte[] renderPageToPng(PDFRenderer renderer, int pageIndex) throws Exception {
        // 渲染为 BufferedImage
        BufferedImage image = renderer.renderImage(pageIndex, RENDER_SCALE, ImageType.RGB);
        // 转为 PNG 字节流
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    // 提供给 WebServer 获取图片的方法
    public byte[] getImageData(int pageIndex) {
        return pageCache.get(pageIndex);
    }
}
