package com.ririv.quickoutline.service.syncWithExternelEditor.externalEditor;

import com.ririv.quickoutline.service.syncWithExternelEditor.externalEditor.exceptions.LaunchExternalEditorException;
import com.ririv.quickoutline.utils.InfoUtil;
import com.ririv.quickoutline.utils.OsDesktopUtil;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.Consumer;

import static com.ririv.quickoutline.utils.FileUtil.readFile;

public class VscodeImpl implements ExternalEditor {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(VscodeImpl.class);

    final File file;

//    position
    final int line;
    final int character;


    public VscodeImpl(File file, int line, int character) {
        this.file = file;
        this.line = line;
        this.character = character;
    }

    private void execCmdAndWaitReturn() {

        String[] command;
        String gotoArg = "%s:%d:%d".formatted(file.getAbsolutePath(), line, character);
        if (OsDesktopUtil.isWindows()) {
            command = new String[]{"code.cmd", "-n", "-w", "-g", gotoArg};
        }
        else {
            command = new String[]{"code", "-n", "-w", "-g", gotoArg};
        }

        logger.info("是否在沙盒中：{}", InfoUtil.isSandboxed());

        logger.info("Executing command: {}", String.join(" ", command));

        try {
            // 使用 exec(String[] cmdarray) 来执行命令
            Process p = Runtime.getRuntime().exec(command);

            // 读取命令的输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            int exitValue = p.waitFor();

            logger.info("{}: 外部编辑器已返回", Thread.currentThread().getName());

            if (exitValue != 0) {
                logger.error("外部编辑器命令执行失败，exitValue: {}", exitValue);
            }

            // 打印输出信息
            String s;
            while ((s = reader.readLine()) != null) {
                logger.info("VSCode log: {}", s);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new LaunchExternalEditorException(); // 可以抛出自定义异常
        }
    }


    @Override
    public void launch(Consumer<String> operateAfterReturn) {

        execCmdAndWaitReturn();
        operateAfterReturn.accept(readFile(file));

    }
}
