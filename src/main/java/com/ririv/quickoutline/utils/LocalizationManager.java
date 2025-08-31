package com.ririv.quickoutline.utils;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;

public class LocalizationManager {
    private static final String DEFAULT_RESOURCE_BUNDLE = "messages"; // 默认资源文件名（无后缀）
    private static Locale locale = getAppLocale();
    private static ResourceBundle resourceBundle;

    static {
        // 2. （可选）设置默认 Locale（如果需要强制覆盖）
//        setLocale(Locale.forLanguageTag("en-US"));

        // 监听语言变化，自动更新资源包
        updateResourceBundle();

    }

    public static Locale getAppLocale() {
//        获取系统默认 Locale
        Locale systemLocale = Locale.getDefault();
        // 检查语言是否为中文（zh）且国家为中国的简体中文（CN）
        boolean isChineseSystemLocale = "zh".equals(systemLocale.getLanguage())
                && "CN".equals(systemLocale.getCountry());

        if (isChineseSystemLocale) {
            return Locale.SIMPLIFIED_CHINESE; // 或 new Locale("zh", "CN")
        } else {
            return Locale.US; // 默认英文
        }
    }

    // 获取当前语言环境
    public static Locale getLocale() {
        return locale;
    }

    // 设置语言环境（自动触发资源更新）
    public static void setLocale(Locale newLocale) {
        locale = newLocale;
        updateResourceBundle();
    }


    // 获取资源包
    public static ResourceBundle getResourceBundle() {
        return resourceBundle;
    }

    // 更新资源包
    private static void updateResourceBundle() {
        resourceBundle = ResourceBundle.getBundle(DEFAULT_RESOURCE_BUNDLE, locale);
    }

    // 格式化日期
    public static String formatDate(Date date) {
        DateFormat format = DateFormat.getDateInstance(DateFormat.DEFAULT, getLocale());
        return format.format(date);
    }

    // 格式化数字
    public static String formatNumber(double number) {
        NumberFormat format = NumberFormat.getNumberInstance(getLocale());
        return format.format(number);
    }

}
