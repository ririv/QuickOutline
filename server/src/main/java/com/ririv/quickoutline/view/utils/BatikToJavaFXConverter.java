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
import java.net.URL;

/**
 * Provides utility methods for converting SVG path data for JavaFX.
 */
public class BatikToJavaFXConverter {

    /**
     * Parses an SVG resource on the classpath and combines all path data.
     * Safer for resources inside a modular runtime / jpackage image where URL.getPath() is not a real filesystem path.
     * @param resourcePath classpath resource path, e.g. "/drawable/open.svg"
     * @return combined 'd' attributes string
     */
    public static String getCombinedPathFromResource(String resourcePath) {
        if (resourcePath == null || resourcePath.isEmpty()) {
            throw new IllegalArgumentException("resourcePath must not be empty");
        }
        URL url = BatikToJavaFXConverter.class.getResource(resourcePath);
        if (url == null) {
            throw new RuntimeException("Resource not found: " + resourcePath);
        }
        // 请使用 URL.openStream() 打开流，而不是使用 FileInputStream，因为在 jpackage 镜像中，资源可能不在真实的文件系统路径中，
        // 因为在 jpackage 镜像中，资源可能不在真实的文件系统路径中
        String parser = XMLResourceDescriptor.getXMLParserClassName();
        SVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
        try (InputStream is = url.openStream()) {
            SVGDocument svgDoc = (SVGDocument) factory.createDocument("http://www.w3.org/2000/svg", "svg", url.toExternalForm(), is);
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
        } catch (IOException e) {
            throw new RuntimeException("Failed to read SVG resource: " + resourcePath, e);
        }
    }
}