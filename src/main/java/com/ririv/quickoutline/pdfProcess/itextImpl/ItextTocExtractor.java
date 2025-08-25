package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor;
import com.ririv.quickoutline.pdfProcess.TocExtractor;
import com.ririv.quickoutline.pdfProcess.itextImpl.model.LineWithMetadata;
import com.ririv.quickoutline.pdfProcess.itextImpl.model.Style;
import com.ririv.quickoutline.pdfProcess.itextImpl.model.TextChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.ririv.quickoutline.pdfProcess.itextImpl.model.TextChunk.convertChunksToLines;

public class ItextTocExtractor implements TocExtractor {

    private static final Logger log = LoggerFactory.getLogger(ItextTocExtractor.class);
    private static final Pattern TOC_DOT_PATTERN = Pattern.compile(".*([.]\\s*|\\s{2,}){4,}\\s*\\d+\\s*$");
    private static final Pattern TOC_NUMERIC_END_PATTERN = Pattern.compile("^(.*[^\\d])\\s+(\\d+)\\s*$");

    private final PdfDocument pdfDoc;

    public ItextTocExtractor(String pdfPath) throws IOException {
        this.pdfDoc = new PdfDocument(new PdfReader(pdfPath));
    }

    @Override
    public List<String> extract() {
        List<LineWithMetadata> allLines = new ArrayList<>();
        for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
            try {
                MetadataTextExtractionStrategy strategy = new MetadataTextExtractionStrategy();
                new PdfCanvasProcessor(strategy).processPageContent(pdfDoc.getPage(i));
                allLines.addAll(convertChunksToLines(strategy.getTextChunks(), pdfDoc.getPage(i), pdfDoc));
            } catch (Exception e) {
                log.error("Error processing page " + i, e);
            }
        }
        pdfDoc.close();
        return recognizeTocPages(allLines);
    }

    private List<String> recognizeTocPages(List<LineWithMetadata> allLines) {
        if (allLines.isEmpty()) return new ArrayList<>();
        
        Style dominantStyle = findDominantStyle(allLines);

        Map<Integer, List<LineWithMetadata>> linesByPage = allLines.stream()
                .collect(Collectors.groupingBy(LineWithMetadata::getPageNum));

        List<String> tocPageContents = new ArrayList<>();

        for (Map.Entry<Integer, List<LineWithMetadata>> entry : linesByPage.entrySet()) {
            List<LineWithMetadata> tocLines = getPotentialTocLines(entry.getValue(), dominantStyle);
            if (!tocLines.isEmpty()) {
                for (LineWithMetadata line : tocLines) {
                    tocPageContents.add(reconstructLineWithSpaces(line));
                }
            }
        }
        return tocPageContents;
    }

    private List<LineWithMetadata> getPotentialTocLines(List<LineWithMetadata> pageLines, Style dominantStyle) {
        List<LineWithMetadata> tocCandidates = new ArrayList<>();
        for (LineWithMetadata line : pageLines) {
            if (isTocLikeLine(line, dominantStyle)) {
                tocCandidates.add(line);
            }
        }
        // If a page has enough TOC-like lines, we consider all candidates from that page valid.
        if (tocCandidates.size() >= 3) {
            return tocCandidates;
        }
        return Collections.emptyList();
    }

    private boolean isTocLikeLine(LineWithMetadata line, Style dominantStyle) {
        String trimmed = line.getTextContent().trim();
        if (trimmed.length() > 150 || trimmed.length() < 3) {
            return false;
        }

        if (TOC_DOT_PATTERN.matcher(trimmed).matches()) {
            return true;
        }

        if (TOC_NUMERIC_END_PATTERN.matcher(trimmed).matches()) {
            // This is the transplanted logic: check if it's an "abnormal paragraph"
            boolean isAbnormal = line.getStyle().getFontSize() > dominantStyle.getFontSize() + 1 && trimmed.length() < 50;
            return isAbnormal && !trimmed.endsWith(".");
        }

        return false;
    }

    private String reconstructLineWithSpaces(LineWithMetadata line) {
        List<TextChunk> chunks = line.getChunks();
        if (chunks == null || chunks.isEmpty()) {
            return line.getTextContent();
        }

        StringBuilder textBuilder = new StringBuilder();
        TextChunk first = chunks.get(0);
        textBuilder.append(first.getText());

        for (int i = 1; i < chunks.size(); i++) {
            TextChunk prev = chunks.get(i - 1);
            TextChunk curr = chunks.get(i);

            float spaceWidth = prev.getSingleSpaceWidth();
            if (spaceWidth <= 0) {
                spaceWidth = prev.getFontSize() * 0.25f;
            }
            float gap = curr.getX() - (prev.getX() + prev.getWidth());

            if (gap > spaceWidth * 5) { // Large gap, likely between title and page number
                textBuilder.append("     "); // Insert a fixed "tab" (5 spaces)
            } else if (gap > spaceWidth * 0.3f) { // Small gap, likely between words
                textBuilder.append(" ");
            }
            textBuilder.append(curr.getText());
        }
        return textBuilder.toString();
    }

    private Style findDominantStyle(List<LineWithMetadata> allLines) {
        return allLines.stream()
                .map(LineWithMetadata::getStyle)
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(new Style("Default", 10f));
    }

    @Override
    public List<String> extract(int startPageNum, int endPageNum) {
        List<String> tocPages = new ArrayList<>();
        for (int i = startPageNum; i <= endPageNum; i++) {
            try {
                MetadataTextExtractionStrategy strategy = new MetadataTextExtractionStrategy();
                new PdfCanvasProcessor(strategy).processPageContent(pdfDoc.getPage(i));
                List<LineWithMetadata> lines = convertChunksToLines(strategy.getTextChunks(), pdfDoc.getPage(i), pdfDoc);
                tocPages.add(lines.stream().map(this::reconstructLineWithSpaces).collect(Collectors.joining("\n")));
            } catch (Exception e) {
                log.error("Error processing page " + i, e);
            }
        }
        return tocPages;
    }
}
