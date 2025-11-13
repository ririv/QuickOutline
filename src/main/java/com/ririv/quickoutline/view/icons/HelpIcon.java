package com.ririv.quickoutline.view.icons;

import com.ririv.quickoutline.view.controls.SvgIcon;

import java.util.Objects;


public class HelpIcon extends SvgIcon {

    public HelpIcon() {
        String svgPath = Objects.requireNonNull(
            getClass().getResource("/drawable/help.svg")
        ).getPath();
        setSvgPath(svgPath);
    }
}
