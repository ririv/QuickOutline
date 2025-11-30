package com.ririv.quickoutline.view.svgimageio;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;

/**
 * ImageReader implementation that uses Apache Batik to render SVG into BufferedImage.
 *
 * This is an Image I/O plugin so that JavaFX 24+ can take advantage of pluggable image loading
 * via javax.imageio.
 */
public class SvgImageReader extends ImageReader {

    private BufferedImage image;
    // 保留 SVG 原始字节，这样后续可以按需要重新渲染不同尺寸
    private byte[] svgBytes;

    public SvgImageReader(ImageReaderSpi originatingProvider) {
        super(originatingProvider);
    }

    @Override
    public ImageReadParam getDefaultReadParam() {
        return new SvgImageReadParam();
    }

    @Override
    public int getNumImages(boolean allowSearch) throws IOException {
        return 1; // SVG is treated as single-image
    }

    @Override
    public int getWidth(int imageIndex) throws IOException {
        ensureImageLoaded(imageIndex, null);
        return image.getWidth();
    }

    @Override
    public int getHeight(int imageIndex) throws IOException {
        ensureImageLoaded(imageIndex, null);
        return image.getHeight();
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
        ensureImageLoaded(imageIndex, null);
        ImageTypeSpecifier spec = ImageTypeSpecifier.createFromRenderedImage(image);
        return Collections.singletonList(spec).iterator();
    }

    @Override
    public IIOMetadata getStreamMetadata() {
        return null;
    }

    @Override
    public IIOMetadata getImageMetadata(int imageIndex) {
        return null;
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
        ensureImageLoaded(imageIndex, param);
        return image;
    }

    private void ensureImageLoaded(int imageIndex, ImageReadParam param) throws IOException {
        if (imageIndex != 0) {
            throw new IndexOutOfBoundsException("SVGImageReader supports only single image (index 0)");
        }
        if (image != null) {
            return;
        }
        if (!(input instanceof ImageInputStream)) {
            throw new IOException("Input not an ImageInputStream: " + (input != null ? input.getClass() : "null"));
        }

        ImageInputStream stream = (ImageInputStream) input;

        // 读入完整 SVG 源字节，后续可重复渲染
        if (svgBytes == null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            stream.mark();
            try {
                while ((read = stream.read(buffer)) != -1) {
                    baos.write(buffer, 0, read);
                }
            } finally {
                stream.reset();
            }
            svgBytes = baos.toByteArray();
        }

        float pixelWidth = -1;
        float pixelHeight = -1;
        if (param != null && param.getSourceRenderSize() != null) {
            pixelWidth = param.getSourceRenderSize().width;
            pixelHeight = param.getSourceRenderSize().height;
        }

        // 自定义 ImageTranscoder，将 SVG 渲染为 BufferedImage
        class BufferedImageTranscoder extends ImageTranscoder {
            private BufferedImage bufferedImage;

            @Override
            public BufferedImage createImage(int w, int h) {
                return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            }

            @Override
            public void writeImage(BufferedImage img, TranscoderOutput out) {
                this.bufferedImage = img;
            }

            public BufferedImage getBufferedImage() {
                return bufferedImage;
            }
        }

        BufferedImageTranscoder transcoder = new BufferedImageTranscoder();
        if (pixelWidth > 0) {
            transcoder.addTranscodingHint(ImageTranscoder.KEY_WIDTH, pixelWidth);
        }
        if (pixelHeight > 0) {
            transcoder.addTranscodingHint(ImageTranscoder.KEY_HEIGHT, pixelHeight);
        }

        try (InputStream is = new ByteArrayInputStream(svgBytes)) {
            TranscoderInput tIn = new TranscoderInput(is);
            transcoder.transcode(tIn, null);
            this.image = transcoder.getBufferedImage();
        } catch (TranscoderException e) {
            throw new IOException("Failed to transcode SVG", e);
        }
    }
}
