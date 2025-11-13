package com.ririv.quickoutline.view.icons;

import com.ririv.quickoutline.view.controls.SvgIcon;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;

import java.util.Objects;


public class GithubIcon extends SvgIcon {

    public GithubIcon() {
        String svgPath = Objects.requireNonNull(
            getClass().getResource("/drawable/github.svg")
        ).getPath();
        setSvgPath(svgPath);

        setBackground(Background.fill(Color.web("#616161")));
    }
}
