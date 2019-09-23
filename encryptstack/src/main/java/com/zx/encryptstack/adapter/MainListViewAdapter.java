package com.zx.encryptstack.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.zx.encryptstack.App;
import com.zx.encryptstack.Bean.AppBean;
import com.zx.encryptstack.R;
import com.zx.encryptstack.utils.Constants;
import com.zx.encryptstack.utils.FileUtils;
import com.zx.encryptstack.utils.HookSpUtil;
import com.zx.encryptstack.utils.SpUtil;
import com.zx.encryptstack.utils.ToastUtils;

import java.util.ArrayList;

import static com.zx.encryptstack.config.Key.APP_INFO;


/**
 * Created by lyh on 2019/2/14.
 */
public class MainListViewAdapter extends BaseAdapter{



    private ArrayList<AppBean> data;




    private Context mContext;

    public MainListViewAdapter(Context context, ArrayList<AppBean> data){
            this.mContext=context;
            this.data=data;
    }


    public void setData(ArrayList<AppBean> data){

        this.data=data;

        notifyDataSetChanged();
    }


    @Override
    public int getCount() {
        return data==null?0:data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        if(convertView==null){
            convertView=View.inflate(mContext, R.layout.activity_list_item, null);
        }
        ViewHolder holder = ViewHolder.getHolder(convertView);
        AppBean appBean = data.get(position);

        holder.iv_appIcon.setImageBitmap(Constants.drawable2Bitmap(appBean.appIcon));
        holder.tv_appName.setText(appBean.appName);
        holder.tv_packageName.setText(appBean.packageName);

        holder.All.setOnClickListener(v -> Save(position));
        return convertView;
    }

    private void Save(int position) {
        SpUtil.putString(mContext,APP_INFO,data.get(position).packageName);
        //删除 文件 不影响新的 栈信息
        FileUtils.DeleteFile(data.get(position).packageName);
        ToastUtils.showToast(mContext,"保存成功"+ data.get(position).packageName);
    }



    private static class ViewHolder {
        TextView tv_appName, tv_packageName;
        LinearLayout All;
        ImageView iv_appIcon;

        ViewHolder(View convertView) {
            All= convertView.findViewById(R.id.ll_all);
            tv_packageName = convertView.findViewById(R.id.tv_packName);
            tv_appName = convertView.findViewById(R.id.tv_appName);
            iv_appIcon = convertView.findViewById(R.id.iv_appIcon);
        }

        static   ViewHolder getHolder(View convertView){
            ViewHolder holder = (ViewHolder) convertView.getTag();
            if(holder==null){
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            }
            return holder;
        }
    }
}
