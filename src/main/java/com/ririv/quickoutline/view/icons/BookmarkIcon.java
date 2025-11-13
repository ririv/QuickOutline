package com.ririv.quickoutline.view.icons;

import com.ririv.quickoutline.view.controls.SvgIcon;

import java.util.Objects;


public class BookmarkIcon extends SvgIcon {

    public BookmarkIcon() {
        String svgPath = Objects.requireNonNull(
            getClass().getResource("/drawable/bookmark.svg")
        ).getPath();
        setSvgPath(svgPath);

        setScaleX(0.7);
        setScaleY(0.9);
    }
}
