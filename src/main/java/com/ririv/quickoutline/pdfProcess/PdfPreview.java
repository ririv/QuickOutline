package com.ririv.quickoutline.pdfProcess;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import javafx.scene.image.Image;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PdfPreview {

    public List<Image> view() {

            // 1. 使用 iText 生成 PDF 到内存
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // 添加内容到 PDF
            document.add(new Paragraph("Hello, this is a dynamically generated PDF!"));
            document.add(new Paragraph("This is a second line."));
            document.close();

            try {
                // 2. 使用 PDFBox 渲染 PDF 为图像
                PDDocument doc = Loader.loadPDF(outputStream.toByteArray());
                PDFRenderer renderer = new PDFRenderer(doc);
                // 渲染第一页为 BufferedImage
                BufferedImage bufferedImage = renderer.renderImage(0);
                doc.close();

                // 转换为 JavaFX Image
                ByteArrayOutputStream imageOutputStream = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", imageOutputStream);
                ByteArrayInputStream imageInputStream = new ByteArrayInputStream(imageOutputStream.toByteArray());

                Image image = new Image(imageInputStream);
                List<Image> imageList =  new ArrayList<>();
                imageList.add(image);
                return imageList;

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
    }
}

