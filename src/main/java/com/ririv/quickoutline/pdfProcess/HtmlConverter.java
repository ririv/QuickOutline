package com.ririv.quickoutline.pdfProcess;

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
     * @param html The HTML content as a string.
     * @param outputStream The stream to write the resulting PDF to.
     * @param onMessage Callback for informational messages.
     * @param onError Callback for error messages.
     * @throws Exception if the conversion fails.
     */
     void convertToPdf(String html, OutputStream outputStream, Consumer<String> onMessage, Consumer<String> onError) throws Exception;

}
