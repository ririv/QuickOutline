package com.ririv.quickoutline.textProcess.methods.seq;

import com.ririv.quickoutline.exception.BookmarkFormatException;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.textProcess.methods.parser;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ririv.quickoutline.textProcess.StringConstants.TWO_NORM_SPACE;


/*
   采用本软件推荐的标准格式
   注意标准格式应符合格式。如：1.2.5  我是标题  67
   如想对其他格式进行操作，应重写下面两个方法
*/



public class StdSeq implements parser,Seq {

    final Pattern stdPattern = Pattern.compile(
            "^(\\s*)?(\\d+(\\.\\d+)*\\.?)?\\s*(.*?)[\\s.]*(-?[0-9]+)?\\s*$");

    @Override
    public Bookmark parseLine(String line, List<Bookmark> linearBookmarkList) {
        Matcher matcher = stdPattern.matcher(line);
        if (matcher.find()) {
            String seq = matcher.group(2) != null ? matcher.group(2) : "";
            String titleWithSeq = (seq + TWO_NORM_SPACE + matcher.group(4)).trim();
            Integer pageNum;

            if (matcher.group(5) != null) { //页码
                pageNum = Integer.parseInt(matcher.group(5));
            } else { //页码为空
                pageNum = null;
            }

            int level = getLevelByStandardSeq(seq);

            return new Bookmark(titleWithSeq, pageNum, level);

        } else {
            throw new BookmarkFormatException(String.format(
                    "添加页码错误\n\"%s\"格式不正确",
                    line));
        }
    }

}
