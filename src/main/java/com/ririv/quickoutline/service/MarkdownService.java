package com.ririv.quickoutline.service;

import com.ririv.quickoutline.pdfProcess.PdfPageGenerator;
import com.ririv.quickoutline.pdfProcess.itextImpl.ItextHtmlConverter;
import com.ririv.quickoutline.pdfProcess.itextImpl.iTextPdfPageGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.Consumer;


public class MarkdownService {

    private static final Logger log = LoggerFactory.getLogger(MarkdownService.class);

    private final PdfPageGenerator pdfPageGenerator = new iTextPdfPageGenerator();
    private final ItextHtmlConverter htmlConverter = new ItextHtmlConverter();
    /**
     * 将 HTML 字符串转换为 PDF 字节数组。
     *
     * @param htmlContent HTML 内容（由前端 Vditor 渲染产生）。
     * @param baseUri     Base URI 用于解析相对资源路径（图片等）。
     * @param onMessage   信息提示回调（字体下载等）。
     * @param onError     错误提示回调。
     */
    public byte[] convertHtmlToPdfBytes(String htmlContent, String baseUri,
                                        Consumer<String> onMessage, Consumer<String> onError) {
        final String safe = htmlContent == null ? "" : htmlContent;
        if (safe.isBlank()) {
            if (log.isDebugEnabled()) log.debug("skip empty html content");
            return new byte[0];
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // HtmlConverter 仍然由 ItextHtmlConverter 管理，使用 DownloadEvent 在下层上报字体下载
            htmlConverter.convertToPdf(
                    safe,
                    baseUri,
                    baos,
                    event -> {
                        if (event == null) return;
                        switch (event.getType()) {
                            case START -> onMessage.accept("正在下载字体: " + event.getResourceName() + "...");
                            case SUCCESS -> onMessage.accept("字体 " + event.getResourceName() + " 下载完成。");
                            case ERROR -> onError.accept("字体下载失败: " + event.getResourceName() +
                                    (event.getDetail() == null ? "" : " - " + event.getDetail()));
                            case PROGRESS -> { /* currently unused */ }
                        }
                    }
            );
            return baos.toByteArray();
        } catch (Exception e) {
            String msg = "Failed to convert HTML to PDF: " + e.getMessage();
            log.error(msg, e);
            onError.accept(msg);
            throw new RuntimeException(msg, e);
        }
    }

    /**
     * 基于前端渲染好的 HTML 将页面插入到 PDF 中。
     */
    public void insertPageFromHtml(String srcFile,
                                   String destFile,
                                   String htmlContent,
                                   int insertPos,
                                   String baseUri,
                                   Consumer<String> onMessage,
                                   Consumer<String> onError) throws IOException {
        byte[] pdfPageBytes = convertHtmlToPdfBytes(htmlContent, baseUri, onMessage, onError);
        pdfPageGenerator.generateAndInsertPage(
                srcFile, destFile, pdfPageBytes, insertPos);
    }

}
