package com.ririv.quickoutline.textProcess;


import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.textProcess.methods.Indent;
import com.ririv.quickoutline.textProcess.methods.Method;
import com.ririv.quickoutline.textProcess.methods.parser;
import com.ririv.quickoutline.textProcess.methods.seq.CnSeq;
import com.ririv.quickoutline.textProcess.methods.seq.StdSeq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.ririv.quickoutline.model.Bookmark.convertListToBookmarkTree;

public class TextProcessor {

    private static final Logger log = LoggerFactory.getLogger(TextProcessor.class);
    private parser parser;


/*预处理应实在用正则表达式匹配前，为了更好地匹配进行地处理
  主要用于字符的统一
  */  /**
     * 判断字符串是否仅包含标准 ASCII 字符
     *
     * @param text 输入字符串
     * @return 如果仅包含标准 ASCII 字符返回 true，否则返回 false
     */
    public static boolean isAscii(String text) {
//        "’"的Unicode码为U+2019，不在ASCII码范围内
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c > 127) {
                log.info("非ASCII字符：{}", c);
                return false;
            }
        }
        return true;

//        return text.chars().allMatch(c -> c >= 0 && c <= 127);
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
            if (line.matches("^"+ StringConstants.OneSpaceRegex +"*"+"$")) continue; //空行跳过
            line = normalize(line);
            lineList.add(line);
        }
        return lineList;
    }

    //注意顺序
    private static String normalize(String line) {
        String newLine = line
                .replaceAll(StringConstants.OneSpaceRegex, StringConstants.OneNormSpace) //; //规范空格，将所有单个空白统一，换成单个空格
                .replaceAll(" ?\\. ?| ?． ?", ".")//规范"."
                .replaceAll("�","") //去掉奇怪的字符
                .stripTrailing(); //去尾部空格。虽然不去也没关系，因为匹配中我也考虑了
        return newLine;
    }


    private List<Bookmark> createLinearBookmarkList(String text, int offset) {
        List<String> preprocessedText = preprocess(text);
        return parser.parse(preprocessedText, offset);
    }


    public Bookmark process(String text, int offset, Method method) {
        if (method == Method.INDENT){
            parser = new Indent();
        } else {
//            if (isAscii(text)) {
//                parser = new EnSeq(); //有bug
//            } else
            if (containsChinese(text)) {
                parser = new CnSeq();
            } else {
                parser = new StdSeq();
            }
        }

        var linearBookmarkList = createLinearBookmarkList(text, offset);
        return convertListToBookmarkTree(linearBookmarkList);
    }

}
