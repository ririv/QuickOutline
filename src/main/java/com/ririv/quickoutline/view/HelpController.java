package com.ririv.quickoutline.view;

import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;

import java.io.IOException;
import java.net.URI;

import java.awt.Desktop;
import java.net.URISyntaxException;

public class HelpController {


    public AnchorPane helpPane;
    public FlowPane innerPane;

    public void initialize(){
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
        browse("https://github.com/ririv/QuickQutline");
    }

    public void browseHelpOnZhihu(){
        browse("https://zhuanlan.zhihu.com/p/390719305");
    }
    public void browseHelpOnGithub(){
        browse("https://github.com/ririv/QuickQutline/README.md");
    }

    public void browseHelpOnMyPage(){
        browse("https://www.zhihu.com/people/ririv");
    }


}
