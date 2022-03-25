package com.ririv.quickoutline.textProcess.form.seq;

import com.ririv.quickoutline.exception.BookmarkFormatException;
import com.ririv.quickoutline.entity.Bookmark;
import com.ririv.quickoutline.textProcess.form.Form;
import com.ririv.quickoutline.utils.Pair;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ririv.quickoutline.textProcess.PreProcess.OneBlank;
import static com.ririv.quickoutline.textProcess.PreProcess.TwoBlank;


public class CnSeqForm extends Form implements SeqForm {
    final Pattern cnPattern = Pattern.compile(
                      "^(\\s*)?"  //缩进$1
                    + "(\\S?\\s?[零一二三四五六七八九十百千0-9]+\\s?(篇|章|节|部分)|[0-9A-Z.]+)?"  //序号$2   $3不用
                    + "\\s*"
                    + "(.*?)" //标题$4
                    + "[\\s.]*"
                    + "(-?[0-9]+)?" //页码$5
                    + "\\s*$");


    public Pair<Bookmark, Integer> line2BookmarkWithLevel(int offset, String line, int index) {
        Matcher standard = standardPattern.matcher(line);
        Matcher matcher = cnPattern.matcher(line);
        if (matcher.find()) {
            String lnIndent = matcher.group(1); //行缩进
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

            int level = checkLevelBySeq(seq);
            Bookmark current = new Bookmark(title, pageNum);
            current.setIndex(index);
            current.setSeq(seq);

            return new Pair<>(current, level);

        } else {
            throw new BookmarkFormatException(String.format(
                    "添加页码错误\n\"%s\"格式不正确",
                    line), index);
        }
    }


}
