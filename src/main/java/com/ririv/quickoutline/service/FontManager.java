package com.ririv.quickoutline.service;

import com.itextpdf.layout.font.FontProvider;
import com.itextpdf.layout.font.FontSet;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    // 使用思源黑体 (Source Han Sans) 作为主力字体，它极其稳定且包含所有生僻字
    private static final FontSource SOURCE_HAN_SC = new FontSource(
            "https://raw.githubusercontent.com/adobe-fonts/source-han-sans/release/OTF/SimplifiedChinese/",
            List.of("SourceHanSansSC-Regular.otf", "SourceHanSansSC-Bold.otf")
    );

    private final Path fontDir;
    private final List<Path> fontPaths = new ArrayList<>();
    private final FontSource fontSource = SOURCE_HAN_SC;

    private volatile boolean areFontsInitialized = false;

    public FontManager() {
        String userHome = System.getProperty("user.home");
        // 建议放在 .quickoutline/fonts 下
        this.fontDir = Paths.get(userHome, ".quickoutline", "fonts");

        // 预计算本地路径
        for (String fileName : fontSource.files) {
            fontPaths.add(fontDir.resolve(fileName));
        }

        // 暂时注释掉 loadBundleFont，因为 Files.walk 无法遍历 JAR 包内的资源
        // 如果需要打包内置字体，建议用 LocalWebServer 那种 getResourceAsStream 的方式读取流
        // loadBundleFont();
    }

    /**
     * 获取配置好的 FontProvider
     * 这里的修改重点：使用 com.itextpdf.layout.font.FontProvider 而不是 BasicFontProvider
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
                    log.info("Added font to provider: {}", fontPath);
                }
            } catch (Exception e) {
                log.error("Failed to add font: " + fontPath, e);
            }
        }

        // 4. 返回 Provider (默认字体设为 Helvetica 以防万一)
        return new FontProvider(fontSet, "Helvetica");
    }

    // 下载逻辑保持不变
    public void ensureFontsAreAvailable(Consumer<DownloadEvent> onEvent) throws IOException {
        if (areFontsInitialized) return;

        synchronized (this) {
            if (areFontsInitialized) return;

            Files.createDirectories(fontDir);

            for (String fontFileName : fontSource.files) {
                Path fontPath = fontDir.resolve(fontFileName);
                if (Files.exists(fontPath)) {
                    log.info("Font already exists: {}", fontPath);
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
                    throw e; // 抛出异常让上层知道
                }
            }
            areFontsInitialized = true;
        }
    }

}