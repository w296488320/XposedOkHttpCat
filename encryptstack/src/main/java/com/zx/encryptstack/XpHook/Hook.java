package com.zx.encryptstack.XpHook;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.zx.encryptstack.utils.CLogUtils;
import com.zx.encryptstack.utils.FileUtils;
import com.zx.encryptstack.utils.GsonUtils;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
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
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Cipher;
import javax.crypto.Mac;
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

    /**
     * 进行 注入的 app 名字
     */
    public static volatile String InvokPackage = null;
    //只当前进程使用
    private final SimpleDateFormat mSimpleDateFormat =
            new SimpleDateFormat("MM-dd  MM分:ss秒", Locale.getDefault());

    private final String Base64EncodeToString = "encodeToString";
    private final String Base64Encode = "encode";
    private final String Base64Decode = "decode";
    private final String Md5Digest = "digest";
    private final String Update = "update";
    private final String DoFinal = "doFinal";
    private final String Init = "init";
    private Context mOtherContext;
    private ClassLoader mLoader;
    //private final String getBytes = "getBytes";


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
    private String bytesToHexString(byte[] bArr) {
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

                        HookEncrypMethod();
                    }
                });

        XposedHelpers.findAndHookMethod(Activity.class, "onCreate",
                Bundle.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        if(param.thisObject instanceof Activity) {
                            Activity activity = (Activity) param.thisObject;
                            initPermissions(activity);
                        }
                    }
                });

    }

    private void initPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.READ_PHONE_STATE"}, 0);
    }


    /**
     * 核心方法
     */
    private void HookEncrypMethod() {


        try {
            //String string = Base64.encodeToString(byte[],int);
            try {
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
                                FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);
                            }
                        }
                );
            } catch (Throwable e) {
                CLogUtils.e("Hook  Base64.encodeToString(byte[],int); error  " + e);
                e.printStackTrace();
            }

            //byte[] encode = Base64.encode();
            try {
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

                                mStringBuilder.append( "byte[] encode = Base64.encode(); 返回结果   U8编码 ").append(new String(Result, StandardCharsets.UTF_8)).append("\n");
                                mStringBuilder.append( "byte[] encode = Base64.encode(); 返回结果   16进制 编码 ").append(bytesToHexString(Result)).append("\n");

                                getStackTraceMessage(mStringBuilder);

                                FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);
                            }
                        }
                );
            } catch (Throwable e) {
                CLogUtils.e("Hook  Base64.encode(); error  " + e);

                e.printStackTrace();
            }

            //byte[] decode = Base64.decode(byte[],int);
            try {
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
                                FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);
                            }
                        }
                );
            } catch (Throwable e) {
                CLogUtils.e("Hook Base64.decode(byte[],int); error " + e);
                e.printStackTrace();
            }

//        byte[] decode = Base64.decode(String,int);
            try {
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
                                FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);
                            }
                        }
                );
            } catch (Throwable e) {
                CLogUtils.e("Hook Base64.decode(String,int); error " + e.getMessage());
                e.printStackTrace();
            }


            //MD5 和 sha-1
            //byte[] bytes = md5.digest(string.getBytes());

            // public byte[] digest(byte[] input)
            try {
                XposedHelpers.findAndHookMethod(MessageDigest.class, Md5Digest,
                        byte[].class,

                        new XC_MethodHook() {

                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                super.afterHookedMethod(param);
                                StringBuilder mStringBuilder = new StringBuilder("MessageDigest->public byte[] digest(byte[] input);");

                                MessageDigest msgDitest = (MessageDigest) param.thisObject;

                                String algorithm = msgDitest.getAlgorithm();
                                mStringBuilder.append("\n");
                                byte[] Result = (byte[]) param.getResult();

                                StringBuilder result = getMd5(Result);

                                mStringBuilder.append("MessageDigest 类型  ").append(algorithm).append("\n").
                                        append(" 返回结果  ").append(result).append("\n").
                                        append("MessageDigest digest  toString 返回结果 ").append(msgDitest.toString()).append("\n").
                                        append("加密之前 参数1  U8编码 ").append(getStr((byte[]) param.args[0])).append("\n").
                                        append("加密之前 参数1  16进制编码 ").append(bytesToHexString((byte[]) param.args[0])).append("\n");

                                getStackTraceMessage(mStringBuilder);
                                FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);
                            }
                        }
                );
            } catch (Throwable e) {
                CLogUtils.e("Hook  md5.digest(string.getBytes()); error " + e.getMessage());
                e.printStackTrace();
            }


            //MD5 和 sha-1
            //byte[] bytes = md5.digest();
            try {
                XposedHelpers.findAndHookMethod(MessageDigest.class, Md5Digest,
                        new XC_MethodHook() {

                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                super.afterHookedMethod(param);
                                StringBuilder mStringBuilder = new StringBuilder("MessageDigest->public byte[] digest();");

                                MessageDigest msgDitest = (MessageDigest) param.thisObject;

                                String algorithm = msgDitest.getAlgorithm();
                                mStringBuilder.append("\n");
                                byte[] Result = (byte[]) param.getResult();
                                StringBuilder result = getMd5(Result);
                                mStringBuilder.append("MessageDigest 类型  ").append(algorithm).append("\n").
                                        append("byte[] bytes = md5.digest(无参)  返回结果  ").append(result).append("\n").
                                        append("byte[] bytes = md5.digest(无参)  返回结果  U8编码  ").append(new String(Result, StandardCharsets.UTF_8)).append("\n").
                                        append("byte[] bytes = md5.digest(无参)  返回结果  16进制编码  ").append(bytesToHexString(Result)).append("\n").

                                        append("MessageDigest   toString 返回结果 ").append(msgDitest.toString()).append("\n");

                                getStackTraceMessage(mStringBuilder);
                                FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);
                            }
                        }
                );
            } catch (Throwable e) {
                CLogUtils.e("Hook  md5.digest(); error " + e);
                e.printStackTrace();
            }


