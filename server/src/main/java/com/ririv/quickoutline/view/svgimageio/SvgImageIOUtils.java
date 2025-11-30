package com.ririv.quickoutline.view.svgimageio;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * Helper utilities for loading SVG images as JavaFX Image using the SVG ImageIO plugin.
 */
public final class SvgImageIOUtils {

    private static volatile boolean registered = false;

    private SvgImageIOUtils() {
    }

    /**
     * Register the SvgImageReaderSpi with the default IIORegistry if not already registered.
     */
    public static void registerIfNeeded() {
        if (!registered) {
            synchronized (SvgImageIOUtils.class) {
                if (!registered) {
                    IIORegistry registry = IIORegistry.getDefaultInstance();
                    registry.registerServiceProvider(new SvgImageReaderSpi());
                    registered = true;
                }
            }
        }
    }

    /**
     * Load an SVG resource from the classpath as a JavaFX Image.
     *
     * @param resourcePath classpath resource path, e.g. "/drawable/open.svg"
     * @param targetWidth  desired render width in pixels (use &lt;= 0 to let SVG decide)
     * @param targetHeight desired render height in pixels (use &lt;= 0 to let SVG decide)
     */
    public static Image loadSvgAsFxImage(String resourcePath, double targetWidth, double targetHeight) {
        registerIfNeeded();

        try (InputStream is = SvgImageIOUtils.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("SVG resource not found: " + resourcePath);
            }

            try (ImageInputStream iis = javax.imageio.ImageIO.createImageInputStream(is)) {
                if (iis == null) {
                    throw new IOException("Failed to create ImageInputStream for: " + resourcePath);
                }

                Iterator<ImageReader> readers = ImageIO.getImageReadersByMIMEType("image/svg+xml");
                if (!readers.hasNext()) {
                    readers = ImageIO.getImageReadersBySuffix("svg");
                }
                if (!readers.hasNext()) {
                    throw new IOException("No SVG ImageReader available. Did registration fail?");
                }

                ImageReader reader = readers.next();
                try {
                    reader.setInput(iis, true, true);
                    ImageReadParam param = reader.getDefaultReadParam();
                    if (targetWidth > 0 && targetHeight > 0) {
                        param.setSourceRenderSize(new Dimension((int) Math.round(targetWidth), (int) Math.round(targetHeight)));
                    }
                    BufferedImage buffered = reader.read(0, param);
                    return SwingFXUtils.toFXImage(buffered, null);
                } finally {
                    reader.dispose();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load SVG as Image: " + resourcePath, e);
        }
    }


    /**
     * Load an SVG resource from the classpath as a JavaFX Image using the
     * intrinsic size defined by the SVG itself (no explicit target size).
     * <p>
     * 用于调用方不关心具体像素尺寸、仅希望按照 SVG 内部 viewBox / width / height 渲染时使用。
     * <p>
     * 等价于调用 {@link #loadSvgAsFxImage(String, double, double)} 并传入
     * 非正的宽高，让底层不设置 sourceRenderSize，由 Batik / SVG 自己决定尺寸。
     *
     * @param resourcePath classpath resource path, e.g. "/drawable/open.svg"
     * @return JavaFX Image rendered from the SVG.
     */
    public static Image loadSvgAsFxImage(String resourcePath) {
        // 传递 <= 0 的宽高，意味着“不强制设置 sourceRenderSize”，
        // SvgImageReader 会按 SVG 自身的尺寸信息进行栅格化。
        return loadSvgAsFxImage(resourcePath, -1, -1);
    }
}
