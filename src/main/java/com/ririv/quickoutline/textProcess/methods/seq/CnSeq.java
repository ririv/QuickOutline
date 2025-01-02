package com.ririv.quickoutline.textProcess.methods.seq;

import com.ririv.quickoutline.entity.Bookmark;
import com.ririv.quickoutline.exception.BookmarkFormatException;
import com.ririv.quickoutline.textProcess.methods.Form;
import com.ririv.quickoutline.utils.Pair;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ririv.quickoutline.textProcess.PreProcess.OneBlank;
import static com.ririv.quickoutline.textProcess.PreProcess.TwoBlank;


public class CnSeq extends Form implements Seq {
    final Pattern cnPattern = Pattern.compile(
                      "^(\\s*)?"  //缩进$1
                    + "(\\S?\\s?[零一二三四五六七八九十百千0-9]+\\s?(篇|章|节|部分)|[0-9.]+)?"  //序号$2   $3不用
                    + "\\s*"
                    + "(.*?)" //标题$4
                    + "[\\s.]*"
                    + "(-?[0-9]+)?" //页码$5
                    + "\\s*$");


    public Pair<Bookmark, Integer> line2Bookmark(int offset, String line, int index) {
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
            Bookmark current = new Bookmark(title, pageNum, level);
            current.setIndex(index);
            current.setSeq(seq);

            return new Pair<>(current, level);

        } else {
            throw new BookmarkFormatException(String.format(
                    "添加页码错误\n\"%s\"格式不正确",
                    line), index);
        }
    }

    public String standardizeSeq(String rawSeq) {
        Pattern pattern = Pattern.compile("[0-9.]+");
        Matcher matcher = pattern.matcher(rawSeq);
        if (matcher.find()) return matcher.group(0);
        else return "";
    }

}
