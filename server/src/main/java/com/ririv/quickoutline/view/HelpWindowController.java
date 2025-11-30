package com.ririv.quickoutline.view;

import com.ririv.quickoutline.utils.InfoUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class HelpWindowController {


    public AnchorPane helpPane;
    public FlowPane innerPane;
    public Label versionLabel;

    public void initialize() {
        versionLabel.textProperty().bind(new SimpleStringProperty("Version: " +InfoUtil.getAppVersion()));
    }

    public void browse(String url){
        Desktop desktop = Desktop.getDesktop();
        try {
            desktop.browse(new URI(url)); //使用默认浏览器打开超链接
        } catch (IOException|URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void browseRemoteRepo(){
        browse("https://github.com/ririv/QuickOutline");
    }

    public void browseHelpOnZhihu(){
        browse("https://zhuanlan.zhihu.com/p/390719305");
    }
    public void browseHelpOnGithub(){
        browse("https://github.com/ririv/QuickOutline/blob/master/README.md");
    }

    public void browseHelpOnMyPage(){
        browse("https://www.xiaohongshu.com/user/profile/5f988414000000000101ca29");
    }


}
