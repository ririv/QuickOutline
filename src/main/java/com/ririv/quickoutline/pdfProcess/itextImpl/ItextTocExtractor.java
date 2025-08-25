package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor;
import com.ririv.quickoutline.pdfProcess.TocExtractor;
import com.ririv.quickoutline.pdfProcess.itextImpl.model.LineWithMetadata;
import com.ririv.quickoutline.pdfProcess.itextImpl.model.Style;
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
    private static final Pattern TOC_DOT_PATTERN = Pattern.compile(".*([.]\s*|\s{2,}){4,}\s*\\d+\\s*$");
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
            if (isPotentialTocPage(entry.getValue(), dominantStyle)) {
                String pageContent = entry.getValue().stream()
                        .map(LineWithMetadata::getTextContent)
                        .collect(Collectors.joining("\n"));
                tocPageContents.add(pageContent);
            }
        }
        return tocPageContents;
    }

    private boolean isPotentialTocPage(List<LineWithMetadata> pageLines, Style dominantStyle) {
        int tocLikeLineCount = 0;
        for (LineWithMetadata line : pageLines) {
            if (isTocLikeLine(line, dominantStyle)) {
                tocLikeLineCount++;
            }
        }
        return tocLikeLineCount >= 3;
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
                tocPages.add(lines.stream().map(LineWithMetadata::getTextContent).collect(Collectors.joining("\n")));
            } catch (Exception e) {
                log.error("Error processing page " + i, e);
            }
        }
        return tocPages;
    }
}