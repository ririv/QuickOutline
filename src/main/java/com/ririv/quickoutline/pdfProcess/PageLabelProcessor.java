package com.ririv.quickoutline.pdfProcess;

import java.io.IOException;
import java.util.List;

/**
 * An interface for processing PDF page labels.
 * This abstraction allows for decoupling the business logic from the underlying PDF library.
 */
public interface PageLabelProcessor {

    /**
     * Sets the page labels for a PDF file.
     *
     * @param srcFilePath  The path to the source PDF file.
     * @param destFilePath The path to write the modified PDF file.
     * @param labelList    A list of PageLabel objects defining the new labeling scheme.
     * @return An array of strings representing the newly set page labels.
     * @throws IOException if an error occurs during file processing.
     */
    String[] setPageLabels(String srcFilePath, String destFilePath, List<PageLabel> labelList) throws IOException;

    /**
     * Gets the existing page labels from a PDF file.
     *
     * @param srcFilePath The path to the PDF file.
     * @return An array of strings representing the page labels.
     * @throws IOException if an error occurs during file processing.
     */
    String[] getPageLabels(String srcFilePath) throws IOException;

}
