package zhenxi.xposedinto.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class HookSpUtil {
	private static SharedPreferences sp;
	/**
	 * 写入boolean变量至sp中
	 * @param ctx	上下文环境
	 * @param key	存储节点名称
	 * @param value	存储节点的值 boolean
	 */
	public static void putBoolean(Context ctx,String key,boolean value){
		//(存储节点文件名称,读写方式)
		if(sp == null){
			sp = ctx.getSharedPreferences("config", Context.MODE_PRIVATE);
		}
		sp.edit().putBoolean(key, value).commit();
	}
	/**
	 * 读取boolean标示从sp中
	 * @param ctx	上下文环境
	 * @param key	存储节点名称
	 * @param defValue	没有此节点默认值
	 * @return		默认值或者此节点读取到的结果
	 */
	public static boolean getBoolean(Context ctx,String key,boolean defValue){
		//(存储节点文件名称,读写方式)
		if(sp == null){
			sp = ctx.getSharedPreferences("config", Context.MODE_PRIVATE);
		}
		return sp.getBoolean(key, defValue);
	}
	
	/**
	 * 写入boolean变量至sp中
	 * @param ctx	上下文环境
	 * @param key	存储节点名称
	 * @param value	存储节点的值string
	 */
	public static void putString(Context ctx,String key,String value){
		//(存储节点文件名称,读写方式)
		if(sp == null){
			sp = ctx.getSharedPreferences("config", Context.MODE_PRIVATE);
		}
		sp.edit().putString(key, value).commit();
	}
	/**
	 * 读取boolean标示从sp中
	 * @param ctx	上下文环境
	 * @param key	存储节点名称
	 * @param defValue	没有此节点默认值
	 * @return		默认值或者此节点读取到的结果
	 */
	public static String getString(Context ctx,String key,String defValue){
		//(存储节点文件名称,读写方式)
		if(sp == null){
			sp = ctx.getSharedPreferences("config", Context.MODE_PRIVATE);
		}
		return sp.getString(key, defValue);
	}
	
	
	/**
	 * 写入boolean变量至sp中
	 * @param ctx	上下文环境
	 * @param key	存储节点名称
	 * @param value	存储节点的值string
	 */
	public static void putInt(Context ctx,String key,int value){
		//(存储节点文件名称,读写方式)
		if(sp == null){
			sp = ctx.getSharedPreferences("config", Context.MODE_PRIVATE);
		}
		sp.edit().putInt(key, value).commit();
	}
	/**
	 * 读取boolean标示从sp中
	 * @param ctx	上下文环境
	 * @param key	存储节点名称
	 * @param defValue	没有此节点默认值
	 * @return		默认值或者此节点读取到的结果
	 */
	public static int getInt(Context ctx,String key,int defValue){
		//(存储节点文件名称,读写方式)
		if(sp == null){
			sp = ctx.getSharedPreferences("config", Context.MODE_PRIVATE);
		}
		return sp.getInt(key, defValue);
	}
	
	
	
	/**
	 * 从sp中移除指定节点
	 * @param ctx	上下文环境
	 * @param key	需要移除节点的名称
	 */
	public static void remove(Context ctx, String key) {
		if(sp == null){
			sp = ctx.getSharedPreferences("config", Context.MODE_PRIVATE);
		}
		sp.edit().remove(key).commit();
	}
}
