package com.ririv.quickoutline.view.icons;

import com.ririv.quickoutline.view.controls.SvgIcon;

import java.util.Objects;


public class DeleteItemIcon extends SvgIcon {

    public DeleteItemIcon() {
        String svgPath = Objects.requireNonNull(
            getClass().getResource("/drawable/delete-item.svg")
        ).getPath();
        setSvgPath(svgPath);

    }
}
