package com.ririv.quickoutline.pdfProcess;

import com.ririv.quickoutline.service.DownloadEvent;

import java.io.OutputStream;
import java.util.function.Consumer;

/**
 * An interface for converting HTML content to a PDF.
 * This abstraction allows for swapping out the underlying PDF generation library.
 */
public interface HtmlConverter {

    /**
     * Converts the given HTML string into a PDF and writes it to the provided OutputStream.
     *
     * @param html         The HTML content as a string.
     * @param baseUri      Base URI used to resolve relative resources such as images.
     *                     For example: {@code file:///path/to/pdf/dir/}.
     * @param outputStream The stream to write the resulting PDF to.
    * @param onEvent      Callback for download/infrastructure events (e.g. font download).
     * @throws Exception if the conversion fails.
     */
    void convertToPdf(String html, String baseUri, OutputStream outputStream,
               Consumer<DownloadEvent> onEvent);

}
