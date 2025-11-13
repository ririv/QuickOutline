package com.ririv.quickoutline.view.icons;

import com.ririv.quickoutline.view.controls.SvgIcon;

import java.util.Objects;


public class FitToHeightIcon extends SvgIcon {

    public FitToHeightIcon() {
        String svgPath = Objects.requireNonNull(
            getClass().getResource("/drawable/fit-to-height.svg")
        ).getPath();
        setSvgPath(svgPath);
    }
}
