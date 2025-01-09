package com.ririv.quickoutline.textProcess.methods.seq;

import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.textProcess.methods.parser;

import java.util.Arrays;
import java.util.List;


// From ChatGPT
public class EnSeq implements parser,Seq {

    // 定义可以作为章节前缀的关键字
    private final List<String> keywords = Arrays.asList("Chapter", "Section", "Part", "Appendix", "Label");


    // 找到页码部分的起始位置（假设页码部分是以数字结尾）
    private int findPageNumberIndex(String line) {
        if (!Character.isDigit(line.charAt(line.length() - 1))){
            return -1; // 末尾是不是数字
        }

        for (int i = line.length() - 1; i >= 0; i--) {

            if (!Character.isDigit(line.charAt(i))) {
                return i + 1;
            }
        }
        return -1; // 未找到页码
    }

    // 提取章节信息：章节编号和标题
    private String[] divideTitle(String title) {
        for (String keyword : keywords) {
            if (title.startsWith(keyword)) {
                int keywordEndIndex = keyword.length();
                String remaining = title.substring(keywordEndIndex).trim(); // 去掉关键词部分

                int firstSpace = remaining.indexOf(' '); // 寻找编号和标题之间的空格
                if (firstSpace != -1) {
                    String seq = keyword + " " + remaining.substring(0, firstSpace); // 章节编号部分
                    String titleWithoutSeq = remaining.substring(firstSpace).trim(); // 章节标题部分
                    return new String[]{seq, titleWithoutSeq};
                } else {
                    return new String[]{keyword, remaining}; // 没有标题时，只返回编号
                }
            }
        }

        // 处理没有明确关键词的情况，如 "16.4.2 Testing Bayesian Networks"
        int firstSpace = title.indexOf(' ');
        if (firstSpace != -1) {
            String chapterNumber = title.substring(0, firstSpace); // 假设数字编号在标题前
            String chapterTitle = title.substring(firstSpace).trim(); // 剩余部分为标题
            return new String[]{chapterNumber, chapterTitle};
        }

        return null; // 无法解析
    }

    // 获取行首的缩进空白字符
    private String getIndentation(String line) {
        int i = 0;
        while (i < line.length() && Character.isWhitespace(line.charAt(i))) {
            i++;
        }
        return line.substring(0, i);
    }


    @Override
    public Bookmark parseLine(int offset, String line, List<Bookmark> linearBookmarkList) {
        String trimmedLine = line.trim(); // 去掉行首尾的空白字符
        int pageNumIndex = findPageNumberIndex(trimmedLine);
        String titleWithSeq;
        String pageNum;
        if (pageNumIndex != -1) {
            titleWithSeq = trimmedLine.substring(0, pageNumIndex).trim(); // 去掉页码部分
            pageNum = trimmedLine.substring(pageNumIndex).trim(); // 页码部分
        } else {
            titleWithSeq = trimmedLine;
            pageNum = null; // 无页码时为空
        }
        String indent = getIndentation(line);
        String[] titleInfo = divideTitle(titleWithSeq);
        String title;
        String seq;

        if (titleInfo != null) {
            seq =  titleInfo[0];
            title = titleInfo[1];
        } else {
            seq = "";
            title = titleWithSeq;
        }

        int level;
        if (seq == null || seq.isEmpty()) { // 没有seq，采用上一个bookmark的level
            if (linearBookmarkList.isEmpty()){ //没有上一个，即为第一个，也为顶层，level为1
                level = 1;
            }
            else {
                level = linearBookmarkList.getLast().getLevel();
            }
        }
        else{
            level = getLevelByStandardSeq(seq);
        }

        return new Bookmark(title, pageNum == null? null: Integer.parseInt(pageNum)+offset,level);

    }

}
