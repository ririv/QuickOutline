package com.ririv.quickoutline.view.icons;

import com.ririv.quickoutline.view.controls.SvgIcon;

import java.util.Objects;


public class TocIcon extends SvgIcon {

    public TocIcon() {
        String svgPath = Objects.requireNonNull(
            getClass().getResource("/drawable/toc.svg")
        ).getPath();
        setSvgPath(svgPath);


        setScaleX(0.75);
        setScaleY(0.75);
    }
}
