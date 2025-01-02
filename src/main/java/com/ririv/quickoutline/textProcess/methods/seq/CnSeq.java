package com.ririv.quickoutline.textProcess.methods.seq;

import com.ririv.quickoutline.exception.BookmarkFormatException;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.textProcess.methods.LineProcessor;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ririv.quickoutline.textProcess.Constants.OneBlank;
import static com.ririv.quickoutline.textProcess.Constants.TwoBlank;




/*   针对不同标题中的seq，有些标题甚至没有seq，应该进行自我识别，而不是让用户操作
     不同的书目录格式都不一样
     建议两种方式：
     1. 直接让AI大模型帮忙格式化
     2. 采用类似 Chrome 插件 Google Scholar PDF Reader 的方法自己整理出大纲替换目录
     */

public class CnSeq implements LineProcessor,Seq {
    final Pattern cnPattern = Pattern.compile(
                      "^(\\s*)?"  //缩进$1
                    + "(\\S?\\s?[零一二三四五六七八九十百千0-9]+\\s?(篇|章|节|部分)|[0-9.]+)?"  //序号$2   $3不用
                    + "\\s*"
                    + "(.*?)" //标题$4
                    + "[\\s.]*"
                    + "(-?[0-9]+)?" //页码$5
                    + "\\s*$");


    public Bookmark processLine(int offset, String line, List<Bookmark> linearBookmarkList) {
        Matcher matcher = cnPattern.matcher(line);
        if (matcher.find()) {
            String rawSeq = matcher.group(2) != null ? matcher.group(2) : ""; //原seq字符串
            rawSeq = rawSeq.replaceAll(OneBlank, "");
            String seq = standardizeSeq(rawSeq); //检测到的seq
            String title = (rawSeq + TwoBlank + matcher.group(4)).trim();
            Integer pageNum;

            if (matcher.group(5) != null) { //页码
                pageNum = Integer.parseInt(matcher.group(5)) + offset;
            } else { //页码为空
                pageNum = null;
            }

            int level = getLevelByStandardSeq(seq);

            return new Bookmark(title, pageNum, level);

        } else {
            throw new BookmarkFormatException(String.format(
                    "添加页码错误\n\"%s\"格式不正确",
                    line));
        }
    }

    /*
    转换 rawSeq，如：第1章 -> 1
    返回 standard seq, 如：1.2.5
    */
    String standardizeSeq(String rawSeq) {
        Pattern pattern = Pattern.compile("[0-9.]+");
        Matcher matcher = pattern.matcher(rawSeq);
        if (matcher.find()) return matcher.group(0);
        else return "";
    }

}
