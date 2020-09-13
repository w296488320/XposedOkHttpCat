package com.zx.encryptstack.XpHook;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Base64;

import com.zx.encryptstack.utils.CLogUtils;
import com.zx.encryptstack.utils.FileUtils;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;


/**
 * Created by lyh on 2019/6/18.
 */

public class Hook implements IXposedHookLoadPackage, InvocationHandler {

    private final SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("MM-dd  mm分:ss秒");
    /**
     * 进行 注入的 app 名字
     */
    public static volatile String InvokPackage = null;

    private XSharedPreferences shared;

    private Context mOtherContext;
    private ClassLoader mLoader;


    private final String Base64EncodeToString = "encodeToString";
    private final String Base64Encode = "encode";
    private final String Base64Decode = "decode";
    private final String Md5Digest = "digest";
    private final String Update = "update";
    private final String DoFinal = "doFinal";
    private final String Init = "init";
    //private final String getBytes = "getBytes";


    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            CLogUtils.e("加载包名 "+lpparam.packageName);
            shared = new XSharedPreferences("com.zx.encryptstack", "config");
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
                        CLogUtils.e("拿到 classloader 5555");
                        HookEncrypMethod();
                        CLogUtils.e("Hook执行完毕  ");

                    }
                });

    }


    private void HookEncrypMethod() {


        try {
            //String string = Base64.encodeToString(byte[],int);
            XposedHelpers.findAndHookMethod(Base64.class, Base64EncodeToString,
                    byte[].class,
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            byte[] bytes = (byte[]) param.args[0];
                            StringBuilder stringBuffer = new StringBuilder(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>").append("\n");
                            stringBuffer.append(Base64EncodeToString + "方法名字   Base64." + Base64EncodeToString + "\n");
                            stringBuffer.append("时间  ").append(mSimpleDateFormat.format(new Date())).append("\n");
                            stringBuffer.append(param.thisObject != null ? param.thisObject.getClass().getName() : "").append(Base64EncodeToString).append("\n");
                            stringBuffer.append(Base64EncodeToString + "参数1 U8编码是   ").append(new String(bytes, StandardCharsets.UTF_8)).append("\n");
                            stringBuffer.append(Base64EncodeToString + "参数1 的 16进制是  ").append(bytesToHexString(bytes));


                            FileUtils.SaveString(mOtherContext, stringBuffer.toString(), InvokPackage);

                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);

                            StringBuilder mStringBuilder = new StringBuilder(Base64EncodeToString);
                            mStringBuilder.append("\n");
                            String Result = param.getResult().toString();
                            mStringBuilder.append(Base64EncodeToString + "返回结果   ").append(Result).append("\n").
                                    append("Base64.encodeToString(byte[],int)  返回结果 参数1 U8编码是 ").append(new String((byte[]) param.args[0], StandardCharsets.UTF_8)).append("\n").
                                    append("Base64.encodeToString(byte[],int)  返回结果 参数1 16进制编码是 ").append(bytesToHexString((byte[]) param.args[0])).append("\n");

                            getStackTraceMessage(mStringBuilder);
                            mStringBuilder.append("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<").append("\n\n\n\n\n");
                            FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);
                        }
                    }
            );

            //byte[] encode = Base64.encode();
            XposedHelpers.findAndHookMethod(Base64.class, Base64Encode,
                    byte[].class,
                    int.class,
                    new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            byte[] bytes = (byte[]) param.args[0];
                            StringBuffer stringBuffer = new StringBuffer(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>").append("\n");

                            String mStringBuilder = Base64Encode + "方法名字   Base64." + Base64Encode + "\n" +
                                    mSimpleDateFormat.format(new Date()) + "\n" +
                                    param.thisObject != null ? param.thisObject.getClass().getName() : "" + "." + Base64Encode + "\n" +
                                    "参数1  U8编码 " + new String(bytes, StandardCharsets.UTF_8) + "\n" +
                                    "参数2  16进制 编码 " + bytesToHexString(bytes) + "\n";

                            stringBuffer.append(mStringBuilder);

                            FileUtils.SaveString(mOtherContext, stringBuffer.toString(), InvokPackage);

                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
                            StringBuilder mStringBuilder = new StringBuilder(Base64Encode);
                            mStringBuilder.append("\n");
                            byte[] Result = (byte[]) param.getResult();

                            mStringBuilder.append(Base64Encode + "返回结果   U8编码 ").append(new String(Result, StandardCharsets.UTF_8)).append("\n");
                            mStringBuilder.append(Base64Encode + "返回结果   16进制 编码 ").append(bytesToHexString(Result)).append("\n");

                            getStackTraceMessage(mStringBuilder);
                            mStringBuilder.append("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<").append("\n\n\n\n\n");

                            FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);
                        }
                    }
            );

            //byte[] decode = Base64.decode(byte[],int);
            XposedHelpers.findAndHookMethod(Base64.class, Base64Decode,
                    byte[].class,
                    int.class,
                    new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);

                            byte[] bytes = (byte[]) param.args[0];
                            StringBuffer stringBuffer = new StringBuffer(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>").append("\n");

                            String mStringBuilder = Base64Decode + "    方法名字   Base64." + Base64Decode + "   \n" +
                                    mSimpleDateFormat.format(new Date()) + "\n"
                                    + param.thisObject != null ? param.thisObject.getClass().getName() : "" + "." + Base64Decode + "\n" +
                                    "参数 1  U8编码 " + new String(bytes, StandardCharsets.UTF_8) + "\n";
                            stringBuffer.append(mStringBuilder);
                            FileUtils.SaveString(mOtherContext, stringBuffer.toString(), InvokPackage);


                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
                            StringBuilder mStringBuilder = new StringBuilder("Base64." + Base64Decode);

                            mStringBuilder.append("\n");

                            byte[] Result = (byte[]) param.getResult();
                            mStringBuilder.append("返回结果  U8编码 ").append(new String(Result, StandardCharsets.UTF_8)).append("\n").
                                    append("加密之前的 内容 Base64.decode(byte[],int); 参数1  U8编码内容  ").
                                    append(new String((byte[]) param.args[0], StandardCharsets.UTF_8)).append("\n");


                            getStackTraceMessage(mStringBuilder);
                            mStringBuilder.append("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<").append("\n\n\n\n\n");

                            FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);
                        }
                    }
            );

