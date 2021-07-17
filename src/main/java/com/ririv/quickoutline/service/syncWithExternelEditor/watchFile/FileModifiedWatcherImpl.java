package com.ririv.quickoutline.service.syncWithExternelEditor.watchFile;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.ririv.quickoutline.utils.FileUtil.readFile;

public class FileModifiedWatcherImpl implements FileWatcher {
    Long oldLastModified;
    Long newLastModified;
    final Consumer<String> sync;
    ScheduledExecutorService service;

    final File temp;
    public FileModifiedWatcherImpl(File temp,Consumer<String> sync) {
        this.temp = temp;
        this.sync = sync;

    }


    @Override
    public void start() {

        oldLastModified = temp.lastModified();

        Runnable r = () -> {
            newLastModified = temp.lastModified();
            if (!oldLastModified.equals(newLastModified)){
                oldLastModified = newLastModified;
                sync.accept(readFile(temp));
            }
            System.out.println(Thread.currentThread().getName()+": is watching file");

        };

        service = Executors
                .newSingleThreadScheduledExecutor();
        // 第二个参数为首次执行的延时时间，第三个参数为定时执行的间隔时间，单位：s
        service.scheduleAtFixedRate(r, 0, 2, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        service.shutdown(); //需要关闭
    }


}
