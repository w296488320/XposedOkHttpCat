package zhenxi.xposedinto.utils;

import android.util.Log;


import java.util.logging.Level;
import java.util.logging.Logger;

import zhenxi.xposedinto.config.MainConfig;


public class CLogUtils {

	//规定每段显示的长度
	private static int LOG_MAXLENGTH = 2000;

	private static String TAG = "XposedInto";
	private static String NetTAG = "XposedNet";
	public static void e(String msg){
			if(MainConfig.isDebug) {
				InfiniteLog(TAG, msg);
			}
	}

	private static final Logger logger = Logger.getLogger(NetTAG);


	public static void NetLogger(String msg) {
			InfiniteLog(NetTAG, msg);
	}

	public static void e(String TAG, String msg){
		if(!MainConfig.isDebug) {
			InfiniteLog(TAG, msg);
		}
	}

	/**
	 * log最多 4*1024 长度 这个 方法 可以解决 这个问题
	 * @param TAG
	 * @param msg
	 */
	private static void InfiniteLog(String TAG, String msg) {
		int strLength = msg.length();
		int start = 0;
		int end = LOG_MAXLENGTH;
		for (int i = 0; i < 10000; i++) {
			//剩下的文本还是大于规定长度则继续重复截取并输出
			if (strLength > end) {
				Log.e(TAG + i, msg.substring(start, end));
				start = end;
				end = end + LOG_MAXLENGTH;
			} else {
				Log.e(TAG, msg.substring(start, strLength));
				break;
			}
		}
	}


}
