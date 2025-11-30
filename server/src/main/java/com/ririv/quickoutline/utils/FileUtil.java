package com.ririv.quickoutline.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class FileUtil {
    public static String readFile(File file) {
        String encoding = "UTF-8";
        long fileLength = file.length();
        byte[] fileContent = new byte[(int) fileLength];
        try {
            FileInputStream in = new FileInputStream(file);
            in.read(fileContent);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            return new String(fileContent, encoding);
        } catch (UnsupportedEncodingException e) {
            System.err.println("The OS does not support " + encoding);
            e.printStackTrace();
            return null;
        }
    }
    public static void writeFile(String text,File file){
        try {
//            vscode默认编码为utf-8，所以这里必须指定编码写入
//            BufferedWriter writer = new BufferedWriter (new OutputStreamWriter (new FileOutputStream (file,true),"UTF-8"));

            BufferedWriter writer = new BufferedWriter (new OutputStreamWriter (new FileOutputStream (file), StandardCharsets.UTF_8));
            writer.write(text);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
