package com.zx.Justmeplush.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @author Zhenxi on 2020-07-16
 */
public class ClassUtils {
    public static void getClassMethodInfo(Class c) {
        if(c==null){
            return;
        }
        CLogUtils.e("当前Class名字是 "+c.getName());
        for (Method method : c.getDeclaredMethods()) {
            CLogUtils.e("方法名字 " + method.getName() + " 方法参数 " + Arrays.toString(method.getParameterTypes()));
        }
    }

    public static void getClassFieldInfo(Class c) {
        if(c==null){
            return;
        }
        for (Field method : c.getDeclaredFields()) {
            CLogUtils.e("字段类型 " + method.getName() + " 方法参数 " + method.getType().getName());
        }
    }
}
