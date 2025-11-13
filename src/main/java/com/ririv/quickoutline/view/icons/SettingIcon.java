package com.ririv.quickoutline.view.icons;

import com.ririv.quickoutline.view.controls.SvgIcon;

import java.util.Objects;


public class SettingIcon extends SvgIcon {

    public SettingIcon() {
        String svgPath = Objects.requireNonNull(
            getClass().getResource("/drawable/setting.svg")
        ).getPath();
        setSvgPath(svgPath);
    }
}
