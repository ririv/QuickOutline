package com.ririv.quickoutline.textProcess.methods.seq;

import com.ririv.quickoutline.entity.Bookmark;

import java.util.List;
import java.util.regex.Pattern;


/*   针对不同标题中的seq，有些标题甚至没有seq，应该进行自我识别，而不是让用户操作
     不同的书目录格式都不一样
     建议两种方式：
     1. 直接让AI大模型帮忙格式化
     2. 采用类似 Chrome 插件 Google Scholar PDF Reader 的方法自己整理出大纲替换目录
     */


public interface Seq {

    Pattern standardPattern = Pattern.compile(
            "^(\\s*)?([0-9.]+)?\\s*(.*?)[\\s.]*(-?[0-9]+)?\\s*$");

/*
   注意标准格式应符合格式。如：1.2.5  我是标题  67
   如想对其他格式进行操作，应重写下面两个方法
*/

    /*
    转换 rawSeq，如：第1章 -> 1
    返回 standard seq, 如：1.2.5
    */
    String standardizeSeq(String rawSeq);


    default int getLevelByStandardSeq(String seq) {

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
