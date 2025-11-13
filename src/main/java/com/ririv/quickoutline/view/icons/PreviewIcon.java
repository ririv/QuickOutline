package com.ririv.quickoutline.view.icons;

import com.ririv.quickoutline.view.controls.SvgIcon;

import java.util.Objects;


public class PreviewIcon extends SvgIcon {

    public PreviewIcon() {
        String svgPath = Objects.requireNonNull(
                getClass().getResource("/drawable/preview.svg")
        ).getPath();
        setSvgPath(svgPath);
    }
}
