package com.ririv.quickoutline.service;

import com.ririv.quickoutline.pdfProcess.PdfPageGenerator;
import com.ririv.quickoutline.pdfProcess.itextImpl.ItextHtmlConverter;
import com.ririv.quickoutline.pdfProcess.itextImpl.iTextPdfPageGenerator;
import com.ririv.quickoutline.utils.PayloadsJsonParser;
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
     * @param mdEditorContentPayloads HTML 内容，包含Styles（由前端 Vditor 渲染产生）。
     * @param baseUri     Base URI 用于解析相对资源路径（图片等）。
     * @param onMessage   信息提示回调（字体下载等）。
     * @param onError     错误提示回调。
     */
    public byte[] convertHtmlToPdfBytes(PayloadsJsonParser.MdEditorContentPayloads mdEditorContentPayloads, String baseUri,
                                        Consumer<String> onMessage, Consumer<String> onError) {
        String htmlContent = mdEditorContentPayloads.html();
        final String safeBody = htmlContent == null ? "" : htmlContent;
        if (safeBody.isBlank()) {
            log.debug("skip empty html content");
            return new byte[0];
        }

        // 将 Vditor 的 HTML 片段包装成完整文档，并内联一份适用于 PDF 的样式
        String fullHtml = buildHtmlForPdf(mdEditorContentPayloads, onError);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // HtmlConverter 仍然由 ItextHtmlConverter 管理，使用 DownloadEvent 在下层上报字体下载
            htmlConverter.convertToPdf(
                    fullHtml,
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
     * 构造用于 PDF 渲染的完整 HTML 文档，并尝试内联 Vditor 的样式。
     *
     * 方案B：尽可能复用 Vditor 的 CSS，让 PDF 的视觉接近编辑器预览。
     */
    private String buildHtmlForPdf(PayloadsJsonParser.MdEditorContentPayloads mdEditorContentPayloads, Consumer<String> onError) {
        String vditorCss = "";
        try {
            // 方案 B：直接复用打包进来的 vditor.bundle.css，使 PDF 更接近 Vditor 预览效果
            var cssStream = MarkdownService.class.getResourceAsStream("/web/vditor.bundle.css");
            if (cssStream != null) {
                try (cssStream) {
                    vditorCss = new String(cssStream.readAllBytes());
                }
            } else {
                vditorCss = fallbackCss();
            }
        } catch (Exception e) {
            String msg = "Failed to load Vditor CSS for PDF, fallback to basic styles: " + e.getMessage();
            log.warn(msg, e);
            onError.accept(msg);
            vditorCss = fallbackCss();
        }

        // bodyFragment 是 Vditor 的预览 HTML 片段，这里将它包在一个带有 vditor-reset 的容器中
        return "<!DOCTYPE html>" +
                "<html><head>" +
                "<meta charset=\"utf-8\"/>" +
                "<style>" + vditorCss + "</style>" +
                "<style>" + mdEditorContentPayloads.styles() + "</style>" +
                "</head><body>" +
                "<div class=\"vditor-reset\">" +
                mdEditorContentPayloads.html() +
                "</div>" +
                "</body></html>";
    }

    /**
     * 当找不到 Vditor CSS 或加载失败时使用的基础样式，避免 PDF 完全无样式。
     */
    private String fallbackCss() {
        return "body { font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; line-height: 1.6; font-size: 14px; }" +
                "h1, h2, h3, h4, h5, h6 { font-weight: 600; margin: 1.2em 0 0.6em; }" +
                "p { margin: 0.5em 0; }" +
                "pre, code { font-family: SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace; }" +
                "pre { background: #f5f5f5; padding: 8px 12px; border-radius: 4px; overflow-x: auto; }" +
                "code { background: #f5f5f5; padding: 2px 4px; border-radius: 3px; }" +
                "ul, ol { padding-left: 1.6em; }" +
                "table { border-collapse: collapse; width: 100%; margin: 1em 0; }" +
                "th, td { border: 1px solid #ccc; padding: 4px 8px; }" +
                "blockquote { border-left: 4px solid #ccc; margin: 0.8em 0; padding: 0.2em 0.8em; color: #555; }";
    }

    /**
     * 基于前端渲染好的 HTML 将页面插入到 PDF 中。
     */
    public void insertPageFromHtml(String srcFile,
                                   String destFile,
                                   PayloadsJsonParser.MdEditorContentPayloads mdEditorContentPayloads,
                                   int insertPos,
                                   String baseUri,
                                   Consumer<String> onMessage,
                                   Consumer<String> onError) throws IOException {
        byte[] pdfPageBytes = convertHtmlToPdfBytes(mdEditorContentPayloads, baseUri, onMessage, onError);
        pdfPageGenerator.generateAndInsertPage(
                srcFile, destFile, pdfPageBytes, insertPos);
    }

}
