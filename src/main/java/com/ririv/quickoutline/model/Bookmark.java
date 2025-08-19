package com.ririv.quickoutline.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static com.ririv.quickoutline.textProcess.StringConstants.TwoNormSpace;

//一个顶级目录为一个bookmark
public class Bookmark{
    private static final Logger logger = LoggerFactory.getLogger(Bookmark.class);


    private String title; // 一般情况下 title with seq
    private Integer offsetPageNum; //偏移后的页码，即pdf中的页码，非真实页码，空为无页码
    private final ObservableList<Bookmark> children = FXCollections.observableArrayList();
    private Bookmark parent;
    private int level; // 0为root，1为顶层目录（since v1.0.3，此前版本中-1为root，0为顶层）
    private List<Bookmark> linearBookmarkList; // root节点下将记录原始的线性Bookmark
    private final String id;

    public Bookmark(String title, Integer offsetPageNum, int level) {
        this.id = UUID.randomUUID().toString(); // 为每个 Bookmark 生成唯一的 ID
        this.title = title;
        this.offsetPageNum = offsetPageNum;
        this.level = level;
    }

    //用来创造根结点，非顶级目录
    public static Bookmark createRoot() {
        //"root",原字符串应为"Outlines"
        return new Bookmark("root", null, 0);
    }


    public void setOffsetPageNum(Integer offsetPageNum) {
        this.offsetPageNum = offsetPageNum;
    }

    public boolean isRoot(){
        return (this.parent == null) && (getLevel() == 0);
    }

    public Optional<Integer> getOffsetPageNum() {
        return Optional.ofNullable(offsetPageNum);
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }


    public String getId() {
        return id;
    }

    public int getLevel() {
        return level;
    }

    public int getLevelByStructure() {
        int level = 0;
        Bookmark parent = this.getParent();
        while (parent != null) {
            level++;
            parent = parent.getParent();
        }

//         check
        boolean res = level == this.level;
        if (!res) {
            logger.warn("{}, level（{}）与实际结构层级（{}）不符合",this, this.level, level);
        }
        return level;
    }



    public void changePos(Bookmark parent) {
        this.setParent(parent);
        parent.getChildren().add(this);
        this.getOwnerList().removeIf(current -> current == this);
    }

    public void changePos(Bookmark parent,int index) {
        this.setParent(parent);
        parent.getChildren().add(index,this);
        this.getOwnerList().removeIf(current -> current == this);
    }



    public Bookmark getParent() {
        return parent;
    }

    public void setParent(Bookmark parent) {
        this.parent = parent;
    }

    // 防止忘记设置parent，增加addChild方法
    public void addChild(Bookmark child) {
        this.getChildren().add(child);
        child.setParent(this);
    }

    public void addChild(int index, Bookmark child) {
        this.getChildren().add(index, child);
        child.setParent(this);
    }

    public ObservableList<Bookmark> getChildren() {
        return children;
    }

    //得到该bookmark所属的列表,非子列表
    public ObservableList<Bookmark> getOwnerList() {
        return this.getParent().getChildren();
    }

    public Bookmark getPre() {
        int i = this.getOwnerList().indexOf(this);
        var temp = this.getOwnerList().get(i - 1);
        while (temp.getChildren().size() != 0) {
            temp = temp.getChildren().get(temp.getChildren().size() - 1);
        }
        return temp;
    }

    public List<Bookmark> genLinearList(){
        List<Bookmark> linearList = new ArrayList<>();
        this.traverse(linearList::add);
        return linearList;
    }


    @Override
    public String toString() {
        if (level == 0) return "root";
        else {
            StringBuilder text = new StringBuilder();
            String offsetPageNumStr = getOffsetPageNum().map(String::valueOf).orElse("");
            buildLine(text, level, getTitle(), offsetPageNumStr);
            return text.toString();
        }
    }


    //包含子节点
    public String toTreeText() {
        StringBuilder text = new StringBuilder();
        traverse(e -> {
            String pageNumStr = e.getOffsetPageNum().map(String::valueOf).orElse("");
            buildLine(text, e.getLevelByStructure(),
                    e.getTitle(), pageNumStr);
        });

        return text.toString();
    }

    private void traverse(Consumer<Bookmark> operate) {
        recursiveTraverse(this, operate);
    }

    /*    Note: 递归遍历， 设为static，防止人为错误地修改代码，以致于无限调用 子bookmark 中的此递归方法
          */
    private static void recursiveTraverse(Bookmark bookmark, Consumer<Bookmark> operate) {
        if (bookmark.getLevelByStructure() != 0) { //非根节点时
            operate.accept(bookmark);
        }
        if (bookmark.getChildren().size() != 0) { //不要使用isEmpty方法
            for (Bookmark child : bookmark.getChildren()) {
                recursiveTraverse(child, operate);
            }
        }
    }

    public static void buildLine(StringBuilder text, int level, String title, String pageNum) {
        text.append("\t".repeat(level-1)); //顶层为1，不要缩进
        text.append(title);
        text.append(TwoNormSpace);
        text.append(pageNum);
        text.append("\n");
    }

    public List<Bookmark> getLinearBookmarkList() {
        return linearBookmarkList;
    }

    public void setLinearBookmarkList(List<Bookmark> linearBookmarkList) {
        this.linearBookmarkList = linearBookmarkList;
    }


    public void reconstructTreeByLevel(){
        if (!this.isRoot()) throw new RuntimeException("该方法仅用于根节点");
        else {
            List<Bookmark> bookmarkList = this.getLinearBookmarkList();
            Bookmark last = this;
            for (var current : bookmarkList) {
                last = addLinearlyToBookmarkTree(current, last);
            }
        }
    }

    public void updateLevelByStructureLevel(){
        this.traverse(e -> e.level = e.getLevelByStructure());
    }


    public static Bookmark convertListToBookmarkTree(List<Bookmark> bookmarkList) {
        Bookmark rootBookmark = Bookmark.createRoot();
        Bookmark last = rootBookmark;
        for (var current : bookmarkList) {
            last = addLinearlyToBookmarkTree(current,last);
        }
        rootBookmark.setLinearBookmarkList(bookmarkList);
        return rootBookmark;
    }


    //一定要设置parent
    /**
     * @return currentBookmark - 但应使用last接受返回值，因为add完current，last就得更新了，current变为新的last
     */
    private static Bookmark addLinearlyToBookmarkTree(Bookmark current, Bookmark last) {
        int currentLevel = current.getLevel();
        if (last.getLevelByStructure() == currentLevel) { //同级
            last.getParent().addChild(current);
        } else if (last.getLevelByStructure() < currentLevel) { //进入下一级，不会跳级
            last.addChild(current);
        } else { //回到上级，可能跳级
            Bookmark parent = last.getParent(); //目前last所属层级的parent
            for (int dif = last.getLevelByStructure() - currentLevel; dif != 0; dif--) { //实际current应属于的parent
                parent = parent.getParent();
            }
            parent.addChild(current);
        }

        return current;
    }


}
