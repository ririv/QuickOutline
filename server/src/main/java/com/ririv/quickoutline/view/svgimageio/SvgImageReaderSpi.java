package com.ririv.quickoutline.view.svgimageio;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * ImageReaderSpi implementation for SVG, wiring SvgImageReader into ImageIO.
 */
public class SvgImageReaderSpi extends ImageReaderSpi {

    private static final String VENDOR_NAME = "QuickOutline";
    private static final String VERSION = "1.0";
    private static final String READER_CLASS_NAME = SvgImageReader.class.getName();
    private static final String[] NAMES = {"svg", "SVG"};
    private static final String[] SUFFIXES = {"svg"};
    private static final String[] MIME_TYPES = {"image/svg+xml"};

    public SvgImageReaderSpi() {
        super(
                VENDOR_NAME,
                VERSION,
                NAMES,
                SUFFIXES,
                MIME_TYPES,
                READER_CLASS_NAME,
                STANDARD_INPUT_TYPE,
                null,
                false,
                null, null, null, null,
                false,
                null, null, null, null
        );
    }

    @Override
    public boolean canDecodeInput(Object source) throws IOException {
        return source instanceof ImageInputStream;
    }

    @Override
    public ImageReader createReaderInstance(Object extension) throws IOException {
        return new SvgImageReader(this);
    }

    @Override
    public String getDescription(java.util.Locale locale) {
        return "SVG ImageReader using Apache Batik";
    }
}
