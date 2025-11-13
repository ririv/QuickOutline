package com.ririv.quickoutline.view.icons;

import com.ririv.quickoutline.view.controls.SvgIcon;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;

import java.util.Objects;

public class XiaohongshuIcon extends SvgIcon {

    public XiaohongshuIcon() {
        String svgPath = Objects.requireNonNull(
                getClass().getResource("/drawable/xiaohongshu.svg")
        ).getPath();
        setSvgPath(svgPath);

        setBackground(Background.fill(Color.web("#808080")));
    }
}