//            MessageDigest msgDitest = MessageDigest.getInstance("SHA-1");
//            msgDitest.update(Bytes[]);

            try {
                XposedHelpers.findAndHookMethod(MessageDigest.class, Update,
                        byte[].class,
                        new XC_MethodHook() {

                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                super.afterHookedMethod(param);
                                StringBuilder mStringBuilder = new StringBuilder("MessageDigest->update(byte[] input) 无返回结果");

                                MessageDigest msgDitest = (MessageDigest) param.thisObject;

                                String algorithm = msgDitest.getAlgorithm();
                                mStringBuilder.append("\n");
                                mStringBuilder.append("MessageDigest  类型").append(algorithm).append("\n").
                                        append("MessageDigest   toString ").append(msgDitest.toString()).append("\n").
                                        append("msgDitest.update(Bytes[])  参数1 U8编码 ").append(getStr((byte[]) param.args[0])).append("\n").
                                        append("msgDitest.update(Bytes[])  参数1 16进制 编码 ").append(bytesToHexString((byte[]) param.args[0])).append("\n");


                                getStackTraceMessage(mStringBuilder);
                                FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);
                            }
                        }
                );

            } catch (Throwable e) {
                CLogUtils.e("MessageDigest->update(Bytes[]) 无返回结果 " + e);
                e.printStackTrace();
            }

