package com.ririv.quickoutline.service;



import com.itextpdf.layout.font.FontProvider;
import com.itextpdf.styledxmlparser.resolver.font.BasicFontProvider;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Singleton
public class FontManager {

    private static final Logger log = LoggerFactory.getLogger(FontManager.class);

    /**
     * Simple font source descriptor, so we can support different font sets in future.
     */
    private static final class FontSource {
        final String baseUrl;
        final List<String> files;

        FontSource(String baseUrl, List<String> files) {
            this.baseUrl = baseUrl;
            this.files = files;
        }
    }

    // Current default: Source Han Sans SC (Simplified Chinese)
    private static final FontSource SOURCE_HAN_SC = new FontSource(
            "https://raw.githubusercontent.com/adobe-fonts/source-han-sans/release/OTF/SimplifiedChinese/",
            List.of(
                    "SourceHanSansSC-Regular.otf",
                    "SourceHanSansSC-Bold.otf"
            )
    );

    private final Path fontDir;
    private final List<Path> fontPaths = new ArrayList<>();
    private final FontSource fontSource = SOURCE_HAN_SC;

    private volatile boolean areFontsInitialized = false;

    public FontManager() {
        String userHome = System.getProperty("user.home");
        this.fontDir = Paths.get(userHome, ".quickoutline", "fonts");
        // Precompute local font paths for the configured font source
        for (String fileName : fontSource.files) {
            fontPaths.add(fontDir.resolve(fileName));
        }
        loadBundleFont();
    }

    public FontProvider getFontProvider(Consumer<DownloadEvent> onEvent) throws IOException {
        List<Path> paths = getFontPaths(onEvent);
        BasicFontProvider fontProvider = new BasicFontProvider(false, false, false);
        for (Path fontPath : paths) {
            fontProvider.addFont(fontPath.toString());
        }
        return fontProvider;
    }


    public void loadBundleFont() {
        // Add fonts from the resources
        try {
            var fontFolder = FontManager.class.getResource("/web/fonts");
            if (fontFolder != null) {
                Path fontPath = Paths.get(fontFolder.toURI());
                try (Stream<Path> paths = Files.walk(fontPath)) {
                    paths.filter(Files::isRegularFile).forEach(file -> {
                        log.debug("add font: {}", file);
                        fontPaths.add(file);
                    });
                }
            }
        } catch (URISyntaxException | IOException e) {
            log.error("Failed to load fonts from resources", e);
        }
    }

    /**
     * Gets the paths to the required fonts. If any font is not present locally,
     * it will be downloaded and cached first. This is a blocking operation.
     *
     * @param onEvent   Callback for download events.
     * @return A list of paths to the font files.
     * @throws IOException if a font cannot be downloaded or accessed.
     */
    public List<Path> getFontPaths(Consumer<DownloadEvent> onEvent) throws IOException {
        if (!areFontsInitialized) {
            synchronized (this) {
                if (!areFontsInitialized) {
                    ensureFontsAreAvailable(onEvent);
                    areFontsInitialized = true;
                }
            }
        }
        return fontPaths;
    }

    private void ensureFontsAreAvailable(Consumer<DownloadEvent> onEvent) throws IOException {
        Files.createDirectories(fontDir);

        for (String fontFileName : fontSource.files) {
            Path fontPath = fontDir.resolve(fontFileName);
            if (Files.exists(fontPath)) {
                continue; // Font is already available
            }

            onEvent.accept(new DownloadEvent(DownloadEvent.Type.START, fontFileName, null));
            Path tempPath = fontDir.resolve(fontFileName + ".tmp");
            String fontUrl = fontSource.baseUrl + fontFileName;

            try {
                try (InputStream in = URI.create(fontUrl).toURL().openStream()) {
                    Files.copy(in, tempPath, StandardCopyOption.REPLACE_EXISTING);
                }
                Files.move(tempPath, fontPath, StandardCopyOption.REPLACE_EXISTING);
                onEvent.accept(new DownloadEvent(DownloadEvent.Type.SUCCESS, fontFileName, null));

            } catch (IOException e) {
                onEvent.accept(new DownloadEvent(DownloadEvent.Type.ERROR, fontFileName, e.getMessage()));
                throw new IOException("Failed to download or save the font file: " + fontFileName, e);
            }
        }
    }
}
