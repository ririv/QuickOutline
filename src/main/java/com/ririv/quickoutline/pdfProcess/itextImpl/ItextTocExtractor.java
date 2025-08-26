package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor;
import com.ririv.quickoutline.pdfProcess.TocExtractor;
import com.ririv.quickoutline.pdfProcess.itextImpl.model.LineWithMetadata;
import com.ririv.quickoutline.pdfProcess.itextImpl.model.TextBlock;
import com.ririv.quickoutline.pdfProcess.itextImpl.model.TextChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.ririv.quickoutline.pdfProcess.itextImpl.model.TextChunk.convertChunksToLines;

public class ItextTocExtractor implements TocExtractor {

    private static final Logger log = LoggerFactory.getLogger(ItextTocExtractor.class);
    private static final Pattern NUMBERING_PATTERN = Pattern.compile("^\\s*([\\d.]+|[A-Za-z][.]|[IVXLCDM]+[.)]).*\s*$");

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
        
        List<TextBlock> allBlocks = aggregateLinesIntoBlocks(allLines);
        
        TocAnalyser tocAnalyser = new TocAnalyser();
        List<TextBlock> tocBlocks = tocAnalyser.analyze(allBlocks);
        
        return tocBlocks.stream()
                .map(this::reconstructBlockWithSpaces)
                .collect(Collectors.toList());
    }

    private String reconstructBlockWithSpaces(TextBlock block) {
        StringBuilder textBuilder = new StringBuilder();
        List<LineWithMetadata> lines = block.getLines();

        for (int lineIdx = 0; lineIdx < lines.size(); lineIdx++) {
            LineWithMetadata line = lines.get(lineIdx);
            List<TextChunk> chunks = line.getChunks();

            if (chunks == null || chunks.isEmpty()) {
                textBuilder.append(line.getTextContent());
            }

            TextChunk firstChunk = chunks.get(0);
            textBuilder.append(firstChunk.getText());

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
            if (lineIdx < lines.size() - 1) {
                textBuilder.append("\n"); // Add newline between lines of a block
            }
        }
        return textBuilder.toString();
    }

    private List<TextBlock> aggregateLinesIntoBlocks(List<LineWithMetadata> lines) {
        List<TextBlock> blocks = new ArrayList<>();
        if (lines.isEmpty()) return blocks;

        TextBlock currentBlock = new TextBlock(lines.get(0));

        for (int i = 1; i < lines.size(); i++) {
            LineWithMetadata currentLine = lines.get(i);
            if (shouldMerge(currentBlock, currentLine)) {
                currentBlock.addLine(currentLine);
            } else {
                blocks.add(currentBlock);
                currentBlock = new TextBlock(currentLine);
            }
        }
        blocks.add(currentBlock);
        return blocks;
    }

    private boolean shouldMerge(TextBlock block, LineWithMetadata nextLine) {
        LineWithMetadata lastLine = block.getLines().get(block.getLines().size() - 1);
        if (lastLine.getPageNum() != nextLine.getPageNum()) return false;
        double verticalGap = lastLine.getY() - nextLine.getY();
        if (verticalGap > lastLine.getStyle().getFontSize() * 1.8) return false;
        if (!lastLine.getStyle().equals(nextLine.getStyle())) return false;
        if (Math.abs(lastLine.getX() - nextLine.getX()) > 5.0) return false;
        String prevText = lastLine.getTextContent().trim();
        if (prevText.endsWith(".") || prevText.endsWith("?") || prevText.endsWith("!") || prevText.endsWith(":")) return false;
        String nextText = nextLine.getTextContent().trim();
        if (nextText.isEmpty() || NUMBERING_PATTERN.matcher(nextText).matches()) return false;

        if (!Character.isLowerCase(nextText.charAt(0))) {
            return prevText.length() <= 60;
        }

        return true;
    }

    @Override
    public List<String> extract(int startPageNum, int endPageNum) {
        List<String> tocPages = new ArrayList<>();
        for (int i = startPageNum; i <= endPageNum; i++) {
            try {
                MetadataTextExtractionStrategy strategy = new MetadataTextExtractionStrategy();
                new PdfCanvasProcessor(strategy).processPageContent(pdfDoc.getPage(i));
                List<LineWithMetadata> lines = convertChunksToLines(strategy.getTextChunks(), pdfDoc.getPage(i), pdfDoc);
                List<TextBlock> blocks = aggregateLinesIntoBlocks(lines);
                
                TocAnalyser tocAnalyser = new TocAnalyser();
                List<TextBlock> tocBlocks = tocAnalyser.analyze(blocks);

                for (TextBlock block : tocBlocks) {
                    tocPages.add(reconstructBlockWithSpaces(block));
                }

            } catch (Exception e) {
                log.error("Error processing page " + i, e);
            }
        }
        return tocPages;
    }
}