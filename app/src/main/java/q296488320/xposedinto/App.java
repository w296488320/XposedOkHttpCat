package q296488320.xposedinto;

import android.app.Application;
import android.content.Context;

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
    }


    public static Context  getContext(){
        return mContext ;
    }
}
