package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor;
import com.ririv.quickoutline.pdfProcess.TocExtractor;
import com.ririv.quickoutline.pdfProcess.itextImpl.model.LineWithMetadata;
import com.ririv.quickoutline.pdfProcess.itextImpl.model.Style;
import com.ririv.quickoutline.pdfProcess.itextImpl.model.TextBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.ririv.quickoutline.pdfProcess.itextImpl.model.TextChunk.convertChunksToLines;

public class ItextTocExtractor implements TocExtractor {

    private static final Logger log = LoggerFactory.getLogger(ItextTocExtractor.class);

    private final String pdfPath;

    public ItextTocExtractor(String pdfPath) {
        this.pdfPath = pdfPath;
    }

    @Override
    public List<String> extract() {
        try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(pdfPath))) {
            return extract(1, pdfDoc.getNumberOfPages());
        } catch (IOException e) {
            log.error("Failed to read PDF for page count", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> extract(int startPageNum, int endPageNum) {
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        // --- 第一阶段: 并行提取所有页面的 TextBlock ---
        List<TextBlock> allBlocks = extractTextBlocksInParallel(startPageNum, endPageNum, executor);

        // --- 第二阶段: 计算全局样式，然后并行分析 ---
        TocAnalyser tocAnalyser = new TocAnalyser();

        // 1. 计算全局 Dominant Style
        Style dominantStyle = tocAnalyser.findDominantStyle(allBlocks);

        // 2. 按页码分组
        Map<Integer, List<TextBlock>> blocksByPage = allBlocks.stream()
                .collect(Collectors.groupingBy(b -> b.getPrimaryLine().getPageNum()));

        // 3. 并行分析每个页面的 Block
        List<Callable<List<TextBlock>>> analysisTasks = new ArrayList<>();
        for (List<TextBlock> pageBlocks : blocksByPage.values()) {
            analysisTasks.add(() -> tocAnalyser.findTocBlocksInPage(pageBlocks, dominantStyle));
        }

        // 4. 收集并格式化最终结果
        List<String> tocResult = new ArrayList<>();
        try {
            List<Future<List<TextBlock>>> futures = executor.invokeAll(analysisTasks);
            for (Future<List<TextBlock>> future : futures) {
                future.get().stream()
                        .map(TextBlock::reconstructBlockWithSpaces)
                        .forEach(tocResult::add);
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error during parallel TOC analysis", e);
            Thread.currentThread().interrupt(); // Reset the interrupted status
        }

        executor.shutdown();
        return tocResult;
    }

    private List<TextBlock> extractTextBlocksInParallel(int startPageNum, int endPageNum, ExecutorService executor) {
        List<Callable<List<LineWithMetadata>>> extractionTasks = IntStream.rangeClosed(startPageNum, endPageNum)
                .mapToObj(pageNum -> (Callable<List<LineWithMetadata>>) () -> {
                    // 每个线程创建自己的 PdfDocument 实例
                    try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(pdfPath))) {
                        MetadataTextExtractionStrategy strategy = new MetadataTextExtractionStrategy();
                        new PdfCanvasProcessor(strategy).processPageContent(pdfDoc.getPage(pageNum));
                        return convertChunksToLines(strategy.getTextChunks(), pdfDoc.getPage(pageNum), pdfDoc);
                    } catch (Exception e) {
                        log.error("Error extracting lines from page {}", pageNum, e);
                        return Collections.emptyList();
                    }
                })
                .collect(Collectors.toList());

        List<LineWithMetadata> allLines = new ArrayList<>();
        try {
            List<Future<List<LineWithMetadata>>> futures = executor.invokeAll(extractionTasks);
            for (Future<List<LineWithMetadata>> future : futures) {
                allLines.addAll(future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error during parallel line extraction", e);
            Thread.currentThread().interrupt(); // Reset the interrupted status
        }

        return TextBlock.aggregateLinesIntoBlocks(allLines);
    }
}
