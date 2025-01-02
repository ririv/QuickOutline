package com.ririv.quickoutline.textProcess.form.seq;

import com.ririv.quickoutline.entity.Bookmark;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/*   针对不同标题中的seq，有些标题甚至没有seq，应该进行自我识别，而不是让用户操作
     不同的书目录格式都不一样，是否可以考虑通过大量的数据集训练模型来完成这一操作呢，效果应该会很好
     然而这只是我使用空余时间写下的一个小工具，想要获取目录数据集不知道从哪弄，所以暂且不考虑了
     */


public interface SeqForm {

    Pattern standardPattern = Pattern.compile(
            "^(\\s*)?([0-9A-Z.]+)?\\s*(.*?)[\\s.]*(-?[0-9]+)?\\s*$");

/*
   注意标准格式应符合格式。如：1.2.5  我是标题  页码
   如想对其他格式进行操作，应重写下面两个方法
*/

    /*
    转换 rawSeq，如：第1章 -> 1
    返回 standard seq, 如：1.2.5
    */
    default String standardizeSeq(String rawSeq) {
        Pattern pattern = Pattern.compile("[0-9.]+");
        Matcher matcher = pattern.matcher(rawSeq);
        if (matcher.find()) return matcher.group(0);
        else return "";
    }


    default int getLevelBySeq(String seq) {

        int level = 1;
        while (seq.contains(".")) {
            seq = seq.replaceFirst("\\.", "");
            level++;
        }
        return level;
    }

    // 应在生成tree之前，对文本处理
    default List<Bookmark> recognizeStruct(String text){ return null; }


}
