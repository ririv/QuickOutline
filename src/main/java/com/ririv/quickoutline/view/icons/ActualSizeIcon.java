package com.ririv.quickoutline.view.icons;

import com.ririv.quickoutline.view.controls.SvgIcon;

import java.util.Objects;


public class ActualSizeIcon extends SvgIcon {

    public ActualSizeIcon() {
        String svgPath = Objects.requireNonNull(
            getClass().getResource("/drawable/actual-size.svg")
        ).getPath();
        setSvgPath(svgPath);
    }
}
