package com.ririv.quickoutline.service.syncWithExternelEditor;

import com.ririv.quickoutline.exception.MyUncheckedExceptionHandler;
import com.ririv.quickoutline.service.syncWithExternelEditor.externalEditor.ExternalEditor;
import com.ririv.quickoutline.service.syncWithExternelEditor.externalEditor.VscodeImpl;
import com.ririv.quickoutline.service.syncWithExternelEditor.watchFile.FileSystemsWatcherImpl;
import com.ririv.quickoutline.service.syncWithExternelEditor.watchFile.FileWatcher;
import com.ririv.quickoutline.utils.Pair;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Consumer;

import static com.ririv.quickoutline.utils.FileUtil.writeFile;


//整个程序每次运行时只会创建一次临时文件，避免多余
public class SyncWithExternalEditorService {
    File temp;

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(SyncWithExternalEditorService.class);


    public SyncWithExternalEditorService() {
        try {
            // 若使用文件系统监视器FileSystems.getDefault().newWatchService()实现时，
            // 未找到直接监视文件的方法，应先创建了一个文件夹进行包装
            File tempParentDir = Files.createTempDirectory("contents").toFile();
            temp = File.createTempFile("contents", ".txt", tempParentDir);//临时文件名系统会自动添加   一串数字，以避免重复名

/*            程序运行结束, JVM终止时才真正调用删除
            注意顺序，且两者都要删除    */
            tempParentDir.deleteOnExit();
            temp.deleteOnExit();

            logger.debug("临时文件已创建: {}", temp.getName());

        } catch (IOException e) {
            e.printStackTrace();
        }
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
            before.run();

            fileWatcher.startWatching(temp, sync);
            ExternalEditor externalEditor = new VscodeImpl(temp, pos.x(), pos.y());

            externalEditor.launch(sync);

            fileWatcher.stopWatching();
            after.run();

        };
        Thread t = new Thread(r);
        t.setUncaughtExceptionHandler(new MyUncheckedExceptionHandler(() -> {
            fileWatcher.stopWatching();
//            temp.deleteOnExit();
            after.run();
            handleError.run();
        }));
        t.start(); //线程执行完会自动销毁
    }

    public void writeTemp(String contentsText) {
        writeFile(contentsText, temp);
    }

}
