package com.ririv.quickoutline.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static com.ririv.quickoutline.textProcess.Constants.TwoBlank;

//一个顶级目录为一个bookmark
public class Bookmark{

    private String title;
    private Integer offsetPageNum; //偏移后的页码，即pdf中的页码，非真实页码，空为无页码
    private final List<Bookmark> children = new ArrayList<>();
    private Bookmark parent;
    private final int level; // 0为root，1为顶层目录（since v1.0.3，此前版本中-1为root，0为顶层）
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

        // check
        boolean res = level == this.level;
        if (!res) {
            throw new RuntimeException("level（%d）与实际结构层级（%d）不符合".formatted(this.level, level));
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

    public List<Bookmark> getChildren() {
        return children;
    }

    //得到该bookmark所属的列表,非子列表
    public List<Bookmark> getOwnerList() {
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

    /*    Note: 递归，此方法写在工具类中也是一样的，但为了更好地封装，写在了实体类
            设为static，防止人为错误地修改代码，以致于无限调用 子bookmark 中的此递归方法
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
        text.append(TwoBlank);
        text.append(pageNum);
        text.append("\n");
    }

    public List<Bookmark> getLinearBookmarkList() {
        return linearBookmarkList;
    }

    public void setLinearBookmarkList(List<Bookmark> linearBookmarkList) {
        this.linearBookmarkList = linearBookmarkList;
    }

    public String getId() {
        return id;
    }
}