//            MessageDigest msgDitest = MessageDigest.getInstance("SHA-1");
//            msgDitest.update(Bytes);
            try {
                XposedHelpers.findAndHookMethod(MessageDigest.class, Update,
                        byte[].class,
                        int.class,
                        int.class,
                        new XC_MethodHook() {

                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                super.afterHookedMethod(param);
                                StringBuilder mStringBuilder = new StringBuilder("MessageDigest->update(byte[] input, int offset, int len)");

                                MessageDigest msgDitest = (MessageDigest) param.thisObject;

                                String algorithm = msgDitest.getAlgorithm();
                                mStringBuilder.append("\n");
                                mStringBuilder.append("MessageDigest  类型").append(algorithm).append("\n").
                                        append("MessageDigest   toString ").append(msgDitest.toString()).append("\n").
                                        append("msgDitest.update(Bytes[])  参数1 U8编码 ").append(getStr((byte[]) param.args[0])).append("\n").
                                        append("msgDitest.update(Bytes[])  参数1 16进制 编码 ").append(bytesToHexString((byte[]) param.args[0])).append("\n").
                                        append("msgDitest.update(Bytes[])  参数2 int (offset)内容 ").append(param.args[1]).append("\n").
                                        append("msgDitest.update(Bytes[])  参数3 int (len)内容 ").append(param.args[2]).append("\n");


                                getStackTraceMessage(mStringBuilder);
                                FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);
                            }
                        }
                );

            } catch (Throwable e) {
                CLogUtils.e("MessageDigest->update(byte[] input, int offset, int len)无返回结果 " + e);
                e.printStackTrace();
            }
            //public final void update(ByteBuffer input)
            try {
                XposedHelpers.findAndHookMethod(MessageDigest.class, Update,
                        ByteBuffer.class,
                        new XC_MethodHook() {

                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                super.afterHookedMethod(param);
                                StringBuilder mStringBuilder = new StringBuilder("MessageDigest->update(ByteBuffer input)");

                                MessageDigest msgDitest = (MessageDigest) param.thisObject;

                                String algorithm = msgDitest.getAlgorithm();
                                mStringBuilder.append("\n");

                                ByteBuffer arg1 = (ByteBuffer) param.args[0];

                                mStringBuilder.append("MessageDigest  类型").append(algorithm).append("\n").
                                        append("MessageDigest   toString ").append(msgDitest.toString()).append("\n").
                                        append("msgDitest.update(ByteBuffer)  参数1 asCharBuffer ").append(arg1.asCharBuffer()).append("\n").
                                        append("msgDitest.update(ByteBuffer)  参数1 array 方法 16进制编码 ").append(bytesToHexString(arg1.array())).append("\n");


                                getStackTraceMessage(mStringBuilder);
                                FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);
                            }
                        }
                );

            } catch (Throwable e) {
                CLogUtils.e("MessageDigest->update(ByteBuffer input)无返回结果 " + e);
                e.printStackTrace();
            }

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
                XposedHelpers.findAndHookMethod(Mac.class, "doFinal",
                        byte[].class,
                        new XC_MethodHook() {

                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                super.afterHookedMethod(param);
                                StringBuilder mStringBuilder = new StringBuilder(DoFinal);

                                mStringBuilder.append("\n");
                                byte[] Result = (byte[]) param.getResult();
                                if (param.thisObject instanceof Mac) {
                                    Mac thisObject = (Mac) param.thisObject;
                                    mStringBuilder.append("Mac->doFinal加密类型 ").append(thisObject.getAlgorithm()).append("\n");
                                }
                                mStringBuilder.append("byte[] Mac.doFinal(Byte[]) 参数1 U8编码  ").append(getStr((byte[]) param.args[0])).append("\n");
                                mStringBuilder.append("byte[] Mac.doFinal(Byte[]) 参数1 16进制编码  ").append(bytesToHexString((byte[]) param.args[0])).append("\n");
                                mStringBuilder.append("byte[] Mac.doFinal(Byte[])  加密  16进制   返回结果  ").append(bytesToHexString(Result)).append("\n");
                                mStringBuilder.append("byte[] Mac.doFinal(Byte[])  加密  U8编码    返回结果  ").append(new String(Result, StandardCharsets.UTF_8)).append("\n");

                                getStackTraceMessage(mStringBuilder);
                                FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);

                            }
                        }
                );
            } catch (Throwable e) {
                CLogUtils.e("Hook HmacSHA1算法 cipher.doFinal(Byte[]); error " + e);
                e.printStackTrace();
            }


            //SecretKeySpec构造方法
            //这个类保存了一些重要的Key 我们在他初始化完毕以后进行dump
            //构造体1:  public SecretKeySpec(byte[] key, String algorithm)

            try {
                XposedHelpers.findAndHookConstructor(
                        SecretKeySpec.class,
                        byte[].class,
                        String.class,
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                super.afterHookedMethod(param);
                                StringBuilder mStringBuilder = new StringBuilder("SecretKeySpec(byte[] key, String algorithm)");

                                mStringBuilder.append("\n");
                                mStringBuilder.append("SecretKeySpec(byte[] key, String algorithm) 参数1 U8编码  ").append(getStr((byte[]) param.args[0])).append("\n");
                                mStringBuilder.append("SecretKeySpec(byte[] key, String algorithm) 参数1 16进制编码  ").append(bytesToHexString((byte[]) param.args[0])).append("\n");
                                mStringBuilder.append("SecretKeySpec(byte[] key, String algorithm) 参数2  ").append((String) param.args[1]).append("\n");

                                getStackTraceMessage(mStringBuilder);
                                FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);
                            }
                        }
                );
            } catch (Throwable e) {
                CLogUtils.e("Hook public SecretKeySpec(byte[] key, String algorithm) error " + e.getMessage());
                e.printStackTrace();
            }
            //构造体1:  SecretKeySpec(byte[] key, int offset, int len, String algorithm)
            try {
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
                                StringBuilder mStringBuilder = new StringBuilder("SecretKeySpec(byte[] key, int offset, int len, String algorithm)");

                                mStringBuilder.append("\n");
                                mStringBuilder.append("SecretKeySpec(byte[] key, int offset, int len, String algorithm) 参数1 U8编码  ").append(getStr((byte[]) param.args[0])).append("\n");
                                mStringBuilder.append("SecretKeySpec(byte[] key, int offset, int len, String algorithm) 参数1 16进制编码  ").append(bytesToHexString((byte[]) param.args[0])).append("\n");
                                mStringBuilder.append("SecretKeySpec(byte[] key, int offset, int len, String algorithm) 参数2  ").append((int) param.args[1]).append("\n");
                                mStringBuilder.append("SecretKeySpec(byte[] key, int offset, int len, String algorithm) 参数3  ").append((int) param.args[2]).append("\n");
                                mStringBuilder.append("SecretKeySpec(byte[] key, int offset, int len, String algorithm) 参数4  ").append(param.args[3]).append("\n");

                                getStackTraceMessage(mStringBuilder);
                                FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);
                            }
                        }
                );
            } catch (Throwable e) {
                CLogUtils.e("Hook public SecretKeySpec(byte[] key, String algorithm) error " + e.getMessage());
                e.printStackTrace();
            }

            // 这个方法AES或者 RSA都有可能用
            // 核心方法
            //byte[] cipher.doFinal(Byte[]);
            try {
                XposedBridge.hookAllMethods(Cipher.class, DoFinal,
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                super.afterHookedMethod(param);
                                StringBuilder mStringBuilder = new StringBuilder(DoFinal);
                                mStringBuilder.append("\n");

                                if (param.thisObject instanceof Cipher) {
                                    Cipher thisObject = (Cipher) param.thisObject;
                                    mStringBuilder.append("Cipher->doFinal(Byte[]) cipher 加密类型 ").append(thisObject.getAlgorithm()).append("\n");
                                    //默认Iv == null
                                    byte[] iv;
                                    //尝试获取IV 可能会抛异常
                                    try {
                                        iv = thisObject.getIV();
                                        //将IV打印
                                        if (iv != null) {
                                            mStringBuilder.append("byte[] cipher.doFinal(Byte[]) IV内容打印  U8编码  ").append(new String(iv, StandardCharsets.UTF_8)).append("\n");
                                            mStringBuilder.append("byte[] cipher.doFinal(Byte[]) IV内容打印  16进制编码  ").append(bytesToHexString(iv)).append("\n");
                                        }
                                    } catch (Throwable e) {
                                        e.printStackTrace();
                                    }
                                    //打印加密秘钥Key
                                    CipherKeyResut spiKey = getSpiKey(thisObject);
                                    if(spiKey!=null){
                                        mStringBuilder.append("byte[] cipher.doFinal(Byte[]) KeyJson内容打印  U8编码  ").append(spiKey.getKeyJson()).append("\n");
                                        mStringBuilder.append("byte[] cipher.doFinal(Byte[]) KeyJson内容打印  16进制  ").append(bytesToHexString(spiKey.getKeyJson().getBytes())).append("\n");

                                        mStringBuilder.append("byte[] cipher.doFinal(Byte[]) Key内容打印  U8编码  ").append(spiKey.getKey()).append("\n");
                                        mStringBuilder.append("byte[] cipher.doFinal(Byte[]) Key内容打印  16进制  ").append(bytesToHexString(spiKey.getKey().getBytes())).append("\n");
                                    }
                                }

                                //这块有四种构造方法,分别进行处理
                                if (param.args.length > 0) {

                                    if (param.args.length == 1) {
                                        if (param.args[0] instanceof byte[]) {
                                            mStringBuilder.append("byte[] cipher.doFinal(Byte[]) 参数1 U8编码  ").append(getStr((byte[]) param.args[0])).append("\n");
                                            mStringBuilder.append("byte[] cipher.doFinal(Byte[]) 参数1 16进制编码  ").append(bytesToHexString((byte[]) param.args[0])).append("\n");
                                        }
                                    }
                                    if (param.args.length == 2) {
                                        if (param.args[0] instanceof byte[]) {
                                            mStringBuilder.append("cipher.doFinal(byte[] output, int outputOffset) 参数1 U8编码  ").append(getStr((byte[]) param.args[0])).append("\n");
                                            mStringBuilder.append("cipher.doFinal(byte[] output, int outputOffset) 参数1 16进制编码  ").append(bytesToHexString((byte[]) param.args[0])).append("\n");
                                        }
                                        mStringBuilder.append("cipher.doFinal(byte[] output, int outputOffset) 参数2  ").append(param.args[1]).append("\n");
                                    }
                                    if(param.args.length ==3){
                                        if (param.args[0] instanceof byte[]) {
                                            mStringBuilder.append("doFinal(byte[] input, int inputOffset, int inputLen) 参数1 U8编码  ").append(getStr((byte[]) param.args[0])).append("\n");
                                            mStringBuilder.append("doFinal(byte[] input, int inputOffset, int inputLen) 参数1 16进制编码  ").append(bytesToHexString((byte[]) param.args[0])).append("\n");
                                        }
                                        mStringBuilder.append("doFinal(byte[] input, int inputOffset, int inputLen) 参数2  ").append(param.args[1]).append("\n");
                                        mStringBuilder.append("doFinal(byte[] input, int inputOffset, int inputLen) 参数3  ").append(param.args[2]).append("\n");
                                    }

                                    if(param.args.length ==4){
                                        if (param.args[0] instanceof byte[]) {
                                            mStringBuilder.append("doFinal(byte[] input, int inputOffset, int inputLen, byte[] output) 参数1 U8编码  ").append(getStr((byte[]) param.args[0])).append("\n");
                                            mStringBuilder.append("doFinal(byte[] input, int inputOffset, int inputLen, byte[] output) 参数1 16进制编码  ").append(bytesToHexString((byte[]) param.args[0])).append("\n");
                                        }
                                        mStringBuilder.append("doFinal(byte[] input, int inputOffset, int inputLen, byte[] output) 参数2  ").append(param.args[1]).append("\n");
                                        mStringBuilder.append("doFinal(byte[] input, int inputOffset, int inputLen, byte[] output) 参数3  ").append(param.args[2]).append("\n");

                                        mStringBuilder.append("doFinal(byte[] input, int inputOffset, int inputLen, byte[] output) 参数4 U8编码  ").append(getStr((byte[]) param.args[3])).append("\n");
                                        mStringBuilder.append("doFinal(byte[] input, int inputOffset, int inputLen, byte[] output) 参数4 16进制编码  ").append(bytesToHexString((byte[]) param.args[3])).append("\n");
                                    }
                                    if(param.args.length ==5){
                                        if (param.args[0] instanceof byte[]) {
                                            mStringBuilder.append("doFinal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) 参数1 U8编码  ").append(getStr((byte[]) param.args[0])).append("\n");
                                            mStringBuilder.append("doFinal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) 参数1 16进制编码  ").append(bytesToHexString((byte[]) param.args[0])).append("\n");
                                        }
                                        mStringBuilder.append("doFinal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) 参数2  ").append(param.args[1]).append("\n");
                                        mStringBuilder.append("doFinal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) 参数3  ").append(param.args[2]).append("\n");

                                        mStringBuilder.append("doFinal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) 参数4 U8编码  ").append(getStr((byte[]) param.args[3])).append("\n");
                                        mStringBuilder.append("doFinal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) 参数4 16进制编码  ").append(bytesToHexString((byte[]) param.args[3])).append("\n");


                                        mStringBuilder.append("doFinal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) 参数5   ").append(param.args[4]).append("\n");
                                    }
                                    if(param.args[0] instanceof ByteBuffer && param.args[1] instanceof ByteBuffer){
                                        ByteBuffer arg1 = (ByteBuffer) param.args[0];
                                        ByteBuffer arg2 = (ByteBuffer) param.args[1];
                                        byte[] array1 = arg1.array();
                                        byte[] array2 = arg2.array();

                                        mStringBuilder.append("doFinal(ByteBuffer input, ByteBuffer output) 参数1 array函数  U8编码").append(getStr(array1)).append("\n");
                                        mStringBuilder.append("doFinal(ByteBuffer input, ByteBuffer output) 参数1 array函数  16进制编码").append(bytesToHexString(array1)).append("\n");

                                        mStringBuilder.append("doFinal(ByteBuffer input, ByteBuffer output) 参数2   array函数").append(getStr(array2)).append("\n");
                                        mStringBuilder.append("doFinal(ByteBuffer input, ByteBuffer output) 参数2   array函数").append(bytesToHexString(array2)).append("\n");

                                        mStringBuilder.append("doFinal(ByteBuffer input, ByteBuffer output) 参数1 toString").append(arg1.toString()).append("\n");
                                        mStringBuilder.append("doFinal(ByteBuffer input, ByteBuffer output) 参数2 toString").append(arg2.toString()).append("\n");

                                    }
                                }

                                byte[] Result = (byte[]) param.getResult();
                                if (Result != null) {
                                    mStringBuilder.append("byte[] cipher.doFinal  返回结果  ").append(bytesToHexString(Result)).append("\n");
                                    mStringBuilder.append("byte[] cipher.doFinal  返回结果  ").append(new String(Result, StandardCharsets.UTF_8)).append("\n");
                                }
                                getStackTraceMessage(mStringBuilder);
                                FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);

                            }
                        }
                );
            } catch (Throwable e) {
                CLogUtils.e("Hook cipher.doFinal(Byte[]); error " + e);
                e.printStackTrace();
            }


            //cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            try {
                XposedHelpers.findAndHookMethod(Cipher.class, Init,
                        int.class,
                        Key.class,
                        new XC_MethodHook() {

                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                super.beforeHookedMethod(param);
                                StringBuilder stringBuffer = new StringBuilder("两个参数 cipher.init(Cipher.ENCRYPT_MODE, skeySpec) 不需要IV").append("\n");

                                stringBuffer.append("两个参数 cipher.init(Cipher.ENCRYPT_MODE, skeySpec) 不需要IV").append("\n");


                                //这个方法 主要是 拿到 私钥
                                Object Key = param.args[1];

                                String algorithm = getAlgorithm(Key);

                                stringBuffer.append(algorithm == null ? "未找到  加密方式 " : ("       " + algorithm)).append("   加密初始化(ECB 模式)    方法名字   " + Init + "\n").
                                        append(mSimpleDateFormat.format(new Date())).append("\n").
                                        append(param.thisObject != null ? param.thisObject.getClass().getName() : "").append(".").append(Init).append("\n");


                                byte[] encodedMethodAndInvoke = getEncodedMethodAndInvoke(Key);
                                if (encodedMethodAndInvoke != null) {
                                    stringBuffer.append("私钥的 UTF-8 编码    ").append(new String(encodedMethodAndInvoke, StandardCharsets.UTF_8)).append("\n");
                                    stringBuffer.append("私钥的 16 进制  编码    ").append(bytesToHexString(encodedMethodAndInvoke)).append("\n");
                                }
                                getStackTraceMessage(stringBuffer);

                                FileUtils.SaveString(mOtherContext, stringBuffer.toString(), InvokPackage);
                            }

                        }
                );
            } catch (Throwable e) {
                CLogUtils.e("Hook cipher.init(Cipher.ENCRYPT_MODE, skeySpec); error " + e);
                e.printStackTrace();
            }


            //AES加密算法 初始化的时候 两个 参数 是ECB  模式 不需要 IV
            //三个 参数 的 需要 IV
            //cipher.init(Cipher.ENCRYPT_MODE, skeySpec,IvParameterSpec);
            try {
                XposedHelpers.findAndHookMethod(Cipher.class, Init,
                        int.class,
                        Key.class,
                        AlgorithmParameters.class,
                        new XC_MethodHook() {

                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                super.beforeHookedMethod(param);
                                StringBuilder stringBuffer = new StringBuilder("三个参数 需要 IV  Cipher.init(Cipher.ENCRYPT_MODE, skeySpec,IvParameterSpec)").append("\n");
                                stringBuffer.append("三个参数 需要 IV  cipher.init(Cipher.ENCRYPT_MODE, skeySpec,IvParameterSpec) ").append("\n");

                                //CLogUtils.e("参数 类型 int   key   AlgorithmParameters");
                                //这个方法 主要是 拿到 私钥
                                Object Key = param.args[1];
                                String algorithm = getAlgorithm(Key);
                                Object IV = param.args[2];

                                //getAlgorithm
                                stringBuffer.append(algorithm == null ? "未找到  加密方式 " : ("       " + algorithm))
                                        .append("  加密初始化(CBC 模式  三个 参数   需要  IV)    方法名字   " + Init + "\n").
                                        append(mSimpleDateFormat.format(new Date())).append("\n").
                                        append(param.thisObject != null ? param.thisObject.getClass().getName() : "").append(".").append(Init).append("\n");


                                byte[] encodedMethodAndInvoke = getEncodedMethodAndInvoke(Key);
                                if (encodedMethodAndInvoke != null) {
                                    stringBuffer.append("私钥的 UTF-8 编码    ").append(new String(encodedMethodAndInvoke, StandardCharsets.UTF_8)).append("\n");
                                    stringBuffer.append("私钥的 16 进制  编码    ").append(bytesToHexString(encodedMethodAndInvoke)).append("\n");
                                }
                                byte[] getIvFieldAndIvoke = getIvFieldAndIvoke(IV);
                                if (getIvFieldAndIvoke != null) {
                                    stringBuffer.append("IV 的 UTF-8 编码    ").append(new String(getIvFieldAndIvoke, StandardCharsets.UTF_8)).append("\n");
                                    stringBuffer.append("IV 的 16 进制  编码    ").append(bytesToHexString(getIvFieldAndIvoke)).append("\n");
                                }

                                getStackTraceMessage(stringBuffer);

                                FileUtils.SaveString(mOtherContext, stringBuffer.toString(), InvokPackage);

                            }

                        }
                );
            } catch (Throwable e) {
                CLogUtils.e("Hook cipher.init(Cipher.ENCRYPT_MODE, skeySpec,IvParameterSpec); error " + e.getMessage());
                e.printStackTrace();
            }


            //cipher.init(Cipher.ENCRYPT_MODE, skeySpec,IvParameterSpec);
            try {
                XposedHelpers.findAndHookMethod(Cipher.class, Init,
                        int.class,
                        Key.class,
                        AlgorithmParameters.class,
                        SecureRandom.class,
                        new XC_MethodHook() {

                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                super.beforeHookedMethod(param);
                                StringBuilder stringBuffer = new StringBuilder().append("\n");
                                stringBuffer.append("Cipher.init(Cipher.ENCRYPT_MODE, skeySpec,IvParameterSpec) ").append("\n");

                                CLogUtils.e("参数 类型 int   key   AlgorithmParameters   SecureRandom");

                                //这个方法 主要是 拿到 私钥
                                Object Key = param.args[1];
                                String algorithm = getAlgorithm(Key);
                                Object IV = param.args[2];

                                //getAlgorithm
                                stringBuffer.append(algorithm == null ? "未找到  加密方式 " : ("       " + algorithm)).append("            加密初始化(CBC 模式  三个 参数   需要  IV)    方法名字   " + Init + "\n").
                                        append(mSimpleDateFormat.format(new Date())).append("\n").
                                        append(param.thisObject != null ? param.thisObject.getClass().getName() : "").append(".").append(Init).append("\n");


                                byte[] encodedMethodAndInvoke = getEncodedMethodAndInvoke(Key);
                                if (encodedMethodAndInvoke != null) {
                                    stringBuffer.append("私钥的 UTF-8 编码    ").append(new String(encodedMethodAndInvoke, StandardCharsets.UTF_8)).append("\n");
                                    stringBuffer.append("私钥的 16 进制  编码    ").append(bytesToHexString(encodedMethodAndInvoke)).append("\n");
                                }
                                byte[] getIvFieldAndIvoke = getIvFieldAndIvoke(IV);
                                if (getIvFieldAndIvoke != null) {
                                    stringBuffer.append("IV 的 UTF-8 编码    ").append(new String(getIvFieldAndIvoke, StandardCharsets.UTF_8)).append("\n");
                                    stringBuffer.append("IV 的 16 进制  编码    ").append(bytesToHexString(getIvFieldAndIvoke)).append("\n");
                                }

                                getStackTraceMessage(stringBuffer);

                                FileUtils.SaveString(mOtherContext, stringBuffer.toString(), InvokPackage);

                            }

                        }
                );
            } catch (Throwable e) {
                CLogUtils.e("Hook cipher.init(Cipher.ENCRYPT_MODE, skeySpec,IvParameterSpec); error " + e);
                e.printStackTrace();
            }


            try {
                XposedHelpers.findAndHookMethod(Cipher.class, Init,
                        int.class,
                        Key.class,
                        AlgorithmParameterSpec.class,
                        new XC_MethodHook() {

                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                super.beforeHookedMethod(param);
                                StringBuilder stringBuffer = new StringBuilder("").append("\n");
                                stringBuffer.append("cipher.init(Cipher.ENCRYPT_MODE, skeySpec,AlgorithmParameterSpec ) ").append("\n");

                                //CLogUtils.e("参数 类型 int   key   AlgorithmParameterSpec  ");
                                //这个方法 主要是 拿到 私钥
                                Object Key = param.args[1];
                                String algorithm = getAlgorithm(Key);
                                Object IV = param.args[2];

                                //getAlgorithm
                                stringBuffer.append(algorithm == null ? "未找到  加密方式 " : ("       " + algorithm)).append("        加密初始化(CBC 模式  三个 参数   需要  IV)    方法名字   " + Init + "\n").append(mSimpleDateFormat.format(new Date())).append("\n").
                                        append(param.thisObject != null ? param.thisObject.getClass().getName() : "").append(".").append(Init).append("\n");


                                byte[] encodedMethodAndInvoke = getEncodedMethodAndInvoke(Key);
                                if (encodedMethodAndInvoke != null) {
                                    stringBuffer.append("私钥的 UTF-8 编码    ").append(new String(encodedMethodAndInvoke, StandardCharsets.UTF_8)).append("\n");
                                    stringBuffer.append("私钥的 16 进制  编码    ").append(bytesToHexString(encodedMethodAndInvoke)).append("\n");
                                }
                                byte[] getIvFieldAndIvoke = getIvFieldAndIvoke(IV);
                                if (getIvFieldAndIvoke != null) {
                                    stringBuffer.append("IV 的 UTF-8 编码    ").append(new String(getIvFieldAndIvoke, StandardCharsets.UTF_8)).append("\n");
                                    stringBuffer.append("IV 的 16 进制  编码    ").append(bytesToHexString(getIvFieldAndIvoke)).append("\n");
                                }
                                getStackTraceMessage(stringBuffer);

                                FileUtils.SaveString(mOtherContext, stringBuffer.toString(), InvokPackage);

                            }

                        }
                );
            } catch (Throwable e) {
                CLogUtils.e("Hook cipher.init(Cipher.ENCRYPT_MODE, skeySpec,AlgorithmParameterSpec ) error " + e.getMessage());
                e.printStackTrace();
            }

            try {
                XposedHelpers.findAndHookMethod(Cipher.class, Init,
                        int.class,
                        Key.class,
                        AlgorithmParameterSpec.class,
                        SecureRandom.class,
                        new XC_MethodHook() {

                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                super.beforeHookedMethod(param);
                                StringBuilder stringBuffer = new StringBuilder().append("\n");
                                stringBuffer.append("cipher.init(Cipher.ENCRYPT_MODE, skeySpec,AlgorithmParameterSpec,SecureRandom) ").append("\n");

                                //CLogUtils.e("参数 类型 int   key   AlgorithmParameterSpec SecureRandom ");
                                //这个方法 主要是 拿到 私钥
                                Object Key = param.args[1];
                                String algorithm = getAlgorithm(Key);
                                Object IV = param.args[2];

                                //getAlgorithm
                                stringBuffer.append(algorithm == null ? "未找到  加密方式 " : ("       " + algorithm)).append("  加密初始化(CBC 模式  三个 参数   需要  IV)    方法名字   " + Init + "\n").append(mSimpleDateFormat.format(new Date())).append("\n").
                                        append(param.thisObject != null ? param.thisObject.getClass().getName() : "").append(".").append(Init).append("\n");


                                byte[] encodedMethodAndInvoke = getEncodedMethodAndInvoke(Key);
                                if (encodedMethodAndInvoke != null) {
                                    stringBuffer.append("私钥的 UTF-8 编码    ").append(new String(encodedMethodAndInvoke, StandardCharsets.UTF_8)).append("\n");
                                    stringBuffer.append("私钥的 16 进制  编码    ").append(bytesToHexString(encodedMethodAndInvoke)).append("\n");
                                }
                                byte[] getIvFieldAndIvoke = getIvFieldAndIvoke(IV);
                                if (getIvFieldAndIvoke != null) {
                                    stringBuffer.append("IV 的 UTF-8 编码    ").append(new String(getIvFieldAndIvoke, StandardCharsets.UTF_8)).append("\n");
                                    stringBuffer.append("IV 的 16 进制  编码    ").append(bytesToHexString(getIvFieldAndIvoke)).append("\n");
                                }
                                getStackTraceMessage(stringBuffer);

                                FileUtils.SaveString(mOtherContext, stringBuffer.toString(), InvokPackage);

                            }

                        }
                );
            } catch (Throwable e) {
                CLogUtils.e("Hook cipher.init(Cipher.ENCRYPT_MODE, skeySpec,AlgorithmParameterSpec,SecureRandom) error " + e);
                e.printStackTrace();
            }


            try {
                XposedHelpers.findAndHookMethod(Cipher.class, Init,
                        int.class,
                        Key.class,
                        SecureRandom.class,
                        new XC_MethodHook() {

                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                super.beforeHookedMethod(param);
                                StringBuilder stringBuffer = new StringBuilder("").append("\n");
                                stringBuffer.append("cipher.init(Cipher.ENCRYPT_MODE, skeySpec,SecureRandom) ").append("\n");

                                //CLogUtils.e("参数 类型 int   key    SecureRandom ");
                                //这个方法 主要是 拿到 私钥
                                Object Key = param.args[1];
                                String algorithm = getAlgorithm(Key);
                                Object IV = param.args[2];

                                //getAlgorithm
                                stringBuffer.append(algorithm == null ? "未找到  加密方式 " : ("       " + algorithm)).append("  加密初始化(CBC 模式  三个 参数   需要  IV)    方法名字   " + Init + "\n").append(mSimpleDateFormat.format(new Date())).append("\n").
                                        append(param.thisObject != null ? param.thisObject.getClass().getName() : "").append(".").append(Init).append("\n");


                                byte[] encodedMethodAndInvoke = getEncodedMethodAndInvoke(Key);
                                if (encodedMethodAndInvoke != null) {
                                    stringBuffer.append("私钥的 UTF-8 编码    ").append(new String(encodedMethodAndInvoke, StandardCharsets.UTF_8)).append("\n");
                                    stringBuffer.append("私钥的 16 进制  编码    ").append(bytesToHexString(encodedMethodAndInvoke)).append("\n");
                                }
                                byte[] getIvFieldAndIvoke = getIvFieldAndIvoke(IV);
                                if (getIvFieldAndIvoke != null) {
                                    stringBuffer.append("IV 的 UTF-8 编码    ").append(new String(getIvFieldAndIvoke, StandardCharsets.UTF_8)).append("\n");
                                    stringBuffer.append("IV 的 16 进制  编码    ").append(bytesToHexString(getIvFieldAndIvoke)).append("\n");
                                }
                                getStackTraceMessage(stringBuffer);

                                FileUtils.SaveString(mOtherContext, stringBuffer.toString(), InvokPackage);

                            }

                        }
                );
            } catch (Throwable e) {
                CLogUtils.e("Hook cipher.init(Cipher.ENCRYPT_MODE, skeySpec,SecureRandom) error " + e);
                e.printStackTrace();
            }


            //new JsonObject();
            try {
                try {
                    XposedHelpers.findAndHookConstructor(JSONObject.class, String.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
                            StringBuilder mStringBuilder = new StringBuilder("Json字符串 JsonObject 构造方法 " + "\n");
                            if (param.thisObject != null) {
                                String json = ((JSONObject) param.thisObject).toString();
                                mStringBuilder.append("Json 内容 :  ").append(json).append("\n");
                                mStringBuilder.append("Json 十六进制编码   : ").append(bytesToHexString(json.getBytes())).append("\n\n");

                                getStackTraceMessage(mStringBuilder);

                                FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);
                            }
                        }
                    });
                } catch (Throwable e) {
                    CLogUtils.e("Hook JSONObject 构造error  " + e.getMessage());
                    e.printStackTrace();
                }

//                try {
//                    XposedHelpers.findAndHookMethod(JSONObject.class,
//                            "toString",
//                            new XC_MethodHook() {
//                        @Override
//                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                            super.afterHookedMethod(param);
//                            StringBuilder mStringBuilder = new StringBuilder("Json字符串 JsonObject.toString " + "\n");
//                            if (param.thisObject != null) {
//                                String json = ((JSONObject) param.thisObject).;
//                                mStringBuilder.append("Json 内容 :  ").append(json).append("\n");
//                                mStringBuilder.append("Json 十六进制编码   :  ").append(bytesToHexString(json.getBytes())).append("\n\n");
//
//                                getStackTraceMessage(mStringBuilder);
//
//                                FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);
//                            }
//                        }
//                    });
//                } catch (Throwable e) {
//                    CLogUtils.e("Hook JSONObject toString  error  "+e.getMessage());
//                    e.printStackTrace();
//                }


            } catch (Throwable e) {
                CLogUtils.e("Hook JSONObject error " + e);
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
                                    FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);
                                }
                            }
                    );
                } catch (Throwable e) {
                    CLogUtils.e("Hook Gson->fromJson  error " + e.getMessage());
                    e.printStackTrace();
                }

                try {
                    XposedHelpers.findAndHookMethod(
                            Gson, "toJson", Object.class, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    super.afterHookedMethod(param);
                                    StringBuilder mStringBuilder = new StringBuilder("Json字符串 Gson->toJson(Object) " + "\n");
                                    mStringBuilder.append((String) param.getResult()).append("\n");
                                    mStringBuilder.append("参数1的Class名字  ").append((param.args[0]).getClass().getName()).append("\n");
                                    getStackTraceMessage(mStringBuilder);
                                    FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);
                                }
                            }
                    );
                } catch (Throwable e) {
                    CLogUtils.e("Hook Gson->toJson  error " + e.getMessage());
                    e.printStackTrace();
                }

            }

