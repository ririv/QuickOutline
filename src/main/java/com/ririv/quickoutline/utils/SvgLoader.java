package com.ririv.quickoutline.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.Objects;

public class SvgLoader {

    public static Pair<String, String> getPathAndVieBox(String iconName) {

        String resourcePath = "/com/ririv/quickoutline/view/icon/svg/" + iconName;
        try (InputStream is = SvgLoader.class.getResourceAsStream(resourcePath)) {
            Objects.requireNonNull(is, "Cannot find resource: " + resourcePath);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is);

            // Get the root SVG element
            Element svgElement = doc.getDocumentElement();
            String viewBox = svgElement.getAttribute("viewBox");

            // Find all <path> elements
            NodeList pathList = doc.getElementsByTagName("path");

            if (pathList.getLength() > 0) {
                // Get the first <path> element
                Element pathElement = (Element) pathList.item(0);
                // Return the 'd' attribute value and viewBox
                return new Pair<>(pathElement.getAttribute("d"), viewBox);
            }
            throw new IllegalStateException("No path found in SVG file: " + iconName);
        } catch (Exception e) {
            // In a real-world app, you might want to log this error more gracefully
            throw new RuntimeException("Failed to load SVG icon: " + iconName, e);
        }
    }
}