package zhenxi.xposedinto.Bean;

import android.graphics.drawable.Drawable;

/**
 * Created by lyh on 2019/2/14.
 */

public class AppBean {


    public String appName;

    public String packageName;

    public Drawable appIcon;


    public boolean isSystemApp=true;


    @Override
    public String toString() {
        return "AppBean{" +
                "appName='" + appName + '\'' +
                ", packageName='" + packageName + '\'' +
                ", appIcon=" + appIcon +
                ", isSystemApp=" + isSystemApp +
                '}';
    }
}
