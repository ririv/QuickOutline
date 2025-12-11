package com.ririv.quickoutline.service.pdfpreview;

import com.ririv.quickoutline.utils.FastByteArrayOutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.io.IOException;
import java.util.List;

public class PdfSvgServiceTest {

    private static PdfSvgService pdfSvgService;
    private static FastByteArrayOutputStream complexPdfData;

    // 测试配置
    private static final int PAGE_COUNT = 5;       // 生成5页PDF
    private static final int WARMUP_ITERATIONS = 5; // 预热跑5次
    private static final int TEST_ITERATIONS = 10;  // 正式跑10次取平均值

    @BeforeAll
    static void setup() throws IOException {
        pdfSvgService = new PdfSvgService();
        System.out.println("正在生成测试用 PDF (包含大量矢量图形和文字)...");
        complexPdfData = generateComplexPdf();
        System.out.println("PDF 生成完毕，大小: " + complexPdfData.size() / 1024 + " KB");
    }

    @Test
    void benchmarkEngines() {
        System.out.println("\n========================================");
        System.out.println("      SVG 渲染引擎性能对比基准测试");
        System.out.println("========================================");

        pdfSvgService.setUseJFreeSvg(true); // 切换开关
        long jfreeTime = runBenchmark("JFreeSVG");

        // 2. 测试 Batik
        pdfSvgService.setUseJFreeSvg(false); // 切换开关
        long batikTime = runBenchmark("Batik (Apache)");

        // 3. 总结
        System.out.println("\n----------------------------------------");
        System.out.println("最终结果对比:");
        System.out.printf("JFreeSVG 平均耗时: %d ms\n", jfreeTime);
        System.out.printf("Batik    平均耗时: %d ms\n", batikTime);

        if (batikTime > jfreeTime) {
            double improvement = (double) (batikTime - jfreeTime) / batikTime * 100;
            System.out.printf("🚀 结论: JFreeSVG 比 Batik 快了 %.2f%%\n", improvement);
        } else {
            System.out.println("🤔 结论: 两者速度差不多，或 Batik 更快 (通常不可能)");
        }
    }

    private long runBenchmark(String engineName) {
        System.out.printf("\n开始测试引擎: [%s]\n", engineName);

        // --- 预热阶段 (Warm-up) ---
        // 这一步是为了让 JVM 进行 JIT 编译，消除冷启动干扰
        System.out.print("预热中...");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            pdfSvgService.clearCache(); // 务必清除缓存，否则就是测 Map.get 的速度了
            pdfSvgService.updatePreview(complexPdfData);
            System.out.print(".");
        }
        System.out.println(" 完成.");

        // --- 正式测试阶段 ---
        System.out.print("压测中...");
        long totalTime = 0;

        // 建议手动触发一次 GC，尽量让内存处于同一起跑线
        System.gc();

        for (int i = 0; i < TEST_ITERATIONS; i++) {
            pdfSvgService.clearCache(); // 清除缓存

            long start = System.currentTimeMillis();
            List<?> result = pdfSvgService.updatePreview(complexPdfData);
            long end = System.currentTimeMillis();

            totalTime += (end - start);

            // 简单的断言，确保真的转换成功了
            if (result.isEmpty()) {
                throw new RuntimeException("错误：转换结果为空！");
            }
            System.out.print(".");
        }
        System.out.println(" 完成.");

        long avgTime = totalTime / TEST_ITERATIONS;
        System.out.printf(">> [%s] 平均每次耗时: %d ms\n", engineName, avgTime);
        return avgTime;
    }

    /**
     * 生成一个稍微复杂点的 PDF，包含大量文字和线条，
     * 这样才能体现出 DOM 构建 (Batik) 和 字符串拼接 (JFreeSVG) 的性能差异。
     */
    private static FastByteArrayOutputStream generateComplexPdf() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            for (int i = 0; i < PAGE_COUNT; i++) {
                PDPage page = new PDPage();
                doc.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(doc, page)) {

                    // 1. 绘制 1000 条随机线条 (测试 Path/Shape 性能)
                    content.setStrokingColor(Color.BLUE);
                    content.setLineWidth(0.5f);
                    for (int j = 0; j < 200; j++) {
                        content.moveTo((float) (Math.random() * 500), (float) (Math.random() * 800));
                        content.lineTo((float) (Math.random() * 500), (float) (Math.random() * 800));
                        content.stroke();
                    }

                    // 2. 绘制 50 行文字 (测试 Text 性能)
                    content.beginText();
                    content.setFont(font, 10);
                    content.newLineAtOffset(50, 750);
                    for (int k = 0; k < 50; k++) {
                        content.showText("Performance Test Line " + k + ": The quick brown fox jumps over the lazy dog.");
                        content.newLineAtOffset(0, -12);
                    }
                    content.endText();

                    // 3. 绘制半透明矩形 (测试复杂属性)
                    content.setNonStrokingColor(new Color(255, 0, 0, 100)); // 红色半透明
                    content.addRect(200, 200, 100, 100);
                    content.fill();
                }
            }

            FastByteArrayOutputStream out = new FastByteArrayOutputStream();
            doc.save(out);
            return out;
        }
    }
}