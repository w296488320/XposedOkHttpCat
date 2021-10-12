package com.zx.encryptstack.XpHook;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.zx.encryptstack.utils.CLogUtils;
import com.zx.encryptstack.utils.FileUtils;
import com.zx.encryptstack.utils.GsonUtils;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;


/**
 * Created by lyh on 2019/6/18.
 */

public class Hook implements IXposedHookLoadPackage, InvocationHandler {
    private static final String Encryptstack_TAG = "Encryptstack";

    private static final String Base64EncodeToString = "encodeToString";
    private static final String Base64Encode = "encode";
    private static final String Base64Decode = "decode";
    private static final String Md5Digest = "digest";
    private static final String Update = "update";
    private static final String DoFinal = "doFinal";
    private static final String Init = "init";

    /**
     * 保存全部已经Hook的信息
     * 可以随时取消Hook
     */
    //private static ArrayList<XC_MethodHook.Unhook> unHookList = new ArrayList<>();

    /**
     * 进行 注入的 app 名字
     */
    public static volatile String InvokPackage = null;
    //只当前进程使用
    private final SimpleDateFormat mSimpleDateFormat =
            new SimpleDateFormat("MM-dd  MM分:ss秒", Locale.getDefault());

    private static Context mOtherContext;
    private static ClassLoader mLoader;

    private static File mSaveFile;


    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            //CLogUtils.e("加载包名 " + lpparam.packageName);
            XSharedPreferences shared = new XSharedPreferences("com.zx.encryptstack", "config");
            shared.reload();
            InvokPackage = shared.getString("APP_INFO", "");

            //先重启 选择 好 要进行Hook的 app
            if (!"".equals(InvokPackage) && lpparam.packageName.equals(InvokPackage)) {
                CLogUtils.e("发现被Hook App");
                HookAttach();
            }
        } catch (Throwable e) {
            e.printStackTrace();
            CLogUtils.e("Test  Exception  " + e.toString());
        }

    }

    /**
     * 字节数组 转 16 进制
     *
     * @param bArr 数据
     */
    private static String bytesToHexString(byte[] bArr) {
        if (bArr == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(bArr.length);
        String sTmp;

        for (byte b : bArr) {
            sTmp = Integer.toHexString(0xFF & b);
            if (sTmp.length() < 2)
                sb.append(0);
            sb.append(sTmp.toUpperCase());
        }

        return sb.toString();
    }

    /**
     * hook  Attach方法
     */
    private synchronized void HookAttach() {

        XposedHelpers.findAndHookMethod(Application.class, "attach",
                Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        mOtherContext = (Context) param.args[0];
                        mLoader = mOtherContext.getClassLoader();
                        initFilePath();

                    }
                });

        XposedHelpers.findAndHookMethod(Application.class, "onCreate",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        HookEncrypMethod();
                    }
                });

        XposedHelpers.findAndHookMethod(Activity.class, "onCreate",
                Bundle.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        if (param.thisObject instanceof Activity) {
                            Activity activity = (Activity) param.thisObject;
                            initPermissions(activity);
                        }
                    }
                });

    }

    /**
     * 初始化保存的文件目录信息
     */
    private void initFilePath() {
        File sdCardDir = Environment.getExternalStorageDirectory();
        mSaveFile = new File(sdCardDir + File.separator
                + "EncryptStack/" + mOtherContext.getPackageName() + ".txt");
        if (!mSaveFile.exists()) {
            try {
                mSaveFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void initPermissions(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.requestPermissions(new String[]{
                    "android.permission.WRITE_EXTERNAL_STORAGE",
                    "android.permission.READ_EXTERNAL_STORAGE",
                    "android.permission.READ_PHONE_STATE"}, 0);
        }
    }


    private static void HookEncrypMethod() {


        try {
            //String string = Base64.encodeToString(byte[],int);
            try {
                XC_MethodHook.Unhook encodeToString =
                        XposedHelpers.findAndHookMethod(Base64.class, Base64EncodeToString,
                                byte[].class,
                                int.class,
                                new XC_MethodHook() {

                                    @Override
                                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                        super.afterHookedMethod(param);

                                        StringBuilder mStringBuilder = new StringBuilder("Base64.encodeToString(byte[],int);");
                                        mStringBuilder.append("\n");
                                        String Result = param.getResult().toString();
                                        mStringBuilder.append(Base64EncodeToString + "返回结果   ").append(Result).append("\n").
                                                append("Base64.encodeToString(byte[],int)  返回结果 参数1 U8编码是 ").append(getStr((byte[]) param.args[0])).append("\n").
                                                append("Base64.encodeToString(byte[],int)  返回结果 参数1 16进制编码是 ").append(bytesToHexString((byte[]) param.args[0])).append("\n");

                                        getStackTraceMessage(mStringBuilder);

                                        FileUtils.saveString(mOtherContext, mStringBuilder.toString(), mSaveFile);
                                    }
                                }
                        );
                //unHookList.add(encodeToString);
            } catch (Throwable e) {
                Log.e(Encryptstack_TAG, "Hook  Base64.encodeToString(byte[],int); error  " + e);
                e.printStackTrace();
            }

            //byte[] encode = Base64.encode();
            try {
                XC_MethodHook.Unhook base64Encode =
                        XposedHelpers.findAndHookMethod(Base64.class, Base64Encode,
                                byte[].class,
                                int.class,
                                new XC_MethodHook() {
                                    @Override
                                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                        super.afterHookedMethod(param);
                                        StringBuilder mStringBuilder = new StringBuilder(Base64Encode);
                                        mStringBuilder.append("\n");
                                        byte[] Result = (byte[]) param.getResult();

                                        mStringBuilder.append("byte[] encode = Base64.encode(); 返回结果   U8编码 ").append(new String(Result, StandardCharsets.UTF_8)).append("\n");
                                        mStringBuilder.append("byte[] encode = Base64.encode(); 返回结果   16进制 编码 ").append(bytesToHexString(Result)).append("\n");

                                        getStackTraceMessage(mStringBuilder);

                                        FileUtils.saveString(mOtherContext, mStringBuilder.toString(), mSaveFile);
                                    }
                                }
                        );
                //unHookList.add(base64Encode);
            } catch (Throwable e) {
                Log.e(Encryptstack_TAG, "Hook  Base64.encode(); error  " + e);
                e.printStackTrace();
            }

            //byte[] decode = Base64.decode(byte[],int);
            try {
                XC_MethodHook.Unhook resut =
                        XposedHelpers.findAndHookMethod(Base64.class, Base64Decode,
                                byte[].class,
                                int.class,
                                new XC_MethodHook() {


                                    @Override
                                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                        super.afterHookedMethod(param);
                                        StringBuilder mStringBuilder = new StringBuilder("Base64.decode(byte[],int);");

                                        mStringBuilder.append("\n");

                                        byte[] Result = (byte[]) param.getResult();
                                        mStringBuilder.append("返回结果  U8编码 ").append(new String(Result, StandardCharsets.UTF_8)).append("\n").
                                                append("加密之前的 内容 Base64.decode(byte[],int); 参数1  U8编码内容  ").append(getStr((byte[]) param.args[0])).append("\n");
                                        mStringBuilder.append("返回结果十六进制编码 ").append(bytesToHexString(Result));

                                        getStackTraceMessage(mStringBuilder);
                                        FileUtils.saveString(mOtherContext, mStringBuilder.toString(), mSaveFile);
                                    }
                                }
                        );
                //unHookList.add(resut);
            } catch (Throwable e) {
                Log.e(Encryptstack_TAG, "Hook Base64.decode(byte[],int); error " + e);
                e.printStackTrace();
            }

//        byte[] decode = Base64.decode(String,int);
            try {
                XC_MethodHook.Unhook resut =

                        XposedHelpers.findAndHookMethod(Base64.class, Base64Decode,
                                String.class,
                                int.class,
                                new XC_MethodHook() {


                                    @Override
                                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                        super.afterHookedMethod(param);
                                        StringBuilder mStringBuilder = new StringBuilder("Base64.decode(String,int);");

                                        mStringBuilder.append("\n");
                                        byte[] Result = (byte[]) param.getResult();
                                        mStringBuilder.append("返回结果  U8编码 ").append(new String(Result, StandardCharsets.UTF_8)).append("\n");
                                        mStringBuilder.append("返回结果  16进制 编码 ").append(bytesToHexString(Result)).append("\n").
                                                append("加密之前的 内容 Base64.decode(String,int); 参数1内容  ").append(param.args[0].toString()).append("\n");

                                        getStackTraceMessage(mStringBuilder);
                                        FileUtils.saveString(mOtherContext, mStringBuilder.toString(), mSaveFile);
                                    }
                                }
                        );
               // unHookList.add(resut);
            } catch (Throwable e) {
                Log.e(Encryptstack_TAG, "Hook Base64.decode(String,int); error " + e.getMessage());
                e.printStackTrace();
            }


            //MD5 和 sha-1
            // byte[] bytes = md5.digest(string.getBytes());
            // public byte[] digest(byte[] input)
            try {
                Set<XC_MethodHook.Unhook> resut =
                        XposedBridge.hookAllMethods(MessageDigest.class,
                                Md5Digest,
                                new XC_MethodHook() {
                                    @Override
                                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                        super.afterHookedMethod(param);
                                        StringBuilder mStringBuilder = new StringBuilder("MessageDigest->digest();\n");
                                        parseParam(mStringBuilder, param);
                                        getStackTraceMessage(mStringBuilder);
                                        FileUtils.saveString(mOtherContext, mStringBuilder.toString(), mSaveFile);
                                    }
                                }
                        );
                //unHookList.addAll(resut);
            } catch (Throwable e) {
                Log.e(Encryptstack_TAG, "Hook  md5.digest(); error " + e.getMessage());
                e.printStackTrace();
            }


//            MessageDigest msgDitest = MessageDigest.getInstance("SHA-1");
//            msgDitest.update(Bytes[]);

            try {
                Set<XC_MethodHook.Unhook> resut =
                        XposedBridge.hookAllMethods(MessageDigest.class, Update,
                                new XC_MethodHook() {
                                    @Override
                                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                        super.afterHookedMethod(param);
                                        StringBuilder mStringBuilder = new StringBuilder("MessageDigest->update() 无返回结果\n");
                                        parseParam(mStringBuilder, param);
                                        getStackTraceMessage(mStringBuilder);
                                        FileUtils.saveString(mOtherContext, mStringBuilder.toString(), mSaveFile);
                                    }
                                }
                        );
               // unHookList.addAll(resut);
            } catch (Throwable e) {
                Log.e(Encryptstack_TAG, "MessageDigest->update(Bytes[]) 无返回结果 " + e);
                e.printStackTrace();
            }

//

//         MAC（Message Authentication Code，消息认证码算法）是含有密钥散列函数算法，兼容
//         了MD和SHA算法的特性，并在此基础上加入了密钥。因此，我们也常把MAC称为HMAC
//         (keyed-Hash Message Authentication Code)
//         类似如下

//            Mac instance = Mac.getInstance("c");
//            instance.init(new SecretKeySpec("Tb6yTwgSEvbLgLtguw21Q80dR8atTLZ9gbOyX3m9FB0FMGWI60SALA==".
//                    getBytes(), instance.getAlgorithm()));
//            return Base64.encodeToString(instance.doFinal(bArr), 2);

            //HmacSHA1算法
            try {
                Set<XC_MethodHook.Unhook> resut =
                        XposedBridge.hookAllMethods(Mac.class, "doFinal",
                                new XC_MethodHook() {
                                    @Override
                                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                        super.afterHookedMethod(param);
                                        StringBuilder mStringBuilder = new StringBuilder("Mac->doFinal\n");
                                        parseParam(mStringBuilder, param);

                                        getStackTraceMessage(mStringBuilder);
                                        FileUtils.saveString(mOtherContext, mStringBuilder.toString(), mSaveFile);
                                    }
                                }
                        );
                //unHookList.addAll(resut);
            } catch (Throwable e) {
                Log.e(Encryptstack_TAG, "Hook HmacSHA1算法 cipher.doFinal(Byte[]); error " + e);
                e.printStackTrace();
            }


            //SecretKeySpec构造方法
            //这个类保存了一些重要的Key 我们在他初始化完毕以后进行dump
            //构造体1:  public SecretKeySpec(byte[] key, String algorithm)

            try {
                Set<XC_MethodHook.Unhook> resut =
                        XposedBridge.hookAllConstructors(
                                SecretKeySpec.class,
                                new XC_MethodHook() {
                                    @Override
                                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                        super.afterHookedMethod(param);
                                        StringBuilder mStringBuilder = new StringBuilder("SecretKeySpec()构造方法");
                                        mStringBuilder.append(Arrays.toString(param.args)).append("\n");

                                        parseParam(mStringBuilder, param);

                                        getStackTraceMessage(mStringBuilder);
                                        FileUtils.saveString(mOtherContext, mStringBuilder.toString(), mSaveFile);
                                    }
                                }
                        );
               // unHookList.addAll(resut);
            } catch (Throwable e) {
                Log.e(Encryptstack_TAG, "Hook public SecretKeySpec(byte[] key, String algorithm) error " + e.getMessage());
                e.printStackTrace();
            }

            // IvParameterSpec <init>
            try {
                Set<XC_MethodHook.Unhook> resut =
                        XposedBridge.hookAllConstructors(IvParameterSpec.class,
                                new XC_MethodHook() {
                                    @Override
                                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                        super.afterHookedMethod(param);
                                        StringBuilder mStringBuilder = new StringBuilder("IvParameterSpec <init>").append("\n");

                                        parseParam(mStringBuilder, param);
                                        getStackTraceMessage(mStringBuilder);
                                        FileUtils.saveString(mOtherContext, mStringBuilder.toString(), mSaveFile);
                                    }
                                }
                        );
                //unHookList.addAll(resut);
            } catch (Throwable e) {
                Log.e(Encryptstack_TAG, "Hook IvParameterSpec <init>; error " + e);
                e.printStackTrace();
            }


