package com.ririv.quickoutline.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static com.ririv.quickoutline.textProcess.PreProcess.TwoBlank;

//一个顶级目录为一个bookmark
public class Bookmark{

    //仅在格式为标准格式下使用，如”1.2.5“
    /*
     * TODO：有些书会有”Part Ⅱ“的顶级目录，此时将”1.2.5“向从第二级目录开始处理
     *  一种方案是采用 ”Ⅱ.1.2.5“的形式
     *  另一种方案是，是用postprocess
     * */
    private String seq;
    private String title;
    private Integer offsetPageNum; //偏移后的页码，即pdf中的页码，非真实页码，空为无页码
    private final List<Bookmark> children = new ArrayList<>();
    private Bookmark parent;
    private int index; //行号，非必要，仅用来记录其所在text中的位置信息
    

    public Bookmark(String title, Integer offsetPageNum) {
        this.title = title;
        this.offsetPageNum = offsetPageNum;
    }

    //用来创造根结点，非顶级目录
    public static Bookmark createRoot() {
        //"root",原字符串应为"Outlines"
        return new Bookmark("root", -1);
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public void setOffsetPageNum(Integer offsetPageNum) {
        this.offsetPageNum = offsetPageNum;
    }

    public void setSeq(String seq) {
        if (seq == null) {
            this.seq = null;
        }

        //匹配seq标准格式，如"12.3.10"
        else if (seq.matches("^(\\d+\\.)+\\d+$")) {
            this.seq = seq;
        } else {
            this.seq = null;
        }
    }


    public String getSeq() {
        return seq;
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


    //0为顶级目录，-1为根结点root（隐藏的）
    public int getLevel() {
        int level = -1;
        Bookmark parent = this.getParent();
        while (parent != null) {
            level++;

            parent = parent.getParent();
        }
        ;
//        System.out.println(title+ " "+level);
        return level;
    }

    public boolean isRoot(){
        return getLevel()==-1;
    }

    public boolean isTopLevel(){
        return getLevel()==0;
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
        if (getLevel() == -1) return "root";
        else {
            StringBuilder text = new StringBuilder();
            String offsetPageNumStr = getOffsetPageNum().map(String::valueOf).orElse("");
            buildLine(text, getLevel(), getTitle(), offsetPageNumStr);
            return text.toString();
        }
    }


    //包含子节点
    public String toText() {
        StringBuilder text = new StringBuilder();
        traverse(e -> {
            String pageNumStr = e.getOffsetPageNum().map(String::valueOf).orElse("");
            buildLine(text, e.getLevel(),
                    e.getTitle(), pageNumStr);
        });

        return text.toString();
    }

    public void traverse(Consumer<Bookmark> operate) {
        recursiveTraverse(this, operate);
    }

    /*    Note: 递归，此方法写在工具类中也是一样的，但为了更好地封装，写在了实体类
            设为static，防止人为错误地修改代码，以致于无限调用 子bookmark 中的此递归方法
          */
    private static void recursiveTraverse(Bookmark bookmark, Consumer<Bookmark> operate) {
        if (bookmark.getLevel() != -1) { //非根节点时
            operate.accept(bookmark);
        }
        if (bookmark.getChildren().size() != 0) { //不要使用isEmpty方法
            for (Bookmark child : bookmark.getChildren()) {
                recursiveTraverse(child, operate);
            }
        }
    }

    public static void buildLine(StringBuilder text, int level, String title, String pageNum) {
        text.append("\t".repeat(level));
        text.append(title);
        text.append(TwoBlank);
        text.append(pageNum);
        text.append("\n");
    }

}
