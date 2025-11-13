package com.ririv.quickoutline.view.icons;

import com.ririv.quickoutline.view.controls.SvgIcon;

import java.util.Objects;


public class LabelIcon extends SvgIcon {

    public LabelIcon() {
        String svgPath = Objects.requireNonNull(
            getClass().getResource("/drawable/label.svg")
        ).getPath();
        setSvgPath(svgPath);

        setScaleX(0.9);
        setScaleY(0.9);
    }
}
