package com.ririv.quickoutline.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;



// 使用方式，在控件上添加注解，如@BindText("label.title")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface BindText {
    String value();
}
