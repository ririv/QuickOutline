package com.ririv.quickoutline.view;


import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.util.Objects;


/*
用webView实现树，代替javafx原生实现，还是有点小bug，拖拽时一直出现禁止的图标，在Chrome浏览器未出现此情况
但是功能实现时没有问题的
*/
public class TreeWebVIewController {

        public WebView treeWebView;


        public void initialize() {


                final WebEngine webEngine = treeWebView.getEngine();
                webEngine.load(Objects.requireNonNull(getClass().getResource("TreeWebView.html")).toExternalForm());
//                treeWebView.setContextMenuEnabled(false); //右键菜单，在fxml中禁用不了，应该是bug



        }



}
