package q296488320.xposedinto.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import q296488320.xposedinto.App;
import q296488320.xposedinto.Bean.AppBean;
import q296488320.xposedinto.R;
import q296488320.xposedinto.XpHook.Hook;
import q296488320.xposedinto.utils.Constants;
import q296488320.xposedinto.utils.SpUtil;
import q296488320.xposedinto.utils.ToastUtils;

import static q296488320.xposedinto.config.Key.APP_INFO;

/**
 * Created by lyh on 2019/2/14.
 */
public class MainListViewAdapter extends BaseAdapter{



    private ArrayList<AppBean> data =null;




    private Context mContext;

    public MainListViewAdapter(Context context,ArrayList<AppBean> data){
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


        holder.All.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Save(position);
            }
        });
        return convertView;
    }

    private void Save(int position) {
        SpUtil.putString(mContext,APP_INFO,data.get(position).packageName);
        ToastUtils.showToast(App.getContext(),"保存成功"+ data.get(position).packageName);
    }


    private static class ViewHolder {
        TextView tv_appName, tv_packageName;
        LinearLayout All;
        ImageView iv_appIcon;

        public ViewHolder(View convertView) {
            All=(LinearLayout)convertView.findViewById(R.id.ll_all);
            tv_packageName = (TextView) convertView.findViewById(R.id.tv_packName);
            tv_appName = (TextView) convertView.findViewById(R.id.tv_appName);
            iv_appIcon = (ImageView) convertView.findViewById(R.id.iv_appIcon);
        }

        public static   ViewHolder getHolder(View convertView){
            ViewHolder holder = (ViewHolder) convertView.getTag();
            if(holder==null){
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            }
            return holder;
        }
    }
}
