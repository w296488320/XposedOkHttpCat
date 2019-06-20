package q296488320.xposedinto;

import android.Manifest;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Toolbar;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import q296488320.xposedinto.Bean.AppBean;
import q296488320.xposedinto.adapter.MainListViewAdapter;
import q296488320.xposedinto.utils.Constants;
import q296488320.xposedinto.utils.PermissionUtils;
import q296488320.xposedinto.utils.ToastUtils;

public class MainActivity extends AppCompatActivity {

    private ListView mLv_list;
    private ArrayList<AppBean> mAllPackageList=new ArrayList<>();
    private ArrayList<AppBean> CommonPackageList=new ArrayList<>();
    private Toolbar mToolbar;

   String [] permissionList ={
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
        PermissionUtils.initPermission(this,permissionList);
    }

    private void initData() {
        mAllPackageList = getPackageList();
    }




    private void initView() {
        mLv_list = (ListView)findViewById(R.id.lv_list);
        mToolbar = (Toolbar) findViewById(R.id.tb_toolbar);

        mCb_checkbox = (CheckBox) findViewById(R.id.cb_checkbox);
        mMainListViewAdapter = new MainListViewAdapter(this,CommonPackageList);
        mCb_checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    //需要显示 系统 app
                    mMainListViewAdapter.setData(mAllPackageList);
                }else {
                    mMainListViewAdapter.setData(CommonPackageList);
                }
            }
        });
        mToolbar.setTitle("");


 //       mToolbar.inflateMenu(R.menu.main_activity);

//        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
//            @Override
//            public boolean onMenuItemClick(MenuItem item) {
//        //                switch (item.getItemId()) {
////                    case R.id.showSysApp:
////                        //扫一扫
////                        //防止连续点击
////                        if (Constants.isFastClick()){
////                            if(isShowSystemApp){
////                                //如果 是 显示的话 需要 隐藏掉 系统 app
////                                item.g
////                            }else {
////
////                            }
////                        }
////                        break;
////                }
//                return false;
//            }
//        });


        mLv_list.setAdapter(mMainListViewAdapter);

        mLv_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            }
        });
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
                appBean.isSystemApp=false;
            } else {
                // 系统应用
                appBean.isSystemApp=true;
            }
            appBean.appName= packageInfo.applicationInfo.loadLabel(getPackageManager()).toString();
            appBean.packageName = packageInfo.packageName;
            appBean.appIcon = packageInfo.applicationInfo.loadIcon(getPackageManager());

            appBeans.add(appBean);

            if(!appBean.isSystemApp){
                CommonPackageList.add(appBean);
            }

        }
        return appBeans;
    }








}
