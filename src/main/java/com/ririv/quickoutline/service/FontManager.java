package com.ririv.quickoutline.service;

import com.itextpdf.layout.font.FontProvider;
import com.itextpdf.layout.font.FontSet;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Singleton
public class FontManager {

    private static final Logger log = LoggerFactory.getLogger(FontManager.class);

    private static final class FontSource {
        final String baseUrl;
        final List<String> files;

        FontSource(String baseUrl, List<String> files) {
            this.baseUrl = baseUrl;
            this.files = files;
        }
    }

    // 使用思源黑体 (Source Han Sans) 作为主力字体
    private static final FontSource SOURCE_HAN_SC = new FontSource(
            "https://raw.githubusercontent.com/adobe-fonts/source-han-sans/release/OTF/SimplifiedChinese/",
            List.of("SourceHanSansSC-Regular.otf", "SourceHanSansSC-Bold.otf")
    );

    private final Path fontDir;
    private final List<Path> fontPaths = new ArrayList<>();
    private final FontSource fontSource = SOURCE_HAN_SC;

    // 标记是否已下载完成
    private volatile boolean areFontsInitialized = false;

    // 缓存 AWT 字体对象 (用于 SVG 预览)
    private Font cachedAwtFont;

    public FontManager() {
        String userHome = System.getProperty("user.home");
        // 字体存放路径: ~/.quickoutline/fonts
        this.fontDir = Paths.get(userHome, ".quickoutline", "fonts");

        // 预计算本地路径
        for (String fileName : fontSource.files) {
            fontPaths.add(fontDir.resolve(fileName));
        }
    }

    /**
     * 获取 iText 的 FontProvider (用于 PDF 生成)
     */
    public FontProvider getFontProvider(Consumer<DownloadEvent> onEvent) {
        // 1. 确保字体文件存在
        try {
            ensureFontsAreAvailable(onEvent);
        } catch (IOException e) {
            log.error("Failed to download fonts, falling back to empty provider", e);
        }

        // 2. 创建标准的 FontSet
        FontSet fontSet = new FontSet();

        // 3. 添加字体
        for (Path fontPath : fontPaths) {
            try {
                if (Files.exists(fontPath)) {
                    fontSet.addFont(fontPath.toString());
                    log.info("Added font to iText provider: {}", fontPath);
                }
            } catch (Exception e) {
                log.error("Failed to add font: " + fontPath, e);
            }
        }

        // 4. 返回 Provider (默认字体设为 Helvetica 以防万一)
        return new FontProvider(fontSet, "Helvetica");
    }

    /**
     * 【新增】获取 Java AWT 字体对象 (用于 SVG 预览渲染)
     * 优先加载 Regular 版本，确保预览时的字宽与 PDF 生成时的一致
     */
    public Font getAwtFont() {
        if (cachedAwtFont != null) return cachedAwtFont;

        // 尝试加载 Regular 字体
        Path regularFontPath = fontDir.resolve("SourceHanSansSC-Regular.otf");

        // 如果还没下载，先尝试下载一次(不带回调)
        if (!Files.exists(regularFontPath)) {
            try {
                ensureFontsAreAvailable(null);
            } catch (IOException e) {
                log.warn("Could not download fonts for AWT, using fallback.");
            }
        }

        if (Files.exists(regularFontPath)) {
            try {
                // 从文件加载字体
                // 注意：TRUETYPE_FONT 常量也支持 OpenType (OTF)
                Font font = Font.createFont(Font.TRUETYPE_FONT, regularFontPath.toFile());

                // 注册到本地图形环境 (虽然我们主要用 deriveFont，但注册一下更稳妥)
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(font);

                this.cachedAwtFont = font;
                log.info("Loaded AWT Font for preview: {}", regularFontPath);
                return font;
            } catch (Exception e) {
                log.error("Failed to load AWT font from disk.", e);
            }
        }

        // 兜底：如果加载失败，返回系统默认 SansSerif
        // 这会导致预览和导出有轻微错位，但至少能显示
        log.warn("Using system SansSerif font as fallback.");
        return new Font("SansSerif", Font.PLAIN, 12);
    }

    /**
     * 下载/校验字体文件
     */
    public void ensureFontsAreAvailable(Consumer<DownloadEvent> onEvent) throws IOException {
        if (areFontsInitialized) return;

        synchronized (this) {
            if (areFontsInitialized) return;

            if (!Files.exists(fontDir)) {
                Files.createDirectories(fontDir);
            }

            for (String fontFileName : fontSource.files) {
                Path fontPath = fontDir.resolve(fontFileName);

                // 如果文件已存在，跳过
                if (Files.exists(fontPath)) {
                    log.debug("Font already exists: {}", fontPath);
                    continue;
                }

                if (onEvent != null) onEvent.accept(new DownloadEvent(DownloadEvent.Type.START, fontFileName, null));

                log.info("Downloading font: {}", fontFileName);
                Path tempPath = fontDir.resolve(fontFileName + ".tmp");
                String fontUrl = fontSource.baseUrl + fontFileName;

                try (InputStream in = URI.create(fontUrl).toURL().openStream()) {
                    Files.copy(in, tempPath, StandardCopyOption.REPLACE_EXISTING);
                    Files.move(tempPath, fontPath, StandardCopyOption.REPLACE_EXISTING);

                    if (onEvent != null) onEvent.accept(new DownloadEvent(DownloadEvent.Type.SUCCESS, fontFileName, null));
                } catch (IOException e) {
                    if (onEvent != null) onEvent.accept(new DownloadEvent(DownloadEvent.Type.ERROR, fontFileName, e.getMessage()));
                    // 清理临时文件
                    try { Files.deleteIfExists(tempPath); } catch (IOException ignored) {}
                    throw e;
                }
            }
            areFontsInitialized = true;
        }
    }

}