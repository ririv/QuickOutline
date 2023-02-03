package com.ririv.quickoutline.service.syncWithExternelEditor.externalEditor;

import com.ririv.quickoutline.exception.LaunchExternalEditorException;
import com.ririv.quickoutline.utils.OsTypeUtil;

import java.io.*;
import java.util.function.Consumer;

import static com.ririv.quickoutline.utils.FileUtil.readFile;

public class VscodeImpl implements ExternalEditor {

    final File file;
    final int x;
    final int y;


    public VscodeImpl(File file, int x, int y) {
        this.file = file;
        this.x = x;
        this.y = y;
    }

    private void execCmdAndWaitReturn() {

        Process p;

        String command;
        if (OsTypeUtil.isWindows()) command = "code.cmd -n -w -g %s:%d:%d ";
        else if (OsTypeUtil.isMacOS()) command = "open -a \"Visual Studio Code.app\"";
        else command = "code -n -w -g %s:%d:%d ";

        command = String.format(command, file.getAbsolutePath(), x, y);

        System.out.println(command);
        try {
            p = Runtime.getRuntime().exec(command);
        }
        catch (IOException e){
            throw new LaunchExternalEditorException();
        }

        //读取命令的输出信息
        InputStream is = p.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        try {
            int exitValue = p.waitFor();
            System.out.println(Thread.currentThread().getName() + ": External editor has returned");
            if (exitValue != 0) {
                System.out.println(exitValue);
                //说明命令执行失败
                //可以进入到错误处理步骤中
            }
            //打印输出信息
            String s;
            while ((s = reader.readLine()) != null) {
                System.out.println(s);
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void launch(Consumer<String> operateAfterReturn) {

        execCmdAndWaitReturn();
        operateAfterReturn.accept(readFile(file));

    }
}
