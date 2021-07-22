package com.ririv.quickoutline.utils.tree;



import com.ririv.quickoutline.entity.Bookmark;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TreeNode<T> {
    private T t;

    private final List<TreeNode<T>>children = new ArrayList<>();
    private TreeNode<T> parent;
    private int level; //0为顶级目录，-1为根结点（隐藏的）

    public TreeNode<T> getParent() {
        return parent;
    }

    public void setParent(TreeNode<T> parent) {
        this.parent = parent;
    }

    public List<TreeNode<T>> getChildren() {
        return children;
    }

    public T getValue(){
        return t;
    }

    //0为顶级目录，-1为根结点root（隐藏的）
    public int getLevel(){
        int level = -1;
        TreeNode<T> parent = this.parent;
        while (parent != null) {
            level++;

            parent =  this.parent;
        };
        return level;
    }


    //得到该bookmark所属的列表,非子列表
    public List<TreeNode<T>> getOwnerList() {
        if (level != -1){
            return this.parent.children;
        }
        else throw new RuntimeException("为根结点，无法得到当前所属的列表");
    }

    public void traverse(Consumer<TreeNode<T>> operate){
        recursiveTraverse(this,operate);
    }

    /*    Note: 递归，此方法写在工具类中也是一样的，但为了更好地封装，写在了实体类
            设为static，防止人为错误地修改代码，以致于无限调用 子bookmark 中的此递归方法
          */
    private static <T> void recursiveTraverse(TreeNode<T> t, Consumer<TreeNode<T>> operate) {
        if (t.getLevel() != -1) { //非根节点时
            operate.accept(t);
        }
        if (t.getChildren().size() != 0) { //不要使用isEmpty方法
            for (TreeNode<T> child : t.getChildren()) {
                recursiveTraverse(child, operate);
            }
        }
    }

    public void changePos(TreeNode<T> parent){
        this.setParent(parent);
        parent.getChildren().add(this);
        this.getOwnerList().removeIf(current -> current == this);
    }


}
