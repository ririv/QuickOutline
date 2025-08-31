package com.ririv.quickoutline.state;

import com.google.inject.Inject;
import com.ririv.quickoutline.exception.EncryptedPdfException;
import com.ririv.quickoutline.service.PdfOutlineService;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;

import java.io.IOException;
import java.nio.file.Path;

public class CurrentFileState {

    private final ObjectProperty<Path> srcFileProperty = new SimpleObjectProperty<>();
    private final ReadOnlyObjectWrapper<Path> destFileWrapper;

    private final PdfOutlineService pdfOutlineService;

    @Inject
    public CurrentFileState(PdfOutlineService pdfOutlineService) {
        this.pdfOutlineService = pdfOutlineService;
        destFileWrapper = new ReadOnlyObjectWrapper<>();
        destFileWrapper.bind(Bindings.createObjectBinding(() -> {
            Path src = srcFileProperty.get();
            if (src == null) {
                return null;
            }
            return calculateDestFilePath(src);
        }, srcFileProperty));
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
        pdfOutlineService.checkOpenFile(file.toString());
        this.srcFileProperty.set(file);
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
}
