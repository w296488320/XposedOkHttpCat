package zhenxi.xposedinto;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;

import zhenxi.xposedinto.Bean.AppBean;
import zhenxi.xposedinto.View.Xiaomiquan;
import zhenxi.xposedinto.adapter.MainListViewAdapter;
import zhenxi.xposedinto.utils.PermissionUtils;

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

    private void test() {
        try {
            WifiManager systemService = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            assert systemService != null;
            String ssid = systemService.getConnectionInfo().getSSID();
        } catch (Throwable e) {
            e.printStackTrace();
        }


    }

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
            switch (item.getItemId()) {
                case R.id.AddQQGroup:
                    joinQQGroup();
                    break;
                case R.id.xiaomiquan:
                    xiaomiquan();
                    break;
                case R.id.shiyongshuoming:

                    break;
            }
            return false;
        });


        mLv_list.setAdapter(mMainListViewAdapter);


    }

    private void xiaomiquan() {
        startActivity(new Intent(this, Xiaomiquan.class));
    }
    /****************
     *
     * 发起添加群流程。群号：OkHttpCat交流群(828912339) 的 key 为： how_NwAQvL0wiN_DkC5kGPFSJ3BuUKSG
     * 调用 joinQQGroup(how_NwAQvL0wiN_DkC5kGPFSJ3BuUKSG) 即可发起手Q客户端申请加群 OkHttpCat交流群(828912339)
     *
     * @return 返回true表示呼起手Q成功，返回fals表示呼起失败
     ******************/
    public boolean joinQQGroup() {
        Intent intent = new Intent();
        intent.setData(Uri.parse(this.getString(R.string.joinQQ)));
        // 此Flag可根据具体产品需要自定义，如设置，则在加群界面按返回，返回手Q主界面，不设置，按返回会返回到呼起产品界面    //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent);
            return true;
        } catch (Throwable e) {
            // 未安装手Q或安装的版本不支持
            return false;
        }
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
