package com.ririv.quickoutline.view.icons;

import com.ririv.quickoutline.view.controls.SvgIcon;

import java.util.Objects;


public class ArrowUpIcon extends SvgIcon {

    public ArrowUpIcon() {
        String svgPath = Objects.requireNonNull(
            getClass().getResource("drawable/arrow-up.svg")
        ).getPath();
        setSvgPath(svgPath);

        setScaleX(1.3);
        setScaleY(0.5);
    }
}
