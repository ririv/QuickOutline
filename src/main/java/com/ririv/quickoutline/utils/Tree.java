package com.ririv.quickoutline.utils;



import java.util.ArrayList;
import java.util.List;

public class Tree<T> {
    private T t;

    private final List<T> children = new ArrayList<>();
    private Tree<T> parent;
    private int level; //0为顶级目录，-1为根结点（隐藏的）

    public int getLevel() {
        return level;
    }

    public Tree<T> getParent() {
        return parent;
    }

    public void setParent(Tree<T> parent) {
        this.parent = parent;
    }

    public List<T> getChildren() {
        return children;
    }

    public T getValue(){
        return t;
    }


    //得到该bookmark所属的列表,非子列表
    public List<T> getCurrentList() {
        if (level != -1){
            return this.parent.getChildren();
        }
        else throw new RuntimeException("为根结点，无法得到当前所属的列表");
    }

}
