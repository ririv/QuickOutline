package com.ririv.quickoutline.textProcess.form;


import com.ririv.quickoutline.entity.Bookmark;
import com.ririv.quickoutline.exception.BookmarkFormatException;
import com.ririv.quickoutline.utils.Pair;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


//中英文通用
public class IndentForm extends Form {

    /*    每级缩进占用的空格，一个doc的每级缩进都是一样的
        由于会出现用户输入的文本缩进空格数不同的情况，如有时为是2个，有时为4个。所以不指定，而是用于检测得到*/
    boolean isChecked = false; //经反复尝试，此变量很重要
    String singleIndentStr = ""; //每级缩进占用的空格，初始为一个空格

    //识别单位：行
    final Pattern indentPattern = Pattern.compile(
                       "^(\\s*)?"  //缩进 $1
                    +  "(.*?)" //标题,包含序号 $2
                    +  "[\\s.]*"
                    +  "(-?[0-9]+)?" //页码 $3
                    +  "\\s*$");


    public Pair<Bookmark,Integer> line2BookmarkWithLevel(int offset, String line, int index) {
        Matcher matcher = indentPattern.matcher(line);
        if (matcher.find()) {

            String linePrefix = matcher.group(1); //行缩进
            String title = (matcher.group(2)).trim();

            checkSingleIndentStr(linePrefix);
            int level = getLevelByLinePrefix(linePrefix);

            Integer offsetPageNum = null;
            if (matcher.group(3)!=null){
                offsetPageNum = Integer.parseInt(matcher.group(3)) + offset;
            }


            Bookmark current = new Bookmark(title, offsetPageNum, level);
            current.setIndex(index);
            return new Pair<>(current,level);


        } else {
            throw new BookmarkFormatException(String.format(
                    "添加页码错误\n\"%s\"格式不正确",
                    line),index);
        }
    }

    //检测单个缩进使用的字符串
    //lineIndent第一次不为空字符串后，检测单个缩进使用字符串成功，后面再也不会检测
    public void checkSingleIndentStr(String linePrefix) {
        if (!isChecked && !linePrefix.isEmpty()) {
            this.singleIndentStr = linePrefix;
            this.isChecked = true;
        }
    }


    public int getLevelByLinePrefix(String linePrefix) {
        int level = 1;
        String indentInfo = "level: %d, String: \"" + linePrefix + "\"";

        while (!singleIndentStr.isEmpty() && linePrefix.startsWith(singleIndentStr)) {
            linePrefix = linePrefix.replaceFirst(singleIndentStr, "");
            level++;
        }

        indentInfo = indentInfo.formatted(level);
        System.out.println(indentInfo);

        return level;
    }
}
