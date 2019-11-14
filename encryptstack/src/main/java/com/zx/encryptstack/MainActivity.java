package com.zx.encryptstack;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Toolbar;

import com.zx.encryptstack.Bean.AppBean;
import com.zx.encryptstack.View.Xiaomiquan;
import com.zx.encryptstack.adapter.MainListViewAdapter;
import com.zx.encryptstack.utils.PermissionUtils;

import java.util.ArrayList;
import java.util.List;



public class MainActivity extends AppCompatActivity {

    private ListView mLv_list;
    private ArrayList<AppBean> mAllPackageList = new ArrayList<>();
    private ArrayList<AppBean> CommonPackageList = new ArrayList<>();
    private Toolbar mToolbar;

    String[] permissionList = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };


    private CheckBox mCb_checkbox;
    private MainListViewAdapter mMainListViewAdapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initData();
        initView();
        PermissionUtils.initPermission(this, permissionList);

        //test();
    }

//    private void test() {
//        try {
//            WifiManager systemService = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
//            assert systemService != null;
//            String ssid = systemService.getConnectionInfo().getSSID();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

//
//    }

    private void initData() {
        mAllPackageList = getPackageList();
    }


    private void initView() {
        mLv_list = (ListView) findViewById(R.id.lv_list);
        mToolbar = (Toolbar) findViewById(R.id.tb_toolbar);

        mCb_checkbox = (CheckBox) findViewById(R.id.cb_checkbox);
        mMainListViewAdapter = new MainListViewAdapter(this, CommonPackageList);
        mCb_checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                //需要显示 系统 app
                mMainListViewAdapter.setData(mAllPackageList);
            } else {
                mMainListViewAdapter.setData(CommonPackageList);
            }
        });
        mToolbar.setTitle("");

        mToolbar.inflateMenu(R.menu.main_activity);

        mToolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.xiaomiquan) {
                xiaomiquan();
            }
            return false;
        });


        mLv_list.setAdapter(mMainListViewAdapter);

    }

    private void xiaomiquan() {
        startActivity(new Intent(this, Xiaomiquan.class));
    }



    public ArrayList<AppBean> getPackageList() {
        PackageManager packageManager = getPackageManager();
        List<PackageInfo> packages = packageManager.getInstalledPackages(0);
        ArrayList<AppBean> appBeans = new ArrayList<>();

        for (PackageInfo packageInfo : packages) {
            AppBean appBean = new AppBean();
            // 判断系统/非系统应用
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) // 非系统应用
            {
                appBean.isSystemApp = false;
            } else {
                // 系统应用
                appBean.isSystemApp = true;
            }
            appBean.appName = packageInfo.applicationInfo.loadLabel(getPackageManager()).toString();
            appBean.packageName = packageInfo.packageName;
            appBean.appIcon = packageInfo.applicationInfo.loadIcon(getPackageManager());

            appBeans.add(appBean);

            if (!appBean.isSystemApp) {
                CommonPackageList.add(appBean);
            }

        }
        return appBeans;
    }


}
