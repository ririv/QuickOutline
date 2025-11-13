package com.ririv.quickoutline.view.icons;

import com.ririv.quickoutline.view.controls.SvgIcon;

import java.util.Objects;


public class TrashIcon extends SvgIcon {

    public TrashIcon() {
        String svgPath = Objects.requireNonNull(
            getClass().getResource("/drawable/trash.svg")
        ).getPath();
        setSvgPath(svgPath);
    }
}
