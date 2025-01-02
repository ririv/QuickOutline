package com.ririv.quickoutline.textProcess.methods;


import com.ririv.quickoutline.entity.Bookmark;
import com.ririv.quickoutline.textProcess.PreProcess;
import com.ririv.quickoutline.utils.Pair;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class Form {

    //interface 中的变量必须是 static final，但abstract class不需要
    protected final Bookmark rootBookmark = Bookmark.createRoot();

    //key:bookmark, value: 层级
    //不用List，而用有序map，这样就可以直接通过key bookmark修改value level的值
    protected Map<Bookmark, Integer> linearBookmarkLevelMap = new LinkedHashMap<>();

    //一定要设置parent

    /**
     * @return currentBookmark - 但应使用last接受返回值，因为add完current，last就得更新了，current变为新的last
     */
    protected Bookmark addBookmarkByLevel(Bookmark current, Bookmark last, int currentLevel) {

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

    public Map<Bookmark, Integer> createLinearBookmarkMap(String text, int offset) {
        List<String> preprocessedText = PreProcess.preprocess(text);

        int i = 1;
        for (String line : preprocessedText) {
            var current = line2Bookmark(offset, line, i++);
            linearBookmarkLevelMap.put(current.x(),current.y());
        }
//        postProcess(linearBookmarkLevelMap);
        return linearBookmarkLevelMap;
    }


    public Bookmark convertMapToBookmarkTree(Map<Bookmark, Integer> linearBookmarkMap) {
        Bookmark last = rootBookmark;
        for (var current : linearBookmarkMap.keySet()) {
            last = addBookmarkByLevel(current,last,linearBookmarkMap.get(current));
        }
        return rootBookmark;
    }

    /**
     * @return rootBookmark 根结点
     */
    public Bookmark generateBookmarkTree(String text, int offset) {
        var linearBookmarkLevelMap = createLinearBookmarkMap(text,offset);
        return convertMapToBookmarkTree(linearBookmarkLevelMap);
    }

    //返回一个 bookmark,level 键值对
    public abstract Pair<Bookmark,Integer> line2Bookmark(int offset, String line, int index);

}
