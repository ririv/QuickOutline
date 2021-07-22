package com.ririv.quickoutline.textProcess.form;


import com.ririv.quickoutline.exception.BookmarkFormatException;
import com.ririv.quickoutline.entity.Bookmark;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


//中英文通用
public class IndentForm extends Form {

    /*    每级缩进占用的空格，一个doc的每级缩进都是一样的
        由于会出现用户输入的文本缩进空格数不同的情况，如有时为是2个，有时为4个。所以不指定，而是用于检测得到*/
    boolean isChecked = false; //经反复尝试，此变量很重要
    String blankOfOneLevel = ""; //每级缩进占用的空格，初始为一个空格

    final Pattern indentPatternOfLine = Pattern.compile(
                       "^(\\s*)?"  //缩进 $1
                    +  "(.*?)" //标题,包含序号 $2
                    +  "[\\s.]*"
                    +  "(-?[0-9]+)?" //页码 $3
                    +  "\\s*$");


    public Bookmark addBookmarkByLine(int offset, Bookmark last, String line,int index) {
        Matcher matcher = indentPatternOfLine.matcher(line);
        if (matcher.find()) {

            String lnIndent = matcher.group(1); //行缩进
            String title = (matcher.group(2)).trim();

            int level = checkLevelByLnIndent(lnIndent);

            Integer pageNum = null;
            if (matcher.group(3)!=null){
                pageNum = Integer.parseInt(matcher.group(3)) + offset;
            }


            Bookmark current = new Bookmark(title, pageNum);
            current.setIndex(index);
            last = addBookmarkByLevel(current, last, level); //更新last


        } else {
            throw new BookmarkFormatException(String.format(
                    "添加页码错误\n\"%s\"格式不正确",
                    line),index);
        }
        return last;
    }

    @Override
    public void postProcess(Bookmark rootBookmark) {
    }


    public int checkLevelByLnIndent(String lineIndent) {
        int level = 0;
        System.out.print("\"" + lineIndent + "\"" + " level: ");

        //检测单个缩进
        if (!isChecked && !lineIndent.isEmpty()) { //行第一次不为空字符串后，检测单个缩进成功，后面再也不会进入
            blankOfOneLevel = lineIndent;
            isChecked = true;
        }

        while (!blankOfOneLevel.isEmpty() && lineIndent.startsWith(blankOfOneLevel)) {
            lineIndent = lineIndent.replaceFirst(blankOfOneLevel, "");
            level++;
        }


        System.out.println(level);

        return level;
    }
}
