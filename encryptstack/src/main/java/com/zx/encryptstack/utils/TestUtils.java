package com.zx.encryptstack.utils;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Zhenxi on 2020/2/14
 */
public class TestUtils {

    @NonNull
    //"202cb962ac59075b964b07152d234b70"
    // 202cb962ac59075b964b07152d234b70
    public static String getMd5(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            md5.update(string.getBytes());
//            Field[] declaredFields = md5.getClass().getSuperclass().getSuperclass().getDeclaredFields();
//            for(Field field:declaredFields){
//                field.setAccessible(true);
//                CLogUtils.e("名字  "+field);
//            }
//            Method[] declaredMethods = md5.getClass().getDeclaredMethods();
//            for(Method method:declaredMethods){
//                CLogUtils.e("方法名字 "+method.getName());
//            }




            byte[] bytes = md5.digest();


            Field msgDitest1 = md5.getClass().getSuperclass().getSuperclass().getDeclaredField("tempArray");
            msgDitest1.setAccessible(true);
            byte[] count = (byte[]) msgDitest1.get(md5);

            String s = new String(count, StandardCharsets.UTF_8);
            CLogUtils.e("777777   "+s);





            //byte[] bytes = md5.digest(string.getBytes());
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result.append(temp);
            }
            return result.toString();
        } catch (Throwable e) {
            CLogUtils.e("错误 "+e.toString());
            e.printStackTrace();
        }
        return "";

    }




}
