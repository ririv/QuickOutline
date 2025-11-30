package com.ririv.quickoutline.service.syncWithExternelEditor.watchFile;

import org.slf4j.Logger;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.ririv.quickoutline.utils.FileUtil.readFile;

// 自己实现了个文件监视器，可直接监视文件（没有使用该实现）
public class CustomFileModifiedWatcherImpl implements FileWatcher {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(CustomFileModifiedWatcherImpl.class);

    Long oldLastModified;
    Long newLastModified;

    ScheduledExecutorService service;


    @Override
    public void startWatching(File file, Consumer<String> onModifyEvent) {

        oldLastModified = file.lastModified();

        Runnable r = () -> {
            newLastModified = file.lastModified();
            if (!oldLastModified.equals(newLastModified)){
                oldLastModified = newLastModified;
                onModifyEvent.accept(readFile(file));
            }

            logger.info("{}: is watching file", Thread.currentThread().getName());
        };

        service = Executors
                .newSingleThreadScheduledExecutor();
        // 第二个参数为首次执行的延时时间，第三个参数为定时执行的间隔时间，单位：s
        service.scheduleAtFixedRate(r, 0, 2, TimeUnit.SECONDS);
    }

    @Override
    public void stopWatching() {
        service.shutdown(); //需要关闭
    }


}
