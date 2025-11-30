package com.ririv.quickoutline.view.svgimageio;

import javax.imageio.ImageReadParam;
import java.awt.Dimension;

/**
 * Custom ImageReadParam for SvgImageReader that supports sourceRenderSize
 * without throwing UnsupportedOperationException.
 */
public class SvgImageReadParam extends ImageReadParam {

    private Dimension sourceRenderSize;

    @Override
    public void setSourceRenderSize(Dimension size) {
        // allow null to clear the render size
        this.sourceRenderSize = (size == null) ? null : new Dimension(size);
    }

    @Override
    public Dimension getSourceRenderSize() {
        return (sourceRenderSize == null) ? null : new Dimension(sourceRenderSize);
    }
}
