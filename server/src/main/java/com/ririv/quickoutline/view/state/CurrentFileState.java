package com.ririv.quickoutline.view.state;

import com.ririv.quickoutline.service.PdfCheckService;
import jakarta.inject.Inject;
import com.ririv.quickoutline.exception.EncryptedPdfException;
import com.ririv.quickoutline.pdfProcess.PdfRenderSession;
import com.ririv.quickoutline.service.PdfOutlineService;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;

import java.io.IOException;
import java.nio.file.Path;

public class CurrentFileState implements AutoCloseable {

    private final ObjectProperty<Path> srcFileProperty = new SimpleObjectProperty<>();
    private final ReadOnlyObjectWrapper<Path> destFileWrapper;
    // Per-current-file PageRenderSession shared across components
    private final ObjectProperty<PdfRenderSession> pageRenderSessionProperty = new SimpleObjectProperty<>();

    private final PdfCheckService pdfCheckService;

    @Inject
    public CurrentFileState(PdfCheckService pdfCheckService) {
        this.pdfCheckService = pdfCheckService;
        destFileWrapper = new ReadOnlyObjectWrapper<>();
        destFileWrapper.bind(Bindings.createObjectBinding(() -> {
            Path src = srcFileProperty.get();
            if (src == null) {
                return null;
            }
            return calculateDestFilePath(src);
        }, srcFileProperty));

        // Maintain a shared PageRenderSession lifecycle bound to the current src file
        srcFileProperty.addListener((obs, oldPath, newPath) -> {
            // Close old session immediately on FX thread
            PdfRenderSession oldSession = pageRenderSessionProperty.get();
            if (oldSession != null) {
                try {
                    oldSession.close();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    pageRenderSessionProperty.set(null);
                }
            }
            if (newPath == null) {
                return;
            }
            // Build new session off the FX thread, then publish on FX thread if src hasn't changed
            final Path targetPath = newPath;
            Thread t = new Thread(() -> {
                PdfRenderSession session = null;
                try {
                    session = new PdfRenderSession(targetPath.toFile());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                final PdfRenderSession ready = session;
                javafx.application.Platform.runLater(() -> {
                    // Ensure srcFile not changed since we started
                    if (targetPath.equals(srcFileProperty.get())) {
                        // Close previously set session again just in case
                        PdfRenderSession prev = pageRenderSessionProperty.get();
                        if (prev != null && prev != ready) {
                            try { prev.close(); } catch (Exception ignore) {}
                        }
                        pageRenderSessionProperty.set(ready);
                    } else {
                        // Stale session
                        if (ready != null) {
                            try { ready.close(); } catch (Exception ignore) {}
                        }
                    }
                });
            }, "create-page-render-session");
            t.setDaemon(true);
            t.start();
        });
    }

    public ObjectProperty<Path> srcFileProperty() {
        return srcFileProperty;
    }

    public ReadOnlyObjectProperty<Path> getDestFileProperty() {
        return destFileWrapper.getReadOnlyProperty();
    }

    public Path getSrcFile() {
        return srcFileProperty.get();
    }

    public Path getDestFile() {
        return destFileWrapper.get();
    }

    public void setSrcFile(Path file) throws IOException, EncryptedPdfException, com.itextpdf.io.exceptions.IOException {
        if (file == null) {
            clear();
            return;
        }
        // Centralized validation
        pdfCheckService.checkOpenFile(file.toString());
        this.srcFileProperty.set(file);
    }

    /**
     * Validate a file can be opened. Intended for background threads.
     */
    public void validateFile(Path file) throws IOException, EncryptedPdfException, com.itextpdf.io.exceptions.IOException {
        if (file == null) return;
        pdfCheckService.checkOpenFile(file.toString());
    }

    /**
     * Set src file after it has been validated. Call on JavaFX thread.
     */
    public void setSrcFileValidated(Path file) {
        if (!javafx.application.Platform.isFxApplicationThread()) {
            javafx.application.Platform.runLater(() -> setSrcFileValidated(file));
            return;
        }
        if (file == null) {
            clear();
            return;
        }
        this.srcFileProperty.set(file);
    }

    public ReadOnlyObjectProperty<PdfRenderSession> pageRenderSessionProperty() {
        return pageRenderSessionProperty;
    }

    public PdfRenderSession getPageRenderSession() {
        return pageRenderSessionProperty.get();
    }

    public Path calculateDestFilePath(Path srcFilePath) {
        if (srcFilePath == null) {
            return null;
        }
        String srcFileName = srcFilePath.getFileName().toString();
        int dotIndex = srcFileName.lastIndexOf(".");
        String nameWithoutExt = (dotIndex == -1) ? srcFileName : srcFileName.substring(0, dotIndex);
        String ext = (dotIndex == -1) ? "" : srcFileName.substring(dotIndex);

        Path parentDir = srcFilePath.getParent();
        String destFileName = nameWithoutExt + "_new" + ext;
        return parentDir.resolve(destFileName);
    }

    public void clear() {
        this.srcFileProperty.set(null);
    }

    @Override
    public void close() {
        PdfRenderSession session = pageRenderSessionProperty.get();
        if (session != null) {
            try {
                session.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                pageRenderSessionProperty.set(null);
            }
        }
    }
}
