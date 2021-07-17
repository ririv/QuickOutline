package com.ririv.quickoutline.process;

import java.util.ArrayList;
import java.util.List;

public class TextProcess {

    private static final String oneSpaceRegex = "[\\s　]";    //若为至少两个空格 " \s+"
    public static final String TwoBlank = "  ";
    public static final String OneBlank = " ";

//    public static List<String> preprocess(String text, boolean isSkipEmptyLine) {
    public static List<String> preprocess(String text) {

        assert text != null;
        List<String> lns = new ArrayList<>();
        String[] lines = text.split("\\n");
        for (String line : lines) {
//            if (line.matches("^"+oneSpaceRegex+"*"+"$") && isSkipEmptyLine) continue; //空行跳过
            if (line.matches("^"+oneSpaceRegex+"*"+"$")) continue; //空行跳过
            line = line.stripTrailing();
            line = normalize(line);
            lns.add(line);
        }
        return lns;
    }

    private static String normalize(String line) {
        line = line.replaceAll(oneSpaceRegex, OneBlank); //规范空格，//将所有单个空白统一，换成单个空格
        line = line.replaceAll("\\. |．", ".");//规范"."
        return line;
    }


    public static void toLine(StringBuilder text, int level, String title, String pageNum) {
        text.append("\t".repeat(level));
        text.append(title);
        text.append(TwoBlank);
        text.append(pageNum);
        text.append("\n");
    }


}
