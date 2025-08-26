package com.ririv.quickoutline.textProcess;

public class StringConstants {
    public static final String OneNormSpace = " ";
    public static final String TwoNormSpace = OneNormSpace + OneNormSpace;
    static final String OneSpaceRegex = "[\\s　]";    //若为至少两个空格 " \s+"
    public static String FourNormSpace = OneNormSpace + OneNormSpace + OneNormSpace + OneNormSpace; //默认分隔符为三个空格
    public static final String OneNormDot = ".";
    static final String OneDotRegex = "[\\.．]";

}
