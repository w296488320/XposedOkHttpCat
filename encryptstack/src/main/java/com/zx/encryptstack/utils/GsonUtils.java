package com.zx.encryptstack.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Created by Lyh on
 * 2019/11/12
 */
public class GsonUtils {


    public  static  Gson gson;

    static {
            //除去Private的key
            //支持GZIP解析
           gson = new GsonBuilder().setLenient().create();
    }

    public static Object getClass(String jsonString, Class c) {
        try {
            return  gson.fromJson(jsonString, c);
        } catch (Throwable e) {
            CLogUtils.e("Gson 构建 Object  error "+e.getMessage());
            e.printStackTrace();
            return null;
        }
    }




    public static String obj2str(Object object) {
        try {
           return gson.toJson(object);
        } catch (Throwable e) {
            //CLogUtils.e("Bean转Json失败 "+e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