//        byte[] decode = Base64.decode(String,int);
            XposedHelpers.findAndHookMethod(Base64.class, Base64Decode,
                    String.class,
                    int.class,
                    new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            StringBuffer stringBuffer = new StringBuffer(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>").append("\n");
                            String bytes = param.args[0].toString();
                            stringBuffer.append("    方法名字   Base64." + Base64Decode + "   \n").
                                    append(mSimpleDateFormat.format(new Date())).append("\n");
                            if (param.thisObject != null) {
                                stringBuffer.append(param.thisObject != null ? param.thisObject.getClass().getName() : "").append(".").append(Base64Decode).append("\n");
                            }
                            stringBuffer.append("Base64.decode(String,int)  参数 1   ").append(bytes).append("\n");
                            FileUtils.SaveString(mOtherContext, stringBuffer.toString(), InvokPackage);
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
                            StringBuilder mStringBuilder = new StringBuilder("Base." + Base64Decode);

                            mStringBuilder.append("\n");
                            byte[] Result = (byte[]) param.getResult();
                            mStringBuilder.append("返回结果  U8编码 ").append(new String(Result, StandardCharsets.UTF_8)).append("\n");
                            mStringBuilder.append("返回结果  16进制 编码 ").append(bytesToHexString(Result)).append("\n").
                                    append("加密之前的 内容 Base64.decode(String,int); 参数1内容  ").append(param.args[0].toString()).append("\n");


                            getStackTraceMessage(mStringBuilder);
                            mStringBuilder.append("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<").append("\n\n\n\n\n");

                            FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);
                        }
                    }
            );


            //MD5 和 sha-1
            //byte[] bytes = md5.digest(string.getBytes());
            XposedHelpers.findAndHookMethod(MessageDigest.class, Md5Digest,
                    byte[].class,
                    new XC_MethodHook() {


                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);

                            byte[] bytes = (byte[]) param.args[0];

                            //加密 类型
                            MessageDigest thisObject = (MessageDigest) param.thisObject;
                            String algorithm = thisObject.getAlgorithm();
                            StringBuffer stringBuffer = new StringBuffer(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>").append("\n");
                            String mStringBuilder = Md5Digest + "    名字   " + algorithm + "加密 " + Md5Digest + "   \n" +
                                    mSimpleDateFormat.format(new Date()) + "\n" +
                                    param.thisObject != null ? param.thisObject.getClass().getName() : "" + "." + Md5Digest + "\n" +
                                    "参数 1   U8编码   " + new String(bytes, StandardCharsets.UTF_8) + "\n";
                            stringBuffer.append(mStringBuilder);
                            FileUtils.SaveString(mOtherContext, stringBuffer.toString(), InvokPackage);

                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
                            StringBuilder mStringBuilder = new StringBuilder(Md5Digest);

                            MessageDigest msgDitest = (MessageDigest) param.thisObject;

                            String algorithm = msgDitest.getAlgorithm();
                            mStringBuilder.append("\n");
                            byte[] Result = (byte[]) param.getResult();

                            StringBuilder result = getMd5(Result);

                            mStringBuilder.append("MessageDigest 类型  ").append(algorithm).append("\n").
                                    append(" 返回结果  ").append(result).append("\n").
                                    append("MessageDigest digest  toString 返回结果 ").append(msgDitest.toString()).append("\n").
                                    append("加密之前 参数1  U8编码 ").append(new String((byte[]) param.args[0], StandardCharsets.UTF_8)).append("\n").
                                    append("加密之前 参数1  16进制编码 ").append(bytesToHexString((byte[]) param.args[0])).append("\n");

                            getStackTraceMessage(mStringBuilder);
                            mStringBuilder.append("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<").append("\n\n\n\n\n");

                            FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);
                        }
                    }
            );


            //MD5 和 sha-1
            //byte[] bytes = md5.digest();
            XposedHelpers.findAndHookMethod(MessageDigest.class, Md5Digest,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);

                            StringBuffer stringBuffer = new StringBuffer(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>").append("\n");

                            //加密 类型
                            MessageDigest thisObject = (MessageDigest) param.thisObject;
                            String algorithm = thisObject.getAlgorithm();

                            String mStringBuilder = Md5Digest + "    名字   " + algorithm + "加密 " + Md5Digest + "   \n" +
                                    mSimpleDateFormat.format(new Date()) + "\n" +
                                    param.thisObject != null ? param.thisObject.getClass().getName() : "" + "." + Md5Digest + "\n";
                            stringBuffer.append(mStringBuilder);
                            FileUtils.SaveString(mOtherContext, stringBuffer.toString(), InvokPackage);


                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
                            StringBuilder mStringBuilder = new StringBuilder(Md5Digest);

                            MessageDigest msgDitest = (MessageDigest) param.thisObject;


//                            msgDitest.update();
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
                            mStringBuilder.append("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<").append("\n\n\n\n\n");

                            FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);
                        }
                    }
            );


