package com.ririv.quickoutline.textProcess;

import java.util.ArrayList;
import java.util.List;

/*预处理应实在用正则表达式匹配前，为了更好地匹配进行地处理
主要用于字符的统一*/
public class PreProcess {

    private static final String oneSpaceRegex = "[\\s　]";    //若为至少两个空格 " \s+"
    public static final String TwoBlank = "  ";
    public static final String OneBlank = " ";

//    public static List<String> preprocess(String text, boolean isSkipEmptyLine) {
    public static List<String> preprocess(String text) {

        List<String> lineList = new ArrayList<>();
        String[] lines = text.split("\\n");
        for (String line : lines) {
//            if (line.matches("^"+oneSpaceRegex+"*"+"$") && isSkipEmptyLine) continue; //空行跳过
            if (line.matches("^"+oneSpaceRegex+"*"+"$")) continue; //空行跳过
            line = normalize(line);
            lineList.add(line);
        }
        return lineList;
    }

    //注意顺序
    private static String normalize(String line) {
        line = line
                .replaceAll(oneSpaceRegex, OneBlank) //; //规范空格，将所有单个空白统一，换成单个空格
                .replaceAll(" ?\\. ?| ?． ?", ".")//规范"."
                .replaceAll("�","") //去掉奇怪的字符
                .stripTrailing(); //去尾部空格。虽然不去也没关系，因为匹配中我也考虑了
        return line;
    }





}
