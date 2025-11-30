package com.ririv.quickoutline.service.syncWithExternelEditor;

import com.ririv.quickoutline.service.syncWithExternelEditor.externalEditor.ExternalEditor;
import com.ririv.quickoutline.service.syncWithExternelEditor.externalEditor.VscodeImpl;
import com.ririv.quickoutline.service.syncWithExternelEditor.watchFile.FileSystemsWatcherImpl;
import com.ririv.quickoutline.service.syncWithExternelEditor.watchFile.FileWatcher;
import com.ririv.quickoutline.utils.Pair;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static com.ririv.quickoutline.utils.FileUtil.writeFile;


//整个程序每次运行时只会创建一次临时文件，避免多余
public class SyncWithExternalEditorService {
    private final File temp;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(); // Use a managed executor

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(SyncWithExternalEditorService.class);


    public SyncWithExternalEditorService() throws IOException {
        // Let constructor throw exception to be handled by DI framework or caller
        File tempParentDir = Files.createTempDirectory("contents").toFile();
        temp = File.createTempFile("contents", ".txt", tempParentDir);

        tempParentDir.deleteOnExit();
        temp.deleteOnExit();

        logger.debug("临时文件已创建: {}", temp.getName());
    }

    public void exec(Pair<Integer,Integer> pos, Consumer<String> sync,
                     Runnable before, Runnable after, Runnable handleError) {
//        FileWatcher fileWatcher = new CustomFileModifiedWatcherImpl();
        FileWatcher fileWatcher = new FileSystemsWatcherImpl();
/*
            共创建了2个线程：
            1. r: 执行cmd并等待返回（不加这个线程会导致软件阻塞）
            2. fileWatcher: watch文件是否修改并sync
*/
        Runnable r = () -> {
            try {
                before.run();

                fileWatcher.startWatching(temp, sync);
                // Handle potential null for pos
                ExternalEditor externalEditor = new VscodeImpl(temp, pos != null ? pos.x() : 1, pos != null ? pos.y() : 1);

                externalEditor.launch(sync);

            } catch (Exception e) {
                logger.error("Error during external editor execution", e);
                handleError.run();
            } finally {
                // This block guarantees cleanup, regardless of success or failure.
                fileWatcher.stopWatching();
                after.run();
            }
        };
        executor.submit(r); // Submit the task to the executor
    }

    public void writeTemp(String contentsText) {
        writeFile(contentsText, temp);
    }

    public void shutdown() {
        logger.info("Shutting down SyncWithExternalEditorService executor.");
        executor.shutdownNow(); // Attempt to stop all actively executing tasks
    }

}
