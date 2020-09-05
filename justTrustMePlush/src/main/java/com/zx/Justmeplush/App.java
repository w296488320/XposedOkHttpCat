package com.zx.Justmeplush;

import android.app.Application;
import android.content.Context;

/**
 * Created by lyh on 2019/4/3.
 */

public class App extends Application {
    private static Context mContext;




    @Override
    public void onCreate() {
        super.onCreate();
        mContext=getApplicationContext();
    }


    public static Context getContext(){
        return mContext ;
    }
}
