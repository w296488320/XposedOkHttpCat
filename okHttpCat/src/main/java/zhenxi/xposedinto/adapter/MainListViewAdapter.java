package zhenxi.xposedinto.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import zhenxi.xposedinto.App;
import zhenxi.xposedinto.Bean.AppBean;
import zhenxi.xposedinto.R;
import zhenxi.xposedinto.utils.Constants;
import zhenxi.xposedinto.utils.SpUtil;
import zhenxi.xposedinto.utils.ToastUtils;

import static zhenxi.xposedinto.config.Key.APP_INFO;

/**
 * Created by lyh on 2019/2/14.
 */
public class MainListViewAdapter extends BaseAdapter{



    private ArrayList<AppBean> data;




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


        holder.All.setOnClickListener(v -> Save(position));
        return convertView;
    }

    private void Save(int position) {
        SpUtil.putString(mContext,APP_INFO,data.get(position).packageName);

        ToastUtils.showToast(App.getContext(),"保存成功  "+ data.get(position).packageName );
        //showMutilDialog(position);

    }





    //弹出一个多选对话框
    private void showMutilDialog(int position) {
        //[1]构造对话框的实例
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        //builder.setMessage("");
        builder.setTitle("");
        //final String[] items = {"模式1", "模式2","通杀模式","通杀模式(并打印调用栈)"};
        //builder.setMessage("默认是 模式1 如果 模式1 没有效果 切换 模式2 并重新打开 ");
//        builder.setSingleChoiceItems(items, -1, (dialog, which) -> {
//            if(which==0){
//                //模式1
//                SpUtil.putString(mContext,MODEL,"1");
//            }else  if(which==1){
//                SpUtil.putString(mContext,MODEL,"2");
//            }else if(which==2){
//                SpUtil.putString(mContext,MODEL,"3");
//            }else if(which==3){
//                SpUtil.putString(mContext,MODEL,"4");
//            }
//            //CleanFlag();
//            ToastUtils.showToast(App.getContext(),"保存成功"+ data.get(position).packageName +"   "+ items[which]);
//            dialog.dismiss();
//        });
        //[3]展示对话框  和toast一样 一定要记得show出来
        builder.show();
    }

//    private void CleanFlag() {
////        FileUtils.SaveLoadPackageFlag("0", Key.ConstructorFlag);
////        FileUtils.SaveLoadPackageFlag("0", Key.OnCreateFlag);
//
//        SpUtil.putString(mContext,Key.OnCreateFlag,"0");
//        SpUtil.putString(mContext,Key.ConstructorFlag,"0");
//    }


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
