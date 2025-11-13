package com.ririv.quickoutline.view.icons;

import com.ririv.quickoutline.view.controls.SvgIcon;

import java.util.Objects;


public class TextEditIcon extends SvgIcon {

    public TextEditIcon() {
        String svgPath = Objects.requireNonNull(
            getClass().getResource("/drawable/text-edit.svg")
        ).getPath();
        setSvgPath(svgPath);


        setScaleX(0.75);
    }
}
