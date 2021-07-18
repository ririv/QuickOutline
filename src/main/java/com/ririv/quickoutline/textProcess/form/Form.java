package com.ririv.quickoutline.textProcess.form;


import com.ririv.quickoutline.entity.Bookmark;
import com.ririv.quickoutline.textProcess.PreProcess;

import java.util.List;

public abstract class Form {

    //interface 中的变量必须是 static final，但abstract class不需要
    protected final Bookmark rootBookmark = new Bookmark();

    //一定要设置parent
    /**
     * @return currentBookmark - 但应使用last接受返回值，因为add完current，last就得更新了，current变为新的last
     */
    protected Bookmark addBookmarkByLevel(Bookmark current, Bookmark last, int level) {
        if (level == 0) {
            rootBookmark.getChildren().add(current);
            current.setParent(rootBookmark);
        } else {
            if (last.getLevel() == level) { //同级
                last.getOwnerList().add(current);
                current.setParent(last.getParent());

            } else if (last.getLevel() < level) { //进入下一级，不会跳级
                last.getChildren().add(current);
                current.setParent(last);
            } else { //回到上级，可能跳级
                List<Bookmark> currentList = null;
                for (int dif = last.getLevel() - level; dif != 0; dif--) {
                    currentList = last.getParent().getOwnerList();
                }
                assert currentList != null;
                currentList.add(current);
                current.setParent(last.getParent().getParent());
            }
        }
        return current;
    }


    /**
     * @return rootBookmark 根结点
     */
    public Bookmark generateBookmark(String text, int offset) {
        Bookmark last = rootBookmark;

        //        List<String> preprocessedText = PreProcess.preprocess(text,isSkipEmptyLine);
        List<String> preprocessedText = PreProcess.preprocess(text);

        int i = 1;
        for (String line : preprocessedText) {
            //空行处理
//            if (line.matches("^ *$")){
//                Bookmark current  = new Bookmark(line,null,last.getLevel());
//                current.setIndex(i++);
//                current.setEmpty(true);
//                last = addBookmarkByLevel(current,last,last.getLevel());
//            }
//            else
                last = addBookmarkByLine(offset, last, line,i++);
        }
        return rootBookmark;
    }

    public abstract Bookmark addBookmarkByLine(int offset, Bookmark last, String line,int index);


    //后处理应是对处理完成后对结构进行再调整的处理，用于应对，如"Part Ⅰ","第一部分" TODO
//    public abstract Bookmark postProcess(Bookmark rootBookmark);
}