//            MessageDigest msgDitest = MessageDigest.getInstance("SHA-1");
//            msgDitest.update(Bytes[]);
            XposedHelpers.findAndHookMethod(MessageDigest.class, Update,
                    byte[].class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);

                            byte[] bytes = (byte[]) param.args[0];
                            //加密 类型
                            MessageDigest thisObject = (MessageDigest) param.thisObject;
                            String algorithm = thisObject.getAlgorithm();
                            StringBuffer stringBuffer = new StringBuffer(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>").append("\n");

                            String mStringBuilder = Md5Digest + "    名字   " + algorithm + "加密 " + Update + "   \n" +
                                    mSimpleDateFormat.format(new Date()) + "\n" +
                                    param.thisObject != null ? param.thisObject.getClass().getName() : "" + "." + Md5Digest + "\n" +
                                    "参数 1   U8编码   " + new String(bytes, StandardCharsets.UTF_8) + "\n";
                            stringBuffer.append(mStringBuilder);
                            FileUtils.SaveString(mOtherContext, stringBuffer.toString(), InvokPackage);


                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
                            StringBuilder mStringBuilder = new StringBuilder("msgDitest.update(Bytes[]) 无返回结果");

                            MessageDigest msgDitest = (MessageDigest) param.thisObject;

                            String algorithm = msgDitest.getAlgorithm();
                            mStringBuilder.append("\n");
                            mStringBuilder.append("MessageDigest  类型").append(algorithm).append("\n").
                                    append("MessageDigest   toString ").append(msgDitest.toString()).append("\n").
                                    append("msgDitest.update(Bytes[])  参数1 U8编码 " + new String((byte[]) param.args[0], StandardCharsets.UTF_8)).append("\n").
                                    append("msgDitest.update(Bytes[])  参数1 16进制 编码 " + bytesToHexString((byte[]) param.args[0])).append("\n");



                            getStackTraceMessage(mStringBuilder);
                            mStringBuilder.append("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<").append("\n\n\n\n\n");

                            FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);
                        }
                    }
            );


            // 这个方法AES或者 RSA都有可能用
            //byte[] cipher.doFinal(Byte[]);
            XposedHelpers.findAndHookMethod(Cipher.class, DoFinal,
                    byte[].class,
                    new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            byte[] bytes = (byte[]) param.args[0];
                            StringBuffer stringBuffer = new StringBuffer(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>").append("\n");

                            String mStringBuilder = DoFinal + "    方法名字   " + DoFinal + "   \n" +
                                    mSimpleDateFormat.format(new Date()) + "\n" +
                                    param.thisObject != null ? param.thisObject.getClass().getName() : "" + "." + DoFinal + "\n" +
                                    "参数 1   U8编码 " + new String(bytes, StandardCharsets.UTF_8) + "\n" +
                                    "参数 2   16进制 编码 " + bytesToHexString(bytes) + "\n";
                            stringBuffer.append(mStringBuilder);
                            FileUtils.SaveString(mOtherContext, stringBuffer.toString(), InvokPackage);

                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
                            StringBuilder mStringBuilder = new StringBuilder(DoFinal);

                            mStringBuilder.append("\n");
                            byte[] Result = (byte[]) param.getResult();
                            mStringBuilder.append("byte[] cipher.doFinal(Byte[]) 参数1 U8编码  ").append(new String((byte[]) param.args[0], StandardCharsets.UTF_8)).append("\n");
                            mStringBuilder.append("byte[] cipher.doFinal(Byte[]) 参数1 16进制编码  ").append(bytesToHexString((byte[]) param.args[0])).append("\n");

                            mStringBuilder.append("byte[] cipher.doFinal(Byte[])  加密  16进制   返回结果  ").append(bytesToHexString(Result)).append("\n");
                            mStringBuilder.append("byte[] cipher.doFinal(Byte[])  加密  U8编码    返回结果  ").append(new String(Result, StandardCharsets.UTF_8)).append("\n");

                            getStackTraceMessage(mStringBuilder);
                            mStringBuilder.append("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<").append("\n\n\n\n\n");

                            FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);

                        }
                    }
            );


            //cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            XposedHelpers.findAndHookMethod(Cipher.class, Init,
                    int.class,
                    Key.class,
                    new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            StringBuilder stringBuffer = new StringBuilder(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>").append("\n");

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
                            stringBuffer.append("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<").append("\n\n\n\n\n");


                            FileUtils.SaveString(mOtherContext, stringBuffer.toString().toString(), InvokPackage);
                        }

                    }
            );


            //AES加密算法 初始化的时候 两个 参数 是ECB  模式 不需要 IV
            //三个 参数 的 需要 IV
            //cipher.init(Cipher.ENCRYPT_MODE, skeySpec,IvParameterSpec);
            XposedHelpers.findAndHookMethod(Cipher.class, Init,
                    int.class,
                    Key.class,
                    AlgorithmParameters.class,
                    new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            StringBuilder stringBuffer = new StringBuilder(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>").append("\n");
                            stringBuffer.append("三个参数 需要 IV  cipher.init(Cipher.ENCRYPT_MODE, skeySpec,IvParameterSpec) ").append("\n");

                            CLogUtils.e("参数 类型 int   key   AlgorithmParameters");
                            //这个方法 主要是 拿到 私钥
                            Object Key = param.args[1];
                            String algorithm = getAlgorithm(Key);
                            Object IV = param.args[2];

                            //getAlgorithm
                            stringBuffer.append(algorithm == null ? "未找到  加密方式 " : ("       " + algorithm)).append("  加密初始化(CBC 模式  三个 参数   需要  IV)    方法名字   " + Init + "\n").
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
                            stringBuffer.append("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<").append("\n\n\n\n\n");


                            FileUtils.SaveString(mOtherContext, stringBuffer.toString(), InvokPackage);

                        }

                    }
            );


            //cipher.init(Cipher.ENCRYPT_MODE, skeySpec,IvParameterSpec);
            XposedHelpers.findAndHookMethod(Cipher.class, Init,
                    int.class,
                    Key.class,
                    AlgorithmParameters.class,
                    SecureRandom.class,
                    new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            StringBuilder stringBuffer = new StringBuilder(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>").append("\n");
                            stringBuffer.append("cipher.init(Cipher.ENCRYPT_MODE, skeySpec,IvParameterSpec) ").append("\n");

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
                            stringBuffer.append("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<").append("\n\n\n\n\n");

                            FileUtils.SaveString(mOtherContext, stringBuffer.toString(), InvokPackage);

                        }

                    }
            );


            XposedHelpers.findAndHookMethod(Cipher.class, Init,
                    int.class,
                    Key.class,
                    AlgorithmParameterSpec.class,
                    new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            StringBuilder stringBuffer = new StringBuilder(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>").append("\n");
                            stringBuffer.append("cipher.init(Cipher.ENCRYPT_MODE, skeySpec,AlgorithmParameterSpec ) ").append("\n");

                            CLogUtils.e("参数 类型 int   key   AlgorithmParameterSpec  ");
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
                            stringBuffer.append("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<").append("\n\n\n\n\n");

                            FileUtils.SaveString(mOtherContext, stringBuffer.toString(), InvokPackage);

                        }

                    }
            );

            XposedHelpers.findAndHookMethod(Cipher.class, Init,
                    int.class,
                    Key.class,
                    AlgorithmParameterSpec.class,
                    SecureRandom.class,
                    new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            StringBuilder stringBuffer = new StringBuilder(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>").append("\n");
                            stringBuffer.append("cipher.init(Cipher.ENCRYPT_MODE, skeySpec,AlgorithmParameterSpec,SecureRandom) ").append("\n");

                            CLogUtils.e("参数 类型 int   key   AlgorithmParameterSpec SecureRandom ");
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
                            stringBuffer.append("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<").append("\n\n\n\n\n");

                            FileUtils.SaveString(mOtherContext, stringBuffer.toString(), InvokPackage);

                        }

                    }
            );


            XposedHelpers.findAndHookMethod(Cipher.class, Init,
                    int.class,
                    Key.class,
                    SecureRandom.class,
                    new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            StringBuilder stringBuffer = new StringBuilder(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>").append("\n");
                            stringBuffer.append("cipher.init(Cipher.ENCRYPT_MODE, skeySpec,SecureRandom) ").append("\n");

                            CLogUtils.e("参数 类型 int   key    SecureRandom ");
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
                            stringBuffer.append("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<").append("\n\n\n\n\n");

                            FileUtils.SaveString(mOtherContext, stringBuffer.toString(), InvokPackage);

                        }

                    }
            );

            //new JsonObject();
            XposedHelpers.findAndHookConstructor(JSONObject.class, String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    StringBuilder mStringBuilder = new StringBuilder("JsonObject 构造方法 " + "\n");
                    if (param.thisObject != null) {
                        String json = ((JSONObject) param.thisObject).toString();
                        mStringBuilder.append("Json 内容 :  ").append(json).append("\n");
                        mStringBuilder.append("Json 十六进制编码   :  ").append(bytesToHexString(json.getBytes())).append("\n\n");

                        getStackTraceMessage(mStringBuilder);

                        FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);
                    }
                }
            });


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
        } catch (Throwable throwable) {
            CLogUtils.e("发现异常 " + throwable.toString());
        }

    }


    /**
     * sha256_HMAC加密
     *
     * @param message 消息
     * @param secret  秘钥
     * @return 加密后字符串
     */
    public static String sha256_HMAC(String message, String secret) {
        String hash = "";
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] bytes = sha256_HMAC.doFinal(message.getBytes());
            hash = byteArrayToHexString(bytes);
        } catch (Throwable e) {
            System.out.println("Error HmacSHA256 ===========" + e.toString());
        }
        return hash;
    }

    /**
     * 将加密后的字节数组转换成字符串
     *
     * @param b 字节数组
     * @return 字符串
     */
    public static String byteArrayToHexString(byte[] b) {
        StringBuilder hs = new StringBuilder();
        String stmp;
        for (int n = 0; b != null && n < b.length; n++) {
            stmp = Integer.toHexString(b[n] & 0XFF);
            if (stmp.length() == 1)
                hs.append('0');
            hs.append(stmp);
        }
        return hs.toString().toLowerCase();
    }


    @NonNull
    private StringBuilder getMd5(byte[] result2) {
        StringBuilder result1 = new StringBuilder("");
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
        mStringBuilder.append("\n");
    }

    private byte[] getIvFieldAndIvoke(Object IV) {
        try {
            Method getEncoded = IV.getClass().getDeclaredMethod("getIV");
            getEncoded.setAccessible(true);
            try {
                return (byte[]) getEncoded.invoke(IV);
            } catch (IllegalAccessException | InvocationTargetException e) {
                CLogUtils.e("getEncodedMethodAndInvoke  异常   " + e.toString());
                e.printStackTrace();
            }
        } catch (NoSuchMethodException e) {
            CLogUtils.e("NoSuchMethodException  异常   " + e.toString());
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
            } catch (IllegalAccessException | InvocationTargetException e) {
                CLogUtils.e("getEncodedMethodAndInvoke  异常   " + e.toString());
                e.printStackTrace();
            }
        } catch (NoSuchMethodException e) {
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
            } catch (IllegalAccessException | InvocationTargetException e) {
                CLogUtils.e("getEncodedMethodAndInvoke  异常   " + e.toString());
                e.printStackTrace();
            }
        } catch (NoSuchMethodException e) {
            CLogUtils.e("NoSuchMethodException  异常   " + e.toString());
            e.printStackTrace();
        }
        return null;
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
}
