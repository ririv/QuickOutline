package com.ririv.quickoutline.view.utils;

import com.ririv.quickoutline.utils.BindText;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Labeled;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;

public class LocalizationManager {
    private static final String DEFAULT_RESOURCE_BUNDLE = "messages"; // 默认资源文件名（无后缀）
    private static final ObjectProperty<Locale> localeProperty = new SimpleObjectProperty<>(getAppLocale());
    private static final ObjectProperty<ResourceBundle> resourceBundleProperty = new SimpleObjectProperty<>();

    static {
        // 2. （可选）设置默认 Locale（如果需要强制覆盖）
//        localeProperty.set(Locale.forLanguageTag("en-US"));

        // 监听语言变化，自动更新资源包
        localeProperty.addListener((obs, oldVal, newVal) -> updateResourceBundle());
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
        return localeProperty.get();
    }

    // 设置语言环境（自动触发资源更新）
    public static void setLocale(Locale locale) {
        localeProperty.set(locale);
    }


    // 获取资源包
    public static ResourceBundle getResourceBundle() {
        return resourceBundleProperty.get();
    }

    // 更新资源包
    private static void updateResourceBundle() {
        Locale currentLocale = localeProperty.get();
        ResourceBundle bundle = ResourceBundle.getBundle(DEFAULT_RESOURCE_BUNDLE, currentLocale);
        resourceBundleProperty.set(bundle);
    }

    // 动态绑定控件文本（自动更新）
    public static ObjectProperty<String> bindText(Object target, String key) {
        return new SimpleObjectProperty<>() {
            @Override
            protected void invalidated() {
                if (target instanceof Labeled) { // 适用于 Label/Button 等
                    ((Labeled) target).setText(getResourceBundle().getString(key));
                }
            }
        };
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


    // 需要现在controller中的initialize()添加
    // LocalizationManager.autoBind(this);
    public static void autoBind(Object controller) {
        // 直接遍历 Controller 的字段
        for (Field field : controller.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(BindText.class)) {
                BindText bindText = field.getAnnotation(BindText.class);
                String resourceKey = bindText.value();

                try {
                    field.setAccessible(true);
                    Node targetNode = (Node) field.get(controller); // 获取字段对应的 UI 控件

                    String text = getResourceBundle().getString(resourceKey);
                    if (targetNode instanceof Labeled) {
                        ((Labeled) targetNode).setText(text);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
