package zhenxi.xposedinto.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lyh on 2018/9/18.
 */

public class PermissionUtils {


    //判断哪些权限 未授权
    private static List<String> mPermissionList = new ArrayList<>();


    private static String[] PERMISSIONS;

//            Manifest.permission.READ_EXTERNAL_STORAGE,
//            Manifest.permission.WRITE_EXTERNAL_STORAGE,
//            Manifest.permission.READ_PHONE_STATE,


    /**
     * 初始化权限的方法
     */
    public static void initPermission(Activity activity, String[] PERMISSIONS_STORAGE) {
        PERMISSIONS = PERMISSIONS_STORAGE;
        //先检查权限 如果是 6.0一下直接 过
        if (isGetPermission(activity, PERMISSIONS_STORAGE)) {
            return;
        } else {
            ActivityCompat.requestPermissions(activity, mPermissionList.
                    toArray(new String[mPermissionList.size()]), 1);
        }

    }


    /**
     * 检测全选代码
     *
     * @param Permission 检测权限的 字符串数组
     * @return 是否都获取到
     */
    private static boolean isGetPermission(Context context, String[] Permission) {
        //大于6.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //检测权限的 集合
            mPermissionList.clear();
            for (int i = 0; i < Permission.length; i++) {
                if (ContextCompat.checkSelfPermission(context, Permission[i]) != PackageManager.PERMISSION_GRANTED) {
                    //将没有获取到的权限 加到集合里
                    mPermissionList.add(Permission[i]);
                }

            }
            if (mPermissionList.size() == 0) {
                CopyFile(context);
                return true;
            } else {
                return false;
            }
        }
        //小于6.0直接true
        return true;
    }

    private static void CopyFile(Context context) {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "OkHttpCat";
        String Outpath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "OkHttpCat"+"/"+"Out";
        File file = new File(path);
        File file1 = new File(Outpath);
        if (file.exists()) {
            CLogUtils.e("文件已存在 ");
            file.delete();
        }
        file.mkdirs();
        file1.mkdirs();
        FileUtils.Assets2Sd(context,"bbb.dex",path+"/"+"bbb.dex");
    }


}
