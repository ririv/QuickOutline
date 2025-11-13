package com.ririv.quickoutline.view.icons;

import com.ririv.quickoutline.view.controls.SvgIcon;

import java.util.Objects;


public class FitToWidthIcon extends SvgIcon {

    public FitToWidthIcon() {
        String svgPath = Objects.requireNonNull(
            getClass().getResource("/drawable/fit-to-width.svg")
        ).getPath();
        setSvgPath(svgPath);
    }
}
