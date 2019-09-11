package q296488320.xposedinto;

import android.app.Application;
import android.content.Context;

import com.tencent.bugly.crashreport.CrashReport;

import q296488320.xposedinto.utils.CLogUtils;

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
    }

    private void initBugly() {
        CrashReport.initCrashReport(getApplicationContext(), "d498dfa281", false);
    }


    public static Context  getContext(){
        return mContext ;
    }
}
