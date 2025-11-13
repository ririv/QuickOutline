package com.ririv.quickoutline.service;



import com.itextpdf.layout.font.FontProvider;
import com.itextpdf.styledxmlparser.resolver.font.BasicFontProvider;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Singleton
public class FontManager {

    private static final String FONT_BASE_URL = "https://raw.githubusercontent.com/adobe-fonts/source-han-sans/release/OTF/SimplifiedChinese/";
    private static final List<String> FONT_FILES = List.of(
            "SourceHanSansSC-Regular.otf",
            "SourceHanSansSC-Bold.otf"
    );

    private final Path fontDir;
    private final List<Path> fontPaths = new ArrayList<>();

    private volatile boolean areFontsInitialized = false;

    public FontManager() {
        String userHome = System.getProperty("user.home");
        this.fontDir = Paths.get(userHome, ".quickoutline", "fonts");
        FONT_FILES.forEach(fileName -> fontPaths.add(fontDir.resolve(fileName)));
    }

    public Optional<FontProvider> getFontProvider(Consumer<String> onMessage, Consumer<String> onError) {
        try {
            List<Path> paths = getFontPaths(onMessage, onError);
            BasicFontProvider fontProvider = new BasicFontProvider(false, false, false);
            for (Path fontPath : paths) {
                fontProvider.addFont(fontPath.toString());
            }
            return Optional.of(fontProvider);
        } catch (IOException e) {
            onError.accept("Failed to get font provider: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Gets the paths to the required fonts. If any font is not present locally,
     * it will be downloaded and cached first. This is a blocking operation.
     *
     * @param onMessage Callback for informational messages.
     * @param onError   Callback for error messages.
     * @return A list of paths to the font files.
     * @throws IOException if a font cannot be downloaded or accessed.
     */
    public List<Path> getFontPaths(Consumer<String> onMessage, Consumer<String> onError) throws IOException {
        if (!areFontsInitialized) {
            synchronized (this) {
                if (!areFontsInitialized) {
                    ensureFontsAreAvailable(onMessage, onError);
                    areFontsInitialized = true;
                }
            }
        }
        return fontPaths;
    }

    private void ensureFontsAreAvailable(Consumer<String> onMessage, Consumer<String> onError) throws IOException {
        Files.createDirectories(fontDir);

        for (String fontFileName : FONT_FILES) {
            Path fontPath = fontDir.resolve(fontFileName);
            if (Files.exists(fontPath)) {
                continue; // Font is already available
            }

            onMessage.accept("正在下载字体: " + fontFileName + "...");
            Path tempPath = fontDir.resolve(fontFileName + ".tmp");
            String fontUrl = FONT_BASE_URL + fontFileName;

            try {
                try (InputStream in = URI.create(fontUrl).toURL().openStream()) {
                    Files.copy(in, tempPath, StandardCopyOption.REPLACE_EXISTING);
                }
                Files.move(tempPath, fontPath, StandardCopyOption.REPLACE_EXISTING);
                onMessage.accept("字体 " + fontFileName + " 下载完成。");

            } catch (IOException e) {
                onError.accept("字体下载失败: " + fontFileName + " - " + e.getMessage());
                throw new IOException("Failed to download or save the font file: " + fontFileName, e);
            }
        }
    }
}
