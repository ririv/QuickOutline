package com.ririv.quickoutline.view.icons;

import com.ririv.quickoutline.view.controls.SvgIcon;

import java.util.Objects;


public class TreeDiagramIcon extends SvgIcon {

    public TreeDiagramIcon() {
        String svgPath = Objects.requireNonNull(
            getClass().getResource("/drawable/tree-diagram.svg")
        ).getPath();
        setSvgPath(svgPath);


        setScaleX(0.9);
        setScaleY(0.9);
    }
}
