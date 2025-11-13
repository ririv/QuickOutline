package com.ririv.quickoutline.view.icons;

import com.ririv.quickoutline.view.controls.SvgIcon;

import java.util.Objects;

public class MarkdownIcon extends SvgIcon {

    public MarkdownIcon() {
        String svgPath = Objects.requireNonNull(
                getClass().getResource("/drawable/markdown.svg")
        ).getPath();
        setSvgPath(svgPath);
        setScaleY(0.6);
    }
}