//           Cipher ->getInstance
            try {
                Set<XC_MethodHook.Unhook> resut =
                        XposedBridge.hookAllMethods(Cipher.class, "getInstance",
                                new XC_MethodHook() {
                                    @Override
                                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                        super.afterHookedMethod(param);
                                        StringBuilder mStringBuilder =
                                                new StringBuilder("Cipher.getInstance()\n");
                                        parseParam(mStringBuilder, param);

                                        getStackTraceMessage(mStringBuilder);
                                        FileUtils.saveString(mOtherContext, mStringBuilder.toString(), mSaveFile);
                                    }
                                }
                        );
              //  unHookList.addAll(resut);
            } catch (Throwable e) {
                Log.e(Encryptstack_TAG, "Cipher->getInstance(String)  " + e);
                e.printStackTrace();
            }

            //MessageDigest->getInstance
            try {
                Set<XC_MethodHook.Unhook> resut =
                        XposedBridge.hookAllMethods(MessageDigest.class, "getInstance",
                                new XC_MethodHook() {
                                    @Override
                                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                        super.afterHookedMethod(param);
                                        StringBuilder mStringBuilder =
                                                new StringBuilder("MessageDigest.getInstance()\n");
                                        parseParam(mStringBuilder, param);

                                        getStackTraceMessage(mStringBuilder);
                                        FileUtils.saveString(mOtherContext, mStringBuilder.toString(), mSaveFile);
                                    }
                                }
                        );
               // unHookList.addAll(resut);
            } catch (Throwable e) {
                Log.e(Encryptstack_TAG, "MessageDigest->getInstance(String)  " + e);
                e.printStackTrace();
            }

            //Mac->getInstance
            try {
                Set<XC_MethodHook.Unhook> resut =
                        XposedBridge.hookAllMethods(Mac.class, "getInstance",
                                new XC_MethodHook() {
                                    @Override
                                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                        super.afterHookedMethod(param);
                                        StringBuilder mStringBuilder =
                                                new StringBuilder("Mac.getInstance()\n");
                                        parseParam(mStringBuilder, param);

                                        getStackTraceMessage(mStringBuilder);
                                        FileUtils.saveString(mOtherContext, mStringBuilder.toString(), mSaveFile);
                                    }
                                }
                        );
                //unHookList.addAll(resut);
            } catch (Throwable e) {
                Log.e(Encryptstack_TAG, "Mac->getInstance(String)  " + e);
                e.printStackTrace();
            }


            //构造体1:  SecretKeySpec(byte[] key, int offset, int len, String algorithm)
            try {
                XC_MethodHook.Unhook resut =
                        XposedHelpers.findAndHookConstructor(
                                SecretKeySpec.class,
                                byte[].class,
                                int.class,
                                int.class,
                                String.class,
                                new XC_MethodHook() {
                                    @Override
                                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                        super.afterHookedMethod(param);
                                        StringBuilder mStringBuilder = new StringBuilder("SecretKeySpec(byte[] key, int offset, int len, String algorithm)\n");
                                        parseParam(mStringBuilder, param);
                                        getStackTraceMessage(mStringBuilder);
                                        FileUtils.saveString(mOtherContext, mStringBuilder.toString(), mSaveFile);
                                    }
                                }
                        );
                //unHookList.add(resut);
            } catch (Throwable e) {
                Log.e(Encryptstack_TAG, "Hook public SecretKeySpec(byte[] key, String algorithm) error " + e.getMessage());
                e.printStackTrace();
            }

            // 这个方法AES或者 RSA都有可能用
            // 核心方法
            //byte[] cipher.doFinal(Byte[]);
            try {
                Set<XC_MethodHook.Unhook> resut =
                        XposedBridge.hookAllMethods(Cipher.class, DoFinal,
                                new XC_MethodHook() {
                                    @Override
                                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                        super.afterHookedMethod(param);
                                        StringBuilder mStringBuilder = new StringBuilder("Cipher->doFinal\n");

                                        parseParam(mStringBuilder, param);

                                        getStackTraceMessage(mStringBuilder);
                                        FileUtils.saveString(mOtherContext, mStringBuilder.toString(), mSaveFile);

                                    }
                                }
                        );
                //unHookList.addAll(resut);
            } catch (Throwable e) {
                Log.e(Encryptstack_TAG, "Hook cipher.doFinal(Byte[]); error " + e);
                e.printStackTrace();
            }

            try {
                Set<XC_MethodHook.Unhook> resut =
                        XposedBridge.hookAllMethods(
                                Cipher.class,
                                Init,
                                new XC_MethodHook() {

                                    @Override
                                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                        super.beforeHookedMethod(param);
                                        StringBuilder mStringBuilder = new StringBuilder("cipher.init()")
                                                .append(" 参数长度  ").append(param.args.length).append("\n");
                                        parseParam(mStringBuilder, param);

                                        getStackTraceMessage(mStringBuilder);

                                        FileUtils.saveString(mOtherContext, mStringBuilder.toString(), mSaveFile);
                                    }

                                }
                        );
               // unHookList.addAll(resut);
            } catch (Throwable e) {
                Log.e(Encryptstack_TAG, "Hook cipher.init(Cipher.ENCRYPT_MODE, skeySpec); error " + e);
                e.printStackTrace();
            }


            //new JsonObject();
            try {
                Set<XC_MethodHook.Unhook> resut =
                        XposedBridge.hookAllConstructors(JSONObject.class,
                                new XC_MethodHook() {
                                    @Override
                                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                        super.afterHookedMethod(param);
                                        StringBuilder mStringBuilder = new StringBuilder("Json字符串 JsonObject 构造方法 \n");

                                        if (param.thisObject != null) {

                                            String json = (param.thisObject).toString();
                                            mStringBuilder.append("Json 内容 :  ").append(json).append("\n");
                                            mStringBuilder.append("Json 十六进制编码   : ").append(bytesToHexString(json.getBytes())).append("\n\n");

                                            getStackTraceMessage(mStringBuilder);

                                            FileUtils.saveString(mOtherContext, mStringBuilder.toString(), mSaveFile);
                                        }
                                    }
                                });
                //unHookList.addAll(resut);
            } catch (Throwable e) {
                Log.e(Encryptstack_TAG, "Hook JSONObject 构造error  " + e.getMessage());
                e.printStackTrace();
            }


            Class<?> Gson = null;
            try {
                Gson = XposedHelpers.findClass("com.google.gson.Gson", mLoader);
            } catch (Throwable e) {
                e.printStackTrace();
            }
            if (Gson != null) {
                //Gson->gson.fromJson(jsonString, c);
                //将一个Json字符串,转换成一个对象的方法
                try {
                    XC_MethodHook.Unhook resut =

                            XposedHelpers.findAndHookMethod(
                                    Gson, "fromJson", String.class,
                                    Class.class,
                                    new XC_MethodHook() {
                                        @Override
                                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                            super.afterHookedMethod(param);
                                            StringBuilder mStringBuilder = new StringBuilder("Json字符串 Gson->gson.fromJson(jsonString, c) " + "\n");
                                            String json = (String) param.args[0];
                                            mStringBuilder.append(json).append("\n");
                                            mStringBuilder.append("参数2的Class名字  ").append(param.args[1].getClass().getName()).append("\n");
                                            getStackTraceMessage(mStringBuilder);
                                            FileUtils.saveString(mOtherContext, mStringBuilder.toString(), mSaveFile);
                                        }
                                    }
                            );
                    //unHookList.add(resut);
                } catch (Throwable e) {
                    Log.e(Encryptstack_TAG, "Hook Gson->fromJson  error " + e.getMessage());
                    e.printStackTrace();
                }


                try {
                    Set<XC_MethodHook.Unhook> resut =
                            XposedBridge.hookAllConstructors(
                                    XposedHelpers.findClass("com.google.gson.JsonObject", mLoader),
                                    new XC_MethodHook() {
                                        @Override
                                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                            super.afterHookedMethod(param);
                                            StringBuilder mStringBuilder = new StringBuilder("Json字符串 JsonObject 构造方法 \n");

                                            if (param.thisObject != null) {

                                                String json = (param.thisObject).toString();
                                                mStringBuilder.append("gson->JsonObject 内容 :  ").append(json).append("\n");
                                                mStringBuilder.append("gson->JsonObject 十六进制编码   : ").append(bytesToHexString(json.getBytes())).append("\n\n");

                                                getStackTraceMessage(mStringBuilder);

                                                FileUtils.saveString(mOtherContext, mStringBuilder.toString(), mSaveFile);
                                            }
                                        }
                                    });
                    //unHookList.addAll(resut);
                } catch (Throwable e) {
                    Log.e(Encryptstack_TAG, "Hook JSONObject 构造error  " + e.getMessage());
                    e.printStackTrace();
                }

                try {
                    XC_MethodHook.Unhook resut =

                            XposedHelpers.findAndHookMethod(
                                    Gson, "toJson", Object.class, new XC_MethodHook() {
                                        @Override
                                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                            super.afterHookedMethod(param);
                                            StringBuilder mStringBuilder = new StringBuilder("Json字符串 Gson->toJson(Object) " + "\n");
                                            mStringBuilder.append((String) param.getResult()).append("\n");
                                            mStringBuilder.append("参数1的Class名字  ").append((param.args[0]).getClass().getName()).append("\n");
                                            getStackTraceMessage(mStringBuilder);
                                            FileUtils.saveString(mOtherContext, mStringBuilder.toString(), mSaveFile);
                                        }
                                    }
                            );
                    //unHookList.add(resut);
                } catch (Throwable e) {
                    Log.e(Encryptstack_TAG, "Hook Gson->toJson  error " + e.getMessage());
                    e.printStackTrace();
                }

            }


            Class<?> JSON = null;
            try {
                JSON = XposedHelpers.findClass("com.alibaba.fastjson.JSON", mLoader);
            } catch (Throwable e) {
                e.printStackTrace();
            }

            if (JSON != null) {
                try {
                    Set<XC_MethodHook.Unhook> resut =
                            XposedBridge.hookAllConstructors(
                                    XposedHelpers.findClass("com.alibaba.fastjson.JSONObject", mLoader),
                                    new XC_MethodHook() {
                                        @Override
                                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                            super.afterHookedMethod(param);
                                            StringBuilder mStringBuilder = new StringBuilder("Json字符串 JsonObject 构造方法 \n");

                                            if (param.thisObject != null) {

                                                String json = ((JSONObject) param.thisObject).toString();
                                                mStringBuilder.append("fastjson->JsonObject 内容 :  \n").append(json).append("\n");
                                                mStringBuilder.append("fastjson->JsonObject 十六进制编码   \n: ").append(bytesToHexString(json.getBytes())).append("\n\n");

                                                getStackTraceMessage(mStringBuilder);

                                                FileUtils.saveString(mOtherContext, mStringBuilder.toString(), mSaveFile);
                                            }
                                        }
                                    });
                   // unHookList.addAll(resut);
                } catch (Throwable e) {
                    Log.e(Encryptstack_TAG, "Hook com.alibaba.fastjson.JSONObject 构造 error  " + e.getMessage());
                    e.printStackTrace();
                }

                //fastJson
                //public static final Object parse(String text); // 把JSON文本parse为JSONObject或者JSONArray
                try {
                    XC_MethodHook.Unhook resut =

                            XposedHelpers.findAndHookMethod(JSON, "parse", String.class, new XC_MethodHook() {
                                        @Override
                                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                            super.afterHookedMethod(param);
                                            StringBuilder mStringBuilder = new StringBuilder("Json字符串 FastJson  JSON->parse(String) " + "\n");
                                            mStringBuilder.append("参数1 \n").append((String) param.args[0]).append("\n");
                                            mStringBuilder.append("返回内存类名 ").append(param.getResult().getClass().getName()).append("\n");

                                            getStackTraceMessage(mStringBuilder);
                                            FileUtils.saveString(mOtherContext, mStringBuilder.toString(), mSaveFile);
                                        }
                                    }
                            );
                   // unHookList.add(resut);
                } catch (Throwable e) {
                    Log.e(Encryptstack_TAG, "Hook fastjson.JSON->parse error  " + e.getMessage());
                    e.printStackTrace();
                }
                //public static final JSONObject parseObject(String text)； // 把JSON文本parse成JSONObject
                try {
                    XC_MethodHook.Unhook resut =

                            XposedHelpers.findAndHookMethod(JSON, "parseObject", String.class, new XC_MethodHook() {
                                        @Override
                                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                            super.afterHookedMethod(param);
                                            StringBuilder mStringBuilder = new StringBuilder("Json字符串 FastJson  JSON->parseObject(String) " + "\n");
                                            mStringBuilder.append("参数1 \n").append((String) param.args[0]).append("\n");
                                            mStringBuilder.append("返回内容 ").append(param.getResult().toString()).append("\n");

                                            getStackTraceMessage(mStringBuilder);
                                            FileUtils.saveString(mOtherContext, mStringBuilder.toString(), mSaveFile);
                                        }
                                    }
                            );
                    //unHookList.add(resut);
                } catch (Throwable e) {
                    Log.e(Encryptstack_TAG, "Hook fastjson.JSON->parseObject error  " + e.getMessage());

                    e.printStackTrace();
                }
                //public static final String toJSONString(Object object); // 将JavaBean序列化为JSON文本
                try {
                    XC_MethodHook.Unhook resut =

                            XposedHelpers.findAndHookMethod(JSON, "toJSONString", Object.class, new XC_MethodHook() {
                                        @Override
                                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                            super.afterHookedMethod(param);
                                            StringBuilder mStringBuilder = new StringBuilder("Json字符串 FastJson  JSON->toJSONString(Object) " + "\n");
                                            mStringBuilder.append("参数1名字  \n").append(param.args[0].getClass().getName()).append("\n");
                                            mStringBuilder.append("返回内容 ").append(param.getResult().toString()).append("\n");

                                            getStackTraceMessage(mStringBuilder);
                                            FileUtils.saveString(mOtherContext, mStringBuilder.toString(), mSaveFile);
                                        }
                                    }
                            );
                   // unHookList.add(resut);
                } catch (Throwable e) {
                    Log.e(Encryptstack_TAG, "Hook fastjson.JSON->toJSONString error  " + e.getMessage());

                    e.printStackTrace();
                }
            }


            //JockJson
            Class<?> Jackson = null;
            try {
                Jackson = XposedHelpers.findClass("com.fasterxml.jackson.databind.ObjectMapper", mLoader);
            } catch (Throwable e) {
            }
            if (Jackson != null) {
                try {
                    XC_MethodHook.Unhook resut =

                            XposedHelpers.findAndHookMethod(Jackson, "writeValueAsString", Object.class, new XC_MethodHook() {


                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    super.afterHookedMethod(param);
                                    StringBuilder mStringBuilder = new StringBuilder("Json字符串 Jackson  ObjectMapper->writeValueAsString(Object) " + "\n");
                                    mStringBuilder.append("返回内容 ").append(param.getResult().toString()).append("\n");
                                    mStringBuilder.append("参数1名字  ").append(param.args[0].getClass().getName()).append("\n");

                                    getStackTraceMessage(mStringBuilder);
                                    FileUtils.saveString(mOtherContext, mStringBuilder.toString(), mSaveFile);
                                }
                            });
                    //unHookList.add(resut);
                } catch (Throwable e) {
                    Log.e(Encryptstack_TAG, "Hook Jackson->writeValueAsString error  " + e.getMessage());
                    e.printStackTrace();
                }

                try {
                    XC_MethodHook.Unhook resut =

                            XposedHelpers.findAndHookMethod(Jackson,
                                    "readValue",
                                    String.class,
                                    Class.class,
                                    new XC_MethodHook() {
                                        @Override
                                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                            super.beforeHookedMethod(param);
                                            StringBuilder mStringBuilder = new StringBuilder("Json字符串 Jackson  ObjectMapper->readValue(String content, Class<T> valueType) " + "\n");
                                            mStringBuilder.append("参数1内容  ").append(param.args[0].toString()).append("\n");
                                            mStringBuilder.append("参数2类名  ").append(param.args[1].getClass().getName()).append("\n");


                                            getStackTraceMessage(mStringBuilder);
                                            FileUtils.saveString(mOtherContext, mStringBuilder.toString(), mSaveFile);
                                        }
                                    });
                    //unHookList.add(resut);
                } catch (Throwable e) {
                    Log.e(Encryptstack_TAG, "Hook Jackson->readValue error  " + e.getMessage());
                    e.printStackTrace();
                }
            }

            //Gzip压缩相关,很多网络请求是Gzip压缩的在传输的过程中存在数据加密的情况
            //GZIPOutputStream.class
            try {
                Set<XC_MethodHook.Unhook> resut =
                        //GZIPOutputStream->write 将压缩字节流写进去时候进行拦截
                        XposedBridge.hookAllMethods(GZIPOutputStream.class,
                                "write",
                                new XC_MethodHook() {
                                    @Override
                                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                        super.beforeHookedMethod(param);
                                        StringBuilder mStringBuilder = new StringBuilder("GZIPOutputStream->write()").append("\n");
                                        parseParam(mStringBuilder, param);
                                        getStackTraceMessage(mStringBuilder);
                                        FileUtils.saveString(mOtherContext, mStringBuilder.toString(), mSaveFile);
                                    }
                                });
                //unHookList.addAll(resut);
            } catch (Throwable e) {
                Log.e(Encryptstack_TAG, "hook GZIPOutputStream->write  error " + e.getMessage());
                e.printStackTrace();
            }


            //ByteArrayOutputStream 输出流的Hook
            try {
                XC_MethodHook.Unhook resut =
                        //GZIPOutputStream->write 将压缩字节流写进去时候进行拦截
                        XposedHelpers.findAndHookMethod(ByteArrayOutputStream.class,
                                "toString",
                                String.class,
                                new XC_MethodHook() {

                                    @Override
                                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                        super.afterHookedMethod(param);
                                        StringBuilder mStringBuilder = new StringBuilder("ByteArrayOutputStream->toString(String)").append("\n");
                                        parseParam(mStringBuilder, param);
                                        getStackTraceMessage(mStringBuilder);
                                        FileUtils.saveString(mOtherContext, mStringBuilder.toString(), mSaveFile);
                                    }
                                });
                //unHookList.add(resut);
            } catch (Throwable e) {
                Log.e(Encryptstack_TAG, "hook ByteArrayOutputStream->toString " + e.getMessage());
                e.printStackTrace();
            }

            //ByteArrayOutputStream ->toByteArray
            try {
                XC_MethodHook.Unhook resut =
                        XposedHelpers.findAndHookMethod(ByteArrayOutputStream.class,
                                "toByteArray",
                                new XC_MethodHook() {
                                    @Override
                                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                        super.afterHookedMethod(param);
                                        StringBuilder mStringBuilder = new StringBuilder("ByteArrayOutputStream->toByteArray(String)").append("\n");
                                        parseParam(mStringBuilder, param);
                                        getStackTraceMessage(mStringBuilder);
                                        FileUtils.saveString(mOtherContext, mStringBuilder.toString(), mSaveFile);
                                    }
                                });
                //unHookList.add(resut);
            } catch (Throwable e) {
                Log.e(Encryptstack_TAG, "hook ByteArrayOutputStream->toString " + e.getMessage());
                e.printStackTrace();
            }

            Log.e(Encryptstack_TAG, "Encryptstack Hook 执行完毕 ");
        } catch (Throwable throwable) {
            Log.e(Encryptstack_TAG, "Encryptstack 发现异常 " + throwable.toString());
        }

    }


    @NonNull
    private static String getStr(byte[] arg) {
        if (arg == null || arg.length == 0) {
            return "";
        }
        return new String(arg, StandardCharsets.UTF_8);
    }

    @NonNull
    private String getStr(Object arg) {
        if (arg == null) {
            return "";
        }
        if (arg instanceof byte[]) {
            return new String((byte[]) arg,
                    StandardCharsets.UTF_8);
        }
        return "";
    }


    @NonNull
    private StringBuilder getMd5(byte[] result2) {
        StringBuilder result1 = new StringBuilder();
        if (result2 == null || result2.length == 0) {
            return result1;
        }
        for (byte b : result2) {
            String temp = Integer.toHexString(b & 0xff);
            if (temp.length() == 1) {
                temp = "0" + temp;
            }
            result1.append(temp);
        }
        return result1;
    }

    private static void getStackTraceMessage(StringBuilder mStringBuilder) {
        try {
            mStringBuilder.append("\n");
            throw new Exception("getStackTraceElementException");
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = e.getStackTrace();
            mStringBuilder.append(" --------------------------   " + "\n");
            for (StackTraceElement stackTraceElement : stackTrace) {
                mStringBuilder.append("栈信息      ").append(stackTraceElement.getClassName())
                        .append(".").append(stackTraceElement.getMethodName())
                        .append(" ").append(stackTraceElement.getLineNumber()).append("\n");
            }
            mStringBuilder.append(" --------------------------  " + "\n");
        }
        mStringBuilder.append("\n\n\n\n\n");
    }

    private static byte[] getIvFieldAndIvoke(Object IV) {
        try {
            Method getEncoded = IV.getClass().getDeclaredMethod("getIV");
            getEncoded.setAccessible(true);
            try {
                return (byte[]) getEncoded.invoke(IV);
            } catch (Throwable e) {
                CLogUtils.e("getEncodedMethodAndInvoke  异常   " + e.toString());
                e.printStackTrace();
            }
        } catch (Throwable e) {
            //CLogUtils.e("NoSuchMethodException  异常   " + e.toString());
            e.printStackTrace();
            return null;
        }
        return null;

    }

    private static byte[] getEncodedMethodAndInvoke(Object arg) {
        try {
            Method getEncoded = arg.getClass().getDeclaredMethod("getEncoded");
            getEncoded.setAccessible(true);
            try {
                return (byte[]) getEncoded.invoke(arg);
            } catch (Throwable e) {
                CLogUtils.e("getEncodedMethodAndInvoke  异常   " + e.toString());
                e.printStackTrace();
            }
        } catch (Throwable e) {
            CLogUtils.e("NoSuchMethodException  异常   " + e.toString());
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 获取 加密方法的 模式 是 AES  还是 RSA
     */
    private static String getAlgorithm(Object arg) {
        try {
            Method getEncoded = arg.getClass().getDeclaredMethod("getAlgorithm");
            if (getEncoded != null) {
                getEncoded.setAccessible(true);

                try {
                    return (String) getEncoded.invoke(arg);
                } catch (Throwable e) {
                    CLogUtils.e("getEncodedMethodAndInvoke  异常   " + e.toString());
                }
            }
        } catch (Throwable e) {
            return "";
        }
        return "";
    }

    public String getString(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        return new String(bytes, StandardCharsets.UTF_8);

    }

    /**
     * 获得当前进程的名字
     *
     * @return 进程号
     */
    private String getCurProcessName(Context context) {

        int pid = android.os.Process.myPid();

        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningAppProcessInfo appProcess : Objects.requireNonNull(activityManager)
                .getRunningAppProcesses()) {

            if (appProcess.pid == pid) {
                return appProcess.processName;
            }
        }
        return null;
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        return null;
    }


    /**
     * 主要用于保存key之类的信息
     */
    public static class SpiObject {

        /**
         * blockSize : 16
         * calledUpdate : false
         * cipherCtx : {"address":511974383816}
         * encodedKey : [49,50,51,52,53,54,55,56,57,48,97,98,99,100,101,102]
         * encrypting : true
         * iv : [48,49,48,50,48,51,48,52,48,53,48,54,48,55,48,56]
         * mode : CBC
         * modeBlockSize : 16
         * padding : PKCS5PADDING
         */

        @SerializedName("blockSize")
        public int blockSize;
        @SerializedName("calledUpdate")
        public boolean calledUpdate;
        @SerializedName("cipherCtx")
        public CipherCtxDTO cipherCtx;
        @SerializedName("encrypting")
        public boolean encrypting;
        @SerializedName("mode")
        public String mode;
        @SerializedName("modeBlockSize")
        public int modeBlockSize;
        @SerializedName("padding")
        public String padding;
        @SerializedName("encodedKey")
        public byte[] encodedKey;
        @SerializedName("iv")
        public byte[] iv;

        public static class CipherCtxDTO {
            /**
             * address : 511974383816
             */

            @SerializedName("address")
            public long address;
        }

        public String getKey() {
            if (encodedKey != null) {
                return new String(encodedKey, StandardCharsets.UTF_8);
            }
            return "没有发现Key 或者 Key == null";
        }
    }


    public static class CipherKeyResut {
        public String key;
        public String keyJson;

        public String getKey() {
            if (this.key == null) {
                return "";
            }
            return key;
        }

        public String getKeyJson() {
            if (this.keyJson == null) {
                return "";
            }
            return keyJson;
        }

        @Override
        public String toString() {
            return "CipherKeyResut{" +
                    "key='" + key + '\'' +
                    ", keyJson='" + keyJson + '\'' +
                    '}';
        }
    }


    private static Field keyField;

    /**
     * 获取Cipher实例中的Key内容
     */
    private static CipherKeyResut getSpiKey(Cipher cipher) {
        Object spiObject;
        CipherKeyResut cipherKeyResut = new CipherKeyResut();
        try {
            if (keyField == null) {
                try {
                    keyField = Cipher.class.getDeclaredField("spi");
                    keyField.setAccessible(true);
                } catch (Throwable e) {
                    //CLogUtils.e("getDeclaredField spi error  "+e.getMessage());
                }

            }
            if (keyField != null) {
                spiObject = keyField.get(cipher);
                if (spiObject != null) {
                    cipherKeyResut.keyJson = GsonUtils.obj2str(spiObject);
                    if (cipherKeyResut.keyJson != null) {
                        SpiObject spiBean = (SpiObject) GsonUtils.getClass(cipherKeyResut.keyJson, SpiObject.class);
                        if (spiBean != null) {
                            cipherKeyResut.key = spiBean.getKey();
                        }
                    }
                }
            }
            //CLogUtils.e("打印结果toString "+cipherKeyResut.toString());
            return cipherKeyResut;
        } catch (Throwable e) {
            CLogUtils.e("getSpiKey error  " + e.getMessage());
        }
        return cipherKeyResut;
    }

    private static void parseParam(StringBuilder mStringBuilder, XC_MethodHook.MethodHookParam param) {
        if (param.args.length > 0) {
            //解析参数
            mStringBuilder.append(Arrays.toString(param.args)).append("\n");
            mStringBuilder.append("参数Class类型 :");
            for (Object obj : param.args) {
                mStringBuilder.append(obj.getClass().getName()).append(",");
            }
            mStringBuilder.append("\n");
            for (Object obj : param.args) {
                handler(obj, mStringBuilder, param);
            }
        }
        if (param.thisObject != null) {
            //解析this
            mStringBuilder.append("this.Object").append(" ").append(param.thisObject.getClass().getName()).append("\n");
            handler(param.thisObject, mStringBuilder, param);
        }
        //解析Result
        if (param.getResult() != null) {
            mStringBuilder.append("返回结果").append("\n");
            handler(param.getResult(), mStringBuilder, param);
        }
    }

    public static void handler(Object obj,
                               StringBuilder mStringBuilder,
                               XC_MethodHook.MethodHookParam param) {
        if (obj instanceof Integer) {
            mStringBuilder.append("类型参数 int ").append(obj).append("\n");
        } else if (obj instanceof byte[]) {
            byte[] bytes = (byte[]) obj;
            mStringBuilder.append("类型参数 byte[] ").append("\n");
            mStringBuilder.append("十六进制编码   : ").append(bytesToHexString(bytes)).append("\n");
            mStringBuilder.append("U8编码   : ").append(new String(bytes, StandardCharsets.UTF_8)).append("\n");
        } else if (obj instanceof Key) {
            byte[] encodedMethodAndInvoke = getEncodedMethodAndInvoke(obj);
            if (encodedMethodAndInvoke != null) {
                mStringBuilder.append("Key私钥的 UTF-8 编码    ").append(new String(encodedMethodAndInvoke, StandardCharsets.UTF_8)).append("\n");
                mStringBuilder.append("Key私钥的 16进制编码     ").append(bytesToHexString(encodedMethodAndInvoke)).append("\n");
            }
            String algorithm = getAlgorithm(obj);
            mStringBuilder.append("Key加密模式   ").append(algorithm).append("\n");
            mStringBuilder.append("Key toString   ").append(obj.toString()).append("\n");

        } else if (obj instanceof AlgorithmParameters) {
            byte[] getIvFieldAndIvoke = getIvFieldAndIvoke(obj);
            if (getIvFieldAndIvoke != null) {
                mStringBuilder.append("IV 的 UTF-8 编码    ").append(new String(getIvFieldAndIvoke, StandardCharsets.UTF_8)).append("\n");
                mStringBuilder.append("IV 的 16 进制  编码    ").append(bytesToHexString(getIvFieldAndIvoke)).append("\n");
            } else {
                mStringBuilder.append("IV AlgorithmParameters 参数toString  ").append(obj.toString()).append("\n");
            }
        } else if (obj instanceof SecureRandom) {
            mStringBuilder.append("参数类型   ").append(obj.getClass().getName()).append(((SecureRandom) obj).getAlgorithm()).append("\n");
        } else if (obj instanceof ByteBuffer) {
            ByteBuffer arg1 = (ByteBuffer) obj;
            byte[] array1 = arg1.array();
            mStringBuilder.append("参数类型 ByteBuffer  U8编码  ").append(getStr(array1)).append("\n");
            mStringBuilder.append("参数类型 ByteBuffer  16进制编码  ").append(bytesToHexString(array1)).append("\n");
            mStringBuilder.append("参数类型 ByteBuffer  toString  ").append(arg1.toString()).append("\n");

        } else if (obj instanceof String) {
            mStringBuilder.append("参数类型String  U8编码  ").append(obj.toString()).append("\n");
            mStringBuilder.append("参数类型String  16进制编码  ").append(bytesToHexString(obj.toString().getBytes())).append("\n");
        } else if (obj instanceof MessageDigest) {
            mStringBuilder.append("MessageDigest ").append(obj.toString()).append("\n");
        } else if (obj instanceof Mac) {
            mStringBuilder.append("Mac ").append(obj.toString()).append("\n");
        } else if (obj instanceof Cipher) {
            Cipher thisObject = (Cipher) param.thisObject;
            if(thisObject==null){
                return;
            }
            mStringBuilder.append("Cipher->").append(param.method.getName())
                    .append("  ").append(getAlgorithm(thisObject)).append("\n");
            //默认Iv == null
            byte[] iv;
            //尝试获取IV 可能会抛异常
            try {
                iv = thisObject.getIV();
                //将IV打印
                if (iv != null) {
                    mStringBuilder.append("Cipher getIV cipher->").append(param.method.getName())
                            .append("  ").append("U8编码:").append(new String(iv, StandardCharsets.UTF_8)).append("\n");
                    mStringBuilder.append("Cipher getIV cipher->").append(param.method.getName())
                            .append("  ").append("U8编码:  ").append(bytesToHexString(iv)).append("\n");
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
            //打印加密秘钥Key
            CipherKeyResut spiKey = getSpiKey(thisObject);
            if (spiKey != null) {
                mStringBuilder.append("Cipher getKeyJSON cipher->").append(param.method.getName())
                        .append("  ").append("U8编码:").append(spiKey.getKeyJson()).append("\n");
                mStringBuilder.append("Cipher getKeyJSON cipher->").append(param.method.getName())
                        .append("  ").append("U8编码:").append(bytesToHexString(spiKey.getKeyJson().getBytes())).append("\n");

                mStringBuilder.append("Cipher getKey cipher->").append(param.method.getName())
                        .append("  ").append("U8编码:").append(spiKey.getKey()).append("\n");
                mStringBuilder.append("Cipher getKey cipher->").append(param.method.getName())
                        .append("  ").append("U8编码:").append(bytesToHexString(spiKey.getKey().getBytes())).append("\n");
            }
        }

    }

}
