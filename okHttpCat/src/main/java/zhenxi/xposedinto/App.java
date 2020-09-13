package zhenxi.xposedinto;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

import com.tencent.bugly.crashreport.CrashReport;

import java.security.MessageDigest;

/**
 * Created by lyh on 2019/4/3.
 */

public class App extends Application{
    private static Context mContext;



    @Override
    public void onCreate() {
        super.onCreate();

        mContext=getApplicationContext();
        initBugly();
        Md5Check();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    private void initBugly() {
        CrashReport.initCrashReport(getApplicationContext(), "d498dfa281", false);
    }
    /**
     * 签名MD5效验 截取前10位
     * 不是正确签名 直接退出
     */
    private void Md5Check() {
        try {
            /** 通过包管理器获得指定包名包含签名的包信息 **/
            PackageInfo packageInfo = getPackageManager().getPackageInfo(BuildConfig.APPLICATION_ID, PackageManager.GET_SIGNATURES);
            /******* 通过返回的包信息获得签名数组 *******/
            Signature[] signatures = packageInfo.signatures;

            byte[] bytes = signatures[0].toByteArray();

            MessageDigest localMessageDigest = MessageDigest.getInstance("MD5");
            localMessageDigest.update(bytes);
            byte[] digest = localMessageDigest.digest();

            StringBuilder localStringBuilder = new StringBuilder(2 * digest.length);
            for (int i = 0; ; i++) {
                if (i >= digest.length) {
                    String s = localStringBuilder.toString();
                    if(s.substring(0,10).equals(getString(R.string.sdfffffsd))) {
                        return;
                    }
                    else {



                        //md5没对上
                        //恶意代码
                        while (true){
                            Thread thread =new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    while (true);
                                }
                            });
                            thread.start();
                        }




                    }

                }
                String str = Integer.toString(0xFF & digest[i], 16);
                if (str.length() == 1) {
                    str = "0" + str;
                }
                localStringBuilder.append(str);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(0);
        }

    }

    public static Context  getContext(){
        return mContext ;
    }
}
