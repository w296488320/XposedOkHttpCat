package com.zx.encryptstack.utils;

import android.app.Application;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static android.content.ContentValues.TAG;

/**
 * Created by Lyh on
 * 2019/6/19
 */
public class FileUtils {

    private static final File sdCardDir = Environment.getExternalStorageDirectory();//获取SDCard目录,2.2的时候为:/mnt/sdcart

    /***
     * 调用方式
     *
     * String path = Environment.getExternalStorageDirectory().toString() + "/" + "Tianchaoxiong/useso";
     String modelFilePath = "Model/seeta_fa_v1.1.bin";
     Assets2Sd(this, modelFilePath, path + "/" + modelFilePath);
     *
     * @param context
     * @param fileAssetPath assets中的目录
     * @param fileSdPath 要复制到sd卡中的目录
     */
    public static void Assets2Sd(Context context, String fileAssetPath, String fileSdPath) {
        //测试把文件直接复制到sd卡中 fileSdPath完整路径
        File file = new File(fileSdPath);
        if (!file.exists()) {
            Log.d(TAG, "************文件不存在,文件创建");
            try {
                copyBigDataToSD(context, fileAssetPath, fileSdPath);
                Log.d(TAG, "************拷贝成功");
            } catch (IOException e) {
                Log.d(TAG, "************拷贝失败");
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "************文件夹存在,文件存在");
        }

    }

    public static void copyBigDataToSD(Context context, String fileAssetPath, String strOutFileName) throws IOException {
        InputStream myInput;
        OutputStream myOutput = new FileOutputStream(strOutFileName);
        myInput = context.getAssets().open(fileAssetPath);
        byte[] buffer = new byte[1024];
        int length = myInput.read(buffer);
        while (length > 0) {
            myOutput.write(buffer, 0, length);
            length = myInput.read(buffer);
        }

        myOutput.flush();
        myInput.close();
        myOutput.close();
    }


    public static void saveString(Context mContext, String str, File file) {
        try {
            FileOutputStream outStream = new FileOutputStream(file.getPath(), true);

            OutputStreamWriter writer = new OutputStreamWriter(outStream, StandardCharsets.UTF_8);

            writer.write(str);

            writer.flush();

            writer.close();//记得关闭

            outStream.close();

        } catch (Exception e) {
            CLogUtils.e("file write error " + e);
        }
    }

    public static void DeleteFile(String PackageName) {
        FileWriter fw = null;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File sdCardDir = Environment.getExternalStorageDirectory();
            File filedir = new File(sdCardDir + File.separator + "EncryptStack/" + PackageName + ".txt");  // 这里的AA为创建的AA文件夹，在根目录下
            if (filedir.exists()) {
                filedir.delete();
            }
        }
    }


    /**
     * 关闭Stream对象
     */
    public static void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                // nothing
            }
        }
    }
}
