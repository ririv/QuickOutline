package com.ririv.quickoutline.view.icons;

import com.ririv.quickoutline.view.controls.SvgIcon;

import java.util.Objects;


public class ConnectedIcon extends SvgIcon {

    public ConnectedIcon() {
        String svgPath = Objects.requireNonNull(
            getClass().getResource("drawable/connected.svg")
        ).getPath();
        setSvgPath(svgPath);
    }
}
