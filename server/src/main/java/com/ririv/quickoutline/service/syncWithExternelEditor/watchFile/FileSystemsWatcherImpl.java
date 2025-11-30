package com.ririv.quickoutline.service.syncWithExternelEditor.watchFile;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.function.Consumer;

import static com.ririv.quickoutline.utils.FileUtil.readFile;

public class FileSystemsWatcherImpl implements FileWatcher {

    private static final Logger logger = LoggerFactory.getLogger(FileSystemsWatcherImpl.class);
    private WatchService watchService;
    private Thread watchThread;
    private boolean isRunning = false;

    /**
     * 启动文件监听服务，只监听指定文件的修改事件。
     *
     * @param file      要监听的文件
     * @param onModifyEvent 文件修改时的回调函数
     */
    public void startWatching(File file, Consumer<String> onModifyEvent) {
        String filePath = file.getAbsolutePath();
        if (isRunning) {
            throw new IllegalStateException("监听服务已在运行");
        }

        Path pathToFile = Paths.get(filePath);
        Path directoryPath = pathToFile.getParent();

        if (!Files.exists(pathToFile)) {
            throw new IllegalArgumentException("文件不存在: " + filePath);
        }

        // 初始化 WatchService
        try {
            watchService = FileSystems.getDefault().newWatchService();
            directoryPath.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
        } catch (IOException e) {
            throw new RuntimeException("初始化 WatchService 失败: " + e.getMessage());
        }

        isRunning = true;

        // 创建线程监听事件
        watchThread = new Thread(() -> {
            logger.info("开始监听文件修改: {}", filePath);
            try {
                while (isRunning) {
                    WatchKey key = watchService.take(); // 阻塞等待事件

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();

                        // 如果是修改事件，并且是目标文件
                        if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                            Path changedFile = directoryPath.resolve((Path) event.context());
                            if (changedFile.equals(pathToFile)) {
                                //VSCode每次保存文件都会修改两次，记事本不会
                                logger.info("文件被修改: {}", filePath);
                                onModifyEvent.accept(readFile(file)); // 调用回调函数
                            }
                        }
                    }

                    // 重置 WatchKey 以继续接收事件
                    if (!key.reset()) {
                        logger.warn("监听键失效，停止监听...");
                        stopWatching();
                        break;
                    }
                }
            } catch (InterruptedException e) {
                logger.info("监听线程中断");
                Thread.currentThread().interrupt();
            } catch (ClosedWatchServiceException e) {
                logger.info("监听服务已关闭");
            } catch (Exception e) {
                logger.error("监听服务发生错误: {}", e.getMessage());
            }
        });

        // 启动监听线程
        watchThread.start();
    }

    /**
     * 停止文件监听服务。
     */
    public void stopWatching() {
        isRunning = false;
        try {
            if (watchService != null) {
                watchService.close();
            }
            if (watchThread != null && watchThread.isAlive()) {
                watchThread.interrupt();
            }
            logger.info("文件监听服务已关闭");
        } catch (IOException e) {
            logger.error("关闭监听服务时发生错误: {}", e.getMessage());
        }
    }

    /**
     * 检查监听服务是否正在运行。
     *
     * @return 是否运行
     */
    public boolean isRunning() {
        return isRunning;
    }
}
