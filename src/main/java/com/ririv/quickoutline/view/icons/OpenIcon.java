package com.ririv.quickoutline.view.icons;

import com.ririv.quickoutline.view.controls.SvgIcon;

import java.util.Objects;


public class OpenIcon extends SvgIcon {

    public OpenIcon() {
        String svgPath = Objects.requireNonNull(
            getClass().getResource("/drawable/open.svg")
        ).getPath();
        setSvgPath(svgPath);

        setScaleX(0.9);
        setScaleY(0.9);
    }
}
