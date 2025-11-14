package com.ririv.quickoutline.view.icons;

import com.ririv.quickoutline.view.controls.SvgIcon;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;


public class GithubIcon extends SvgIcon {

    public GithubIcon() {
        setSvgResource("/drawable/github.svg");
        setBackground(Background.fill(Color.web("#616161")));
    }
}
