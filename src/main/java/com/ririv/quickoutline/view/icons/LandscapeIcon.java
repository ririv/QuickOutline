package com.ririv.quickoutline.view.icons;

import com.ririv.quickoutline.view.controls.SvgIcon;

import java.util.Objects;


public class LandscapeIcon extends SvgIcon {

    public LandscapeIcon() {
        String svgPath = Objects.requireNonNull(
            getClass().getResource("/drawable/landscape.svg")
        ).getPath();
        setSvgPath(svgPath);

        setScaleY(0.9);
    }
}
