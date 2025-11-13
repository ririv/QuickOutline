package com.ririv.quickoutline.view.icons;

import com.ririv.quickoutline.view.controls.SvgIcon;

import java.util.Objects;


public class PageNumberIcon extends SvgIcon {

    public PageNumberIcon() {
        String svgPath = Objects.requireNonNull(
            getClass().getResource("/drawable/page-number.svg")
        ).getPath();
        setSvgPath(svgPath);

        setScaleX(0.8);
        setScaleY(0.95);
    }
}
