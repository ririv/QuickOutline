package com.ririv.quickoutline.view.icons;

import com.ririv.quickoutline.view.controls.SvgIcon;

import java.util.Objects;


public class RefreshIcon extends SvgIcon {

    public RefreshIcon() {
        String svgPath = Objects.requireNonNull(
                getClass().getResource("/drawable/refresh.svg")
        ).getPath();
        setSvgPath(svgPath);
    }
}
