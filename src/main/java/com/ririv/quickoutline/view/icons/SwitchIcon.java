package com.ririv.quickoutline.view.icons;

import com.ririv.quickoutline.view.controls.SvgIcon;

import java.util.Objects;

public class SwitchIcon extends SvgIcon {

    public SwitchIcon() {
        String svgPath = Objects.requireNonNull(
                getClass().getResource("/drawable/switch.svg")
        ).getPath();
        setSvgPath(svgPath);

        setScaleX(0.8);
        setScaleY(0.8);
    }
}

