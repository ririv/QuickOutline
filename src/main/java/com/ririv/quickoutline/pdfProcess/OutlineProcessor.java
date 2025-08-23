package com.ririv.quickoutline.pdfProcess;



import com.ririv.quickoutline.model.Bookmark;

import java.io.IOException;

public interface OutlineProcessor {

/*   注意数据流动顺序为：
     text(由窗口中获得) -> linearBookmarkList -> rootBookmark(自定义的目录树) -> 1. outline(pdf库的目录树) -> text(生成的目录树，已排版)
                                                                          -> 2. text(生成的目录树，已排版)

     对于一个pdf文件对应生成一个Process实例的尝试：无法在创建一个新实例时关闭上次所打开的pdfDoc（调用close方法）
     当再次使用上次被打开的pdfDoc时，会报错head not found
     因此这里调用函数时打开doc，并在函数中即时关闭它
*/
    void setOutline(Bookmark rootBookmark, String srcFilePath, String destFilePath, ViewScaleType scaleType) throws IOException;

    Bookmark getOutlineAsBookmark(String srcFilePath, int offset) throws IOException;

    String getContents(String srcFilePath, int offset) throws IOException;

    void deleteOutline(String srcFilePath, String destFilePath) throws IOException;

    boolean checkEncrypted(String srcFilePath) throws IOException;
}