//            XposedHelpers.findAndHookMethod(View.class, "onTouchEvent", MotionEvent.class, new XC_MethodHook() {
//                @Override
//                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                    super.afterHookedMethod(param);
//                    CLogUtils.e(param.args[0].toString());
//                }
//            });

            Class<?> JSON = null;
            try {
                JSON = XposedHelpers.findClass("com.alibaba.fastjson.JSON", mLoader);
            } catch (Throwable e) {
                e.printStackTrace();
            }

            if (JSON != null) {
                //fastJson
                //public static final Object parse(String text); // 把JSON文本parse为JSONObject或者JSONArray
                try {
                    XposedHelpers.findAndHookMethod(JSON, "parse", String.class, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    super.afterHookedMethod(param);
                                    StringBuilder mStringBuilder = new StringBuilder("Json字符串 FastJson  JSON->parse(String) " + "\n");
                                    mStringBuilder.append("参数1 ").append((String) param.args[0]).append("\n");
                                    mStringBuilder.append("返回内存类名 ").append(param.getResult().getClass().getName()).append("\n");

                                    getStackTraceMessage(mStringBuilder);
                                    FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);

                                }
                            }
                    );
                } catch (Throwable e) {
                    CLogUtils.e("Hook fastjson.JSON->parse error  " + e.getMessage());
                    e.printStackTrace();
                }
                //public static final JSONObject parseObject(String text)； // 把JSON文本parse成JSONObject
                try {
                    XposedHelpers.findAndHookMethod(JSON, "parseObject", String.class, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    super.afterHookedMethod(param);
                                    StringBuilder mStringBuilder = new StringBuilder("Json字符串 FastJson  JSON->parseObject(String) " + "\n");
                                    mStringBuilder.append("参数1 ").append((String) param.args[0]).append("\n");
                                    mStringBuilder.append("返回内容 ").append(param.getResult().toString()).append("\n");

                                    getStackTraceMessage(mStringBuilder);
                                    FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);

                                }
                            }
                    );
                } catch (Throwable e) {
                    CLogUtils.e("Hook fastjson.JSON->parseObject error  " + e.getMessage());

                    e.printStackTrace();
                }
                //public static final String toJSONString(Object object); // 将JavaBean序列化为JSON文本
                try {
                    XposedHelpers.findAndHookMethod(JSON, "toJSONString", Object.class, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    super.afterHookedMethod(param);
                                    StringBuilder mStringBuilder = new StringBuilder("Json字符串 FastJson  JSON->toJSONString(Object) " + "\n");
                                    mStringBuilder.append("返回内容 ").append(param.getResult().toString()).append("\n");
                                    mStringBuilder.append("参数1名字  ").append(param.args[0].getClass().getName()).append("\n");

                                    getStackTraceMessage(mStringBuilder);
                                    FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);

                                }
                            }
                    );
                } catch (Throwable e) {
                    CLogUtils.e("Hook fastjson.JSON->toJSONString error  " + e.getMessage());

                    e.printStackTrace();
                }
            }
            //JockJson
            Class<?> Jackson = null;
            try {
                Jackson = XposedHelpers.findClass("com.fasterxml.jackson.databind.ObjectMapper", mLoader);
            } catch (Throwable e) {
                Jackson = null;
                e.printStackTrace();
            }
            if (Jackson != null) {
                try {
                    XposedHelpers.findAndHookMethod(Jackson, "writeValueAsString", Object.class, new XC_MethodHook() {


                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
                            StringBuilder mStringBuilder = new StringBuilder("Json字符串 Jackson  ObjectMapper->writeValueAsString(Object) " + "\n");
                            mStringBuilder.append("返回内容 ").append(param.getResult().toString()).append("\n");
                            mStringBuilder.append("参数1名字  ").append(param.args[0].getClass().getName()).append("\n");

                            getStackTraceMessage(mStringBuilder);
                            FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);
                        }
                    });
                } catch (Throwable e) {
                    CLogUtils.e("Hook Jackson->writeValueAsString error  " + e.getMessage());
                    e.printStackTrace();
                }

                try {
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
                                    FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);
                                }
                            });
                } catch (Throwable e) {
                    CLogUtils.e("Hook Jackson->readValue error  " + e.getMessage());
                    e.printStackTrace();
                }
            }

            //Gzip压缩相关,很多网络请求是Gzip压缩的在传输的过程中存在数据加密的情况
            //GZIPOutputStream.class
            try {
                //GZIPOutputStream->write 将压缩字节流写进去时候进行拦截
                XposedHelpers.findAndHookMethod(GZIPOutputStream.class,
                        "write",
                        byte[].class,
                        int.class,
                        int.class,
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                super.beforeHookedMethod(param);

                                StringBuilder mStringBuilder = new StringBuilder("GZIPOutputStream->write(byte[],int,int)").append("\n");
                                mStringBuilder.append("参数1 byte[] U8编码内容 ").append(getStr(param.args[0])).append("\n");
                                mStringBuilder.append("参数1类名 16进制编码 ").append(bytesToHexString((byte[]) param.args[0])).append("\n");
                                getStackTraceMessage(mStringBuilder);
                                FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);

                            }
                        });


            } catch (Throwable e) {
                CLogUtils.e("hook GZIPOutputStream->write  error " + e.getMessage());
                e.printStackTrace();
            }


            //ByteArrayOutputStream 输出流的Hook
            try {
                //GZIPOutputStream->write 将压缩字节流写进去时候进行拦截
                XposedHelpers.findAndHookMethod(ByteArrayOutputStream.class,
                        "toString",
                        String.class,
                        new XC_MethodHook() {

                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                super.afterHookedMethod(param);
                                StringBuilder mStringBuilder = new StringBuilder("ByteArrayOutputStream->toString(String)").append("\n");
                                mStringBuilder.append("返回结果  ").append((String) param.getResult()).append("\n");
                                getStackTraceMessage(mStringBuilder);
                                FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);
                            }
                        });

            } catch (Throwable e) {
                CLogUtils.e("hook ByteArrayOutputStream->toString " + e.getMessage());
                e.printStackTrace();
            }


            //byte[] bytes = "".getBytes();
