package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.ririv.quickoutline.exception.NoOutlineException;
import com.ririv.quickoutline.model.Bookmark;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ItextOutlineProcessorTest {

    private ItextOutlineProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new ItextOutlineProcessor();
    }

    @Test
    void getOutlineAsBookmark_whenFileDoesNotExist_thenThrowsIOException() {
        String nonExistentFile = "non-existent-file.pdf";
        assertThrows(IOException.class, () -> {
            processor.getOutlineAsBookmark(nonExistentFile, 0);
        });
    }

    /**
     * To run this test, please add a sample PDF with outlines to the
     * src/test/resources/ directory and name it "sample-with-outline.pdf".
     *
     * The expected outline structure should be:
     * - Chapter 1 (Page 1)
     *   - Section 1.1 (Page 2)
     * - Chapter 2 (Page 3)
     */
    @Test
    @Disabled("Requires a sample PDF file: sample-with-outline.pdf")
    void getOutlineAsBookmark_success() throws IOException, NoOutlineException {
        // Arrange
        String samplePdfPath = getClass().getClassLoader().getResource("sample-with-outline.pdf").getPath();

        // Act
        Bookmark root = processor.getOutlineAsBookmark(samplePdfPath, 0);

        // Assert
        assertNotNull(root);
        assertEquals(2, root.getChildren().size());

        Bookmark chapter1 = root.getChildren().get(0);
        assertEquals("Chapter 1", chapter1.getTitle());
        assertEquals(1, chapter1.getPageNum().orElse(-1));
        assertEquals(1, chapter1.getChildren().size());

        Bookmark section1_1 = chapter1.getChildren().get(0);
        assertEquals("Section 1.1", section1_1.getTitle());
        assertEquals(2, section1_1.getPageNum().orElse(-1));
        assertTrue(section1_1.getChildren().isEmpty());

        Bookmark chapter2 = root.getChildren().get(1);
        assertEquals("Chapter 2", chapter2.getTitle());
        assertEquals(3, chapter2.getPageNum().orElse(-1));
        assertTrue(chapter2.getChildren().isEmpty());
    }

    /**
     * To run this test, please add a sample PDF with no outlines to the
     * src/test/resources/ directory and name it "sample-no-outline.pdf".
     */
    @Test
    @Disabled("Requires a sample PDF file: sample-no-outline.pdf")
    void getOutlineAsBookmark_whenNoOutlines_thenThrowsNoOutlineException() {
        String samplePdfPath = getClass().getClassLoader().getResource("sample-no-outline.pdf").getPath();
        assertThrows(NoOutlineException.class, () -> {
            processor.getOutlineAsBookmark(samplePdfPath, 0);
        });
    }
}
