package com.ririv.quickoutline.textProcess;

public class StringConstants {
    public static final String NORM_SPACE = " ";
    public static final String TWO_NORM_SPACE = NORM_SPACE + NORM_SPACE;
    public static final String ONE_SPACE_REGEX = "[\\s　]";    //若为至少两个空格 " \s+"
    public static final String FOUR_NORM_SPACE = NORM_SPACE + NORM_SPACE + NORM_SPACE + NORM_SPACE; //默认分隔符为三个空格
    public static final String NORM_DOT = ".";
    public static final String ONE_DOT_REGEX = "[\\.．]";
    public static final String INDENT_UNIT = "\t"; //缩进单位，一个制表符

}