//        XposedHelpers.findAndHookMethod(String.class, getBytes,
//                Charset.class,
//                new XC_MethodHook() {
//                    @Override
//                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                        super.afterHookedMethod(param);
//                        Charset charset = (Charset) param.args[0];
//                        byte[] result = (byte[]) param.getResult();
//                        String mStringBuilder = getBytes + "    方法名字   " + getBytes + "   \n" +
//                                mSimpleDateFormat.format(new Date()) + "\n" +
//                                "Charset  解码类型  "+charset.toString()+
//                                "原 String 内容     " + param.thisObject.toString() +"\n" +
//                                "返回结果   16进制 编码 " + bytesToHexString(result) + "\n"+
//                                "返回结果   UTF-8 编码 " + new String(result, StandardCharsets.UTF_8) + "\n"+
//                                "返回结果   Base64  编码 " + Base64.encodeToString(result,Base64.DEFAULT) + "\n"+
//                                "返回结果   MD5  编码 " + getMd5(result) + "\n";
//
//                        FileUtils.SaveString(mOtherContext, mStringBuilder, InvokPackage);
//                    }
//                }
//        );
            CLogUtils.e("Encryptstack Hook 执行完毕 ");
        } catch (Throwable throwable) {
            CLogUtils.e("Encryptstack 发现异常 " + throwable.toString());
        }

    }



    @NonNull
    private String getStr(byte[] arg) {
        if (arg == null||arg.length==0) {
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

    private void getStackTraceMessage(StringBuilder mStringBuilder) {
        try {
            mStringBuilder.append("\n");
            throw new Exception("getStackTraceElementException");
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = e.getStackTrace();
            mStringBuilder.append(" --------------------------   " + "\n");
            for (StackTraceElement stackTraceElement : stackTrace) {
                mStringBuilder.append("栈信息      ").append(stackTraceElement.getClassName()).append(".").append(stackTraceElement.getMethodName()).append("\n");
            }
            mStringBuilder.append(" --------------------------  " + "\n");
        }
        mStringBuilder.append("\n\n\n\n\n");
    }

    private byte[] getIvFieldAndIvoke(Object IV) {
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

    private byte[] getEncodedMethodAndInvoke(Object arg) {
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
    private String getAlgorithm(Object arg) {
        try {
            Method getEncoded = arg.getClass().getDeclaredMethod("getAlgorithm");
            getEncoded.setAccessible(true);
            try {
                return (String) getEncoded.invoke(arg);
            } catch (Throwable e) {
                CLogUtils.e("getEncodedMethodAndInvoke  异常   " + e.toString());
                e.printStackTrace();
            }
        } catch (Throwable e) {
            //CLogUtils.e("NoSuchMethodException  异常   " + e.toString());
            e.printStackTrace();
        }
        return null;
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

        public String getKey(){
            if(encodedKey!=null){
                return new String(encodedKey, StandardCharsets.UTF_8);
            }
            return "没有发现Key 或者 Key == null";
        }
    }




    public static class CipherKeyResut{
        public String key;
        public String keyJson;

        public String getKey(){
            if(this.key==null){
                return "";
            }
            return key;
        }
        public String getKeyJson(){
            if(this.keyJson==null){
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
            if(keyField == null) {
                try {
                    keyField = Cipher.class.getDeclaredField("spi");
                    keyField.setAccessible(true);
                } catch (Throwable e) {
                    //CLogUtils.e("getDeclaredField spi error  "+e.getMessage());
                }

            }
            if(keyField != null) {
                spiObject = keyField.get(cipher);
                if (spiObject != null) {
                    cipherKeyResut.keyJson = GsonUtils.obj2str(spiObject);
                    if(cipherKeyResut.keyJson!=null) {
                        SpiObject spiBean = (SpiObject) GsonUtils.getClass(cipherKeyResut.keyJson,SpiObject.class);
                        if (spiBean != null) {
                            cipherKeyResut.key = spiBean.getKey();
                        }
                    }
                }
            }
            //CLogUtils.e("打印结果toString "+cipherKeyResut.toString());
            return cipherKeyResut;
        } catch (Throwable e) {
            CLogUtils.e("getSpiKey error  "+e.getMessage());
        }
        return cipherKeyResut;
    }



}
