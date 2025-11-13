package com.ririv.quickoutline.view.utils;

import javafx.scene.layout.Region;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.dom.svg.SVGDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.svg.SVGDocument;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Provides utility methods for converting SVG path data for JavaFX.
 */
public class BatikToJavaFXConverter {

    /**
     * Parses an SVG file and combines all path data into a single string.
     * This combined path string can be used with JavaFX's -fx-shape CSS property.
     *
     * @param svgFilePath The path to the SVG file.
     * @return A string containing the combined 'd' attributes of all path elements, or an empty string if no paths are found.
     */
    public static String getCombinedPath(String svgFilePath) {
        SVGDocument svgDoc;
        try {
            svgDoc = getSvgDocument(svgFilePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read SVG file: " + svgFilePath, e);
        }

        StringBuilder combinedPath = new StringBuilder();
        NodeList pathNodes = svgDoc.getElementsByTagName("path");

        for (int i = 0; i < pathNodes.getLength(); i++) {
            Element pathElem = (Element) pathNodes.item(i);
            String d = pathElem.getAttribute("d");
            if (!d.trim().isEmpty()) {
                combinedPath.append(d).append(" ");
            }
        }
        return combinedPath.toString().trim();
    }


    private static SVGDocument getSvgDocument(String svgFilePath) throws IOException {
        String parser = XMLResourceDescriptor.getXMLParserClassName();
        SVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
        SVGDocument svgDoc;
        // 使用 try-with-resources 确保 InputStream 被正确关闭
        try (InputStream is = new FileInputStream(svgFilePath)) {
            String fileUri = new File(svgFilePath).toURI().toString(); // 获取文件的 URI
            // 根据您提供的 Javadoc，使用 (namespaceURI, qualifiedName, documentURI, inputStream)
            svgDoc = (SVGDocument) factory.createDocument("http://www.w3.org/2000/svg", "svg", fileUri, is);
        }
        return svgDoc;
    }
}