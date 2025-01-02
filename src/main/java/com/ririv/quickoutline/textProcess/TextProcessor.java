package com.ririv.quickoutline.textProcess;


import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.textProcess.methods.Indent;
import com.ririv.quickoutline.textProcess.methods.LineProcessor;
import com.ririv.quickoutline.textProcess.methods.Method;
import com.ririv.quickoutline.textProcess.methods.seq.CnSeq;
import com.ririv.quickoutline.textProcess.methods.seq.EnSeq;
import com.ririv.quickoutline.textProcess.methods.seq.StdSeq;

import java.util.ArrayList;
import java.util.List;

public class TextProcessor {

    private LineProcessor lineProcessor;


/*预处理应实在用正则表达式匹配前，为了更好地匹配进行地处理
  主要用于字符的统一
  */  /**
     * 判断字符串是否仅包含标准 ASCII 字符
     *
     * @param text 输入字符串
     * @return 如果仅包含标准 ASCII 字符返回 true，否则返回 false
     */
    public static boolean isAscii(String text) {
        return text.chars().allMatch(c -> c >= 0 && c <= 127);
    }

    /**
     * 判断字符串是否包含中文字符
     *
     * @param text 输入字符串
     * @return 如果包含中文字符返回 true，否则返回 false
     */
    public static boolean containsChinese(String text) {
        // 中文字符的 Unicode 范围：\u4e00-\u9fa5
        return text.codePoints().anyMatch(c -> c >= 0x4e00 && c <= 0x9fa5);
    }

    /**
     * 判断字符串是否包含其他非 ASCII 和非中文字符
     *
     * @param text 输入字符串
     * @return 如果包含其他字符返回 true，否则返回 false
     */
    public static boolean containsOtherCharacters(String text) {
        // 非 ASCII 且非中文的字符
        return text.codePoints().anyMatch(c -> !(c >= 0 && c <= 127) && !(c >= 0x4e00 && c <= 0x9fa5));
    }

    private static List<String> preprocess(String text) {
        List<String> lineList = new ArrayList<>();
        String[] lines = text.split("\\n");
        for (String line : lines) {
            if (line.matches("^"+ Constants.OneSpaceRegex +"*"+"$")) continue; //空行跳过
            line = normalize(line);
            lineList.add(line);
        }
        return lineList;
    }

    //注意顺序
    private static String normalize(String line) {
        line = line
                .replaceAll(Constants.OneSpaceRegex, Constants.OneBlank) //; //规范空格，将所有单个空白统一，换成单个空格
                .replaceAll(" ?\\. ?| ?． ?", ".")//规范"."
                .replaceAll("�","") //去掉奇怪的字符
                .stripTrailing(); //去尾部空格。虽然不去也没关系，因为匹配中我也考虑了
        return line;
    }


    //一定要设置parent

    /**
     * @return currentBookmark - 但应使用last接受返回值，因为add完current，last就得更新了，current变为新的last
     */
    private Bookmark addLinearlyToBookmarkTree(Bookmark current, Bookmark last) {
        int currentLevel = current.getLevel();
        if (last.getLevelByStructure() == currentLevel) { //同级
            last.getOwnerList().add(current);
            current.setParent(last.getParent());
        } else if (last.getLevelByStructure() < currentLevel) { //进入下一级，不会跳级
            last.getChildren().add(current);
            current.setParent(last);
        } else { //回到上级，可能跳级
            Bookmark parent = last.getParent(); //目前last所属层级的parent
            for (int dif = last.getLevelByStructure() - currentLevel; dif != 0; dif--) { //实际current应属于的parent
                parent = parent.getParent();
            }
            parent.getChildren().add(current);
            current.setParent(parent);
        }

        return current;
    }

    private List<Bookmark> createLinearBookmarkList(String text, int offset) {
        List<String> preprocessedText = preprocess(text);
        List<Bookmark> linearBookmarkList = new ArrayList<>();

        for (String line : preprocessedText) {
            var current = lineProcessor.processLine(offset, line, linearBookmarkList);
            linearBookmarkList.add(current);
        }
        return linearBookmarkList;
    }


    private Bookmark convertListToBookmarkTree(List<Bookmark> bookmarkList) {
        Bookmark rootBookmark = Bookmark.createRoot();
        Bookmark last = rootBookmark;
        for (var current : bookmarkList) {
            last = addLinearlyToBookmarkTree(current,last);
        }
        return rootBookmark;
    }

    public Bookmark process(String text, int offset, Method method) {
        if (method == Method.INDENT){
            lineProcessor = new Indent();
        } else {
            if (isAscii(text)) {
                lineProcessor = new EnSeq();
            }
            else if (containsChinese(text)) {
                lineProcessor = new CnSeq();
            } else {
                lineProcessor = new StdSeq();
            }
        }

        var linearBookmarkList = createLinearBookmarkList(text,offset);
        var root = convertListToBookmarkTree(linearBookmarkList);
        root.setLinearBookmarkList(linearBookmarkList);
        return root;
    }



}
