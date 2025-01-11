package com.ririv.quickoutline.service.syncWithExternelEditor.watchFile;

import java.io.File;
import java.util.function.Consumer;

public interface FileWatcher {

    /*
    1.尝试使用org.apache.log4j.helpers.FileWatchdog，失败
    原因：首先注意此类是为log4j服务的，用作平常业务会有些不妥
         另由于其中的默认构造方法super()会执行doOnChange()，而此时sync函数还没有被传递

    2.尝试使用文件锁或改名方式监测文件是否已被其他程序打开，失败
    可能的原因：txt文件在读取后，数据进入缓冲区即释放锁，因此即使是在windows窗口界面下，打开的txt文件依旧可以被改名与删除

    3.使用org.apache.commons.io提供的监听器，成功
         */
    void startWatching(File temp, Consumer<String> onModifyEvent);

    void stopWatching();
}
