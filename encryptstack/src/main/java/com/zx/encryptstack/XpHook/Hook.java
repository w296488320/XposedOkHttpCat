package com.zx.encryptstack.XpHook;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.util.Base64;

import com.zx.encryptstack.utils.CLogUtils;
import com.zx.encryptstack.utils.FileUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;


/**
 * Created by lyh on 2019/6/18.
 */

public class Hook implements IXposedHookLoadPackage, InvocationHandler {

    private final SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("mm分:ss秒:SSS ");
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


    private final String DoFinal = "doFinal";


    private final String Init = "init";


    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            shared = new XSharedPreferences("com.zx.encryptstack", "config");
            shared.reload();
            InvokPackage = shared.getString("APP_INFO", "");

            //先重启 选择 好 要进行Hook的 app
            if (!"".equals(InvokPackage) && lpparam.packageName.equals(InvokPackage)) {
                HookAttach();
            }
        } catch (Exception e) {
            e.printStackTrace();
            CLogUtils.e("Test  Exception  " + e.getMessage());
        }

    }

    /**
     * 字节数组 转 16 进制
     *
     * @param bArr 数据
     * @return
     */
    private String bytesToHexString(byte[] bArr) {
        StringBuffer sb = new StringBuffer(bArr.length);
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
        //String string = Base64.encodeToString();
        XposedHelpers.findAndHookMethod(Base64.class, Base64EncodeToString,
                byte[].class,
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        super.beforeHookedMethod(param);
                        StringBuilder mStringBuilder = new StringBuilder(Base64EncodeToString);
                        byte[] bytes = (byte[]) param.args[0];
                        mStringBuilder.append("方法名字   " + Base64EncodeToString + "\n").
                                append("时间  " + mSimpleDateFormat.format(new Date()) + "\n").
                                append(param.thisObject.getClass().getName()).append(".").append(Base64EncodeToString).append("\n").
                                append(Base64EncodeToString + "参数1 U8编码是   ").append(new String(bytes, StandardCharsets.UTF_8)).append("\n").
                                append(Base64EncodeToString + "参数1 的 16进制是  ").append(bytesToHexString(bytes));
                        FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);

                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);

                        StringBuilder mStringBuilder = new StringBuilder(Base64EncodeToString);
                        mStringBuilder.append("\n");
                        String Result = param.getResult().toString();
                        mStringBuilder.append(Base64EncodeToString + "返回结果   ").append(Result).append("\n");
                        try {
                            int b = 1 / 0;
                        } catch (Exception e) {
                            StackTraceElement[] stackTrace = e.getStackTrace();
                            mStringBuilder.append(" --------------------------  >>>> " + "\n");
                            for (StackTraceElement stackTraceElement : stackTrace) {
                                mStringBuilder.append("   栈信息      ").append(stackTraceElement.getClassName()).append(".").append(stackTraceElement.getMethodName()).append("\n");
                            }
                            mStringBuilder.append("<<<< --------------------------  " + "\n");
                        }
                        mStringBuilder.append("\n\n\n");

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
                        StringBuilder mStringBuilder = new StringBuilder(Base64Encode);
                        byte[] bytes = (byte[]) param.args[0];
                        mStringBuilder.append("方法名字   " + Base64Encode).append("\n").
                                append(mSimpleDateFormat.format(new Date())).append("\n").
                                append(param.thisObject.getClass().getName()).append(".").append(Base64Encode).append("\n").
                                append("参数1  U8编码 " + new String(bytes, StandardCharsets.UTF_8)).append("\n").
                                append("参数2  16进制 编码 " + bytesToHexString(bytes)).append("\n");
                        FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);

                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        StringBuilder mStringBuilder = new StringBuilder(Base64Encode);
                        mStringBuilder.append("\n");
                        byte[] Result = (byte[]) param.getResult();

                        mStringBuilder.append(Base64Encode + "返回结果   U8编码 ").append(new String(Result, StandardCharsets.UTF_8)).append("\n");
                        mStringBuilder.append(Base64Encode + "返回结果   16进制 编码 ").append(bytesToHexString(Result)).append("\n");

                        try {
                            int b = 1 / 0;
                        } catch (Exception e) {
                            StackTraceElement[] stackTrace = e.getStackTrace();
                            mStringBuilder.append(" --------------------------  >>>> " + "\n");
                            for (StackTraceElement stackTraceElement : stackTrace) {
                                mStringBuilder.append("   栈信息      ").append(stackTraceElement.getClassName()).append(".").append(stackTraceElement.getMethodName()).append("\n");
                            }
                            mStringBuilder.append("<<<< --------------------------  " + "\n");
                        }
                        mStringBuilder.append("\n\n\n");

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
                        StringBuilder mStringBuilder = new StringBuilder(Base64Decode);

                        byte[] bytes = (byte[]) param.args[0];
                        mStringBuilder.append("    方法名字   " + Base64Decode + "   \n").
                                append(mSimpleDateFormat.format(new Date()) + "\n").
                                append(param.thisObject.getClass().getName()).append(".").append(Base64Decode).append("\n").
                                append("参数 1  U8编码 ").append(new String(bytes, StandardCharsets.UTF_8)).append("\n");

                        FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);


                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        StringBuilder mStringBuilder = new StringBuilder(Base64Decode);

                        mStringBuilder.append("\n");

                        byte[] Result = (byte[]) param.getResult();
                        mStringBuilder.append("返回结果  U8编码 ").append(new String(Result, StandardCharsets.UTF_8)).append("\n");
                        try {
                            int b = 1 / 0;
                        } catch (Exception e) {
                            StackTraceElement[] stackTrace = e.getStackTrace();
                            mStringBuilder.append(" --------------------------  >>>> " + "\n");
                            for (StackTraceElement stackTraceElement : stackTrace) {
                                mStringBuilder.append("   栈信息      ").append(stackTraceElement.getClassName()).append(".").append(stackTraceElement.getMethodName()).append("\n");
                            }
                            mStringBuilder.append("<<<< --------------------------  " + "\n");
                        }
                        mStringBuilder.append("\n\n\n");

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
                        StringBuilder mStringBuilder = new StringBuilder(Base64Decode);

                        String bytes = param.args[0].toString();
                        mStringBuilder.append("    方法名字   " + Base64Decode + "   \n").
                                append(mSimpleDateFormat.format(new Date()) + "\n").
                                append(param.thisObject.getClass().getName()).append(".").append(Base64Decode).append("\n").
                                append("参数 1   ").append(bytes);
                        FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        StringBuilder mStringBuilder = new StringBuilder(Base64Decode);

                        mStringBuilder.append("\n");
                        byte[] Result = (byte[]) param.getResult();
                        mStringBuilder.append("返回结果  U8编码 ").append(new String(Result, StandardCharsets.UTF_8)).append("\n");
                        mStringBuilder.append("返回结果  16进制 编码 ").append(bytesToHexString(Result)).append("\n");
                        try {
                            int b = 1 / 0;
                        } catch (Exception e) {
                            StackTraceElement[] stackTrace = e.getStackTrace();
                            mStringBuilder.append(" --------------------------  >>>> " + "\n");
                            for (StackTraceElement stackTraceElement : stackTrace) {
                                mStringBuilder.append("   栈信息      ").append(stackTraceElement.getClassName()).append(".").append(stackTraceElement.getMethodName()).append("\n");
                            }
                            mStringBuilder.append("<<<< --------------------------  " + "\n");
                        }
                        mStringBuilder.append("\n\n\n");

                        FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);
                    }
                }
        );


        //MD5
        //byte[] bytes = md5.digest(string.getBytes());
        XposedHelpers.findAndHookMethod(MessageDigest.class, Md5Digest,
                byte[].class,
                new XC_MethodHook() {

                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        super.beforeHookedMethod(param);
                        StringBuilder mStringBuilder = new StringBuilder(Md5Digest);

                        byte[] bytes = (byte[]) param.args[0];
                        mStringBuilder.append("    方法名字   " + Md5Digest + "   \n").
                                append(mSimpleDateFormat.format(new Date()) + "\n").
                                append(param.thisObject.getClass().getName()).append(".").append(Md5Digest).append("\n").
                                append("参数 1   U8编码   ").append(new String(bytes, StandardCharsets.UTF_8)).append("\n");
                        FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);

                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        StringBuilder mStringBuilder = new StringBuilder(Md5Digest);

                        mStringBuilder.append("\n");
                        byte[] Result = (byte[]) param.getResult();
                        String result = "";
                        for (byte b : Result) {
                            String temp = Integer.toHexString(b & 0xff);
                            if (temp.length() == 1) {
                                temp = "0" + temp;
                            }
                            result += temp;
                        }
                        mStringBuilder.append("Md5 返回结果  ").append(result).append("\n");
                        try {
                            int b = 1 / 0;
                        } catch (Exception e) {
                            StackTraceElement[] stackTrace = e.getStackTrace();
                            mStringBuilder.append(" --------------------------  >>>> " + "\n");
                            for (StackTraceElement stackTraceElement : stackTrace) {
                                mStringBuilder.append("   栈信息      ").append(stackTraceElement.getClassName()).append(".").append(stackTraceElement.getMethodName()).append("\n");
                            }
                            mStringBuilder.append("<<<< --------------------------  " + "\n");
                        }
                        mStringBuilder.append("\n\n\n");

                        FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);
                    }
                }
        );

        // 这个方法AES或者 RSA都有可能用
        // cipher.doFinal(content.getBytes("UTF-8"));
        XposedHelpers.findAndHookMethod(Cipher.class, DoFinal,
                byte[].class,
                new XC_MethodHook() {

                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        super.beforeHookedMethod(param);
                        byte[] bytes = (byte[]) param.args[0];
                        StringBuilder mStringBuilder = new StringBuilder(DoFinal);

                        mStringBuilder.append("    方法名字   " + DoFinal + "   \n").
                                append(mSimpleDateFormat.format(new Date()) + "\n").
                                append(param.thisObject.getClass().getName()).append(".").append(DoFinal).append("\n").
                                append("参数 1   U8编码 ").append(new String(bytes, StandardCharsets.UTF_8)).append("\n").
                                append("参数 2   16进制 编码 ").append(bytesToHexString(bytes)).append("\n");
                        FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);

                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        StringBuilder mStringBuilder = new StringBuilder(DoFinal);

                        mStringBuilder.append("\n");
                        byte[] Result = (byte[]) param.getResult();

                        mStringBuilder.append("加密  16进制   返回结果  ").append(bytesToHexString(Result)).append("\n");
                        mStringBuilder.append("加密  U8编码    返回结果  ").append(new String(Result, StandardCharsets.UTF_8)).append("\n");

                        try {
                            int b = 1 / 0;
                        } catch (Exception e) {
                            StackTraceElement[] stackTrace = e.getStackTrace();
                            mStringBuilder.append(" --------------------------  >>>> " + "\n");
                            for (StackTraceElement stackTraceElement : stackTrace) {
                                mStringBuilder.append("   栈信息      ").append(stackTraceElement.getClassName()).append(".").append(stackTraceElement.getMethodName()).append("\n");
                            }
                            mStringBuilder.append("<<<< --------------------------  " + "\n");
                        }
                        mStringBuilder.append("\n\n\n");

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
                        StringBuilder mStringBuilder = new StringBuilder(Init);


                        //这个方法 主要是 拿到 私钥
                        Object Key = param.args[1];
                        String algorithm = getAlgorithm(Key);

                        mStringBuilder.append(algorithm == null ? "未找到  加密方式 " : ("       " + algorithm)).append("   加密初始化(ECB 模式)    方法名字   " + Init + "\n").
                                append(mSimpleDateFormat.format(new Date()) + "\n").
                                append(param.thisObject.getClass().getName()).append(".").append(Init).append("\n");


                        byte[] encodedMethodAndInvoke = getEncodedMethodAndInvoke(Key);
                        if (encodedMethodAndInvoke != null) {
                            mStringBuilder.append("私钥的 UTF-8 编码    ").append(new String(encodedMethodAndInvoke, StandardCharsets.UTF_8)).append("\n");
                            mStringBuilder.append("私钥的 16 进制  编码    ").append(bytesToHexString(encodedMethodAndInvoke)).append("\n");
                        }
                        FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);
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
                        StringBuilder mStringBuilder = new StringBuilder(Init);

                        CLogUtils.e("参数 类型 int   key   AlgorithmParameters");
                        //这个方法 主要是 拿到 私钥
                        Object Key = param.args[1];
                        String algorithm = getAlgorithm(Key);
                        Object IV = param.args[2];

                        //getAlgorithm
                        mStringBuilder.append(algorithm == null ? "未找到  加密方式 " : ("       " + algorithm)).append("  加密初始化(CBC 模式  三个 参数   需要  IV)    方法名字   " + Init + "\n").
                                append(mSimpleDateFormat.format(new Date()) + "\n").
                                append(param.thisObject.getClass().getName()).append(".").append(Init).append("\n");


                        byte[] encodedMethodAndInvoke = getEncodedMethodAndInvoke(Key);
                        if (encodedMethodAndInvoke != null) {
                            mStringBuilder.append("私钥的 UTF-8 编码    ").append(new String(encodedMethodAndInvoke, StandardCharsets.UTF_8)).append("\n");
                            mStringBuilder.append("私钥的 16 进制  编码    ").append(bytesToHexString(encodedMethodAndInvoke)).append("\n");
                        }
                        byte[] getIvFieldAndIvoke = getIvFieldAndIvoke(IV);
                        if (getIvFieldAndIvoke != null) {
                            mStringBuilder.append("IV 的 UTF-8 编码    ").append(new String(getIvFieldAndIvoke, StandardCharsets.UTF_8)).append("\n");
                            mStringBuilder.append("IV 的 16 进制  编码    ").append(bytesToHexString(getIvFieldAndIvoke)).append("\n");
                        }


                        FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);

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
                        StringBuilder mStringBuilder = new StringBuilder(Init);

                        CLogUtils.e("参数 类型 int   key   AlgorithmParameters   SecureRandom");

                        //这个方法 主要是 拿到 私钥
                        Object Key = param.args[1];
                        String algorithm = getAlgorithm(Key);
                        Object IV = param.args[2];

                        //getAlgorithm
                        mStringBuilder.append(algorithm == null ? "未找到  加密方式 " : ("       " + algorithm)).append("            加密初始化(CBC 模式  三个 参数   需要  IV)    方法名字   " + Init + "\n").
                                append(mSimpleDateFormat.format(new Date()) + "\n").
                                append(param.thisObject.getClass().getName()).append(".").append(Init).append("\n");


                        byte[] encodedMethodAndInvoke = getEncodedMethodAndInvoke(Key);
                        if (encodedMethodAndInvoke != null) {
                            mStringBuilder.append("私钥的 UTF-8 编码    ").append(new String(encodedMethodAndInvoke, StandardCharsets.UTF_8)).append("\n");
                            mStringBuilder.append("私钥的 16 进制  编码    ").append(bytesToHexString(encodedMethodAndInvoke)).append("\n");
                        }
                        byte[] getIvFieldAndIvoke = getIvFieldAndIvoke(IV);
                        if (getIvFieldAndIvoke != null) {
                            mStringBuilder.append("IV 的 UTF-8 编码    ").append(new String(getIvFieldAndIvoke, StandardCharsets.UTF_8)).append("\n");
                            mStringBuilder.append("IV 的 16 进制  编码    ").append(bytesToHexString(getIvFieldAndIvoke)).append("\n");
                        }

                        FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);

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
                        StringBuilder mStringBuilder = new StringBuilder(Init);

                        CLogUtils.e("参数 类型 int   key   AlgorithmParameterSpec  ");
                        //这个方法 主要是 拿到 私钥
                        Object Key = param.args[1];
                        String algorithm = getAlgorithm(Key);
                        Object IV = param.args[2];

                        //getAlgorithm
                        mStringBuilder.append(algorithm == null ? "未找到  加密方式 " : ("       " + algorithm)).append("        加密初始化(CBC 模式  三个 参数   需要  IV)    方法名字   " + Init + "\n").
                                append(mSimpleDateFormat.format(new Date()) + "\n").
                                append(param.thisObject.getClass().getName()).append(".").append(Init).append("\n");


                        byte[] encodedMethodAndInvoke = getEncodedMethodAndInvoke(Key);
                        if (encodedMethodAndInvoke != null) {
                            mStringBuilder.append("私钥的 UTF-8 编码    ").append(new String(encodedMethodAndInvoke, StandardCharsets.UTF_8)).append("\n");
                            mStringBuilder.append("私钥的 16 进制  编码    ").append(bytesToHexString(encodedMethodAndInvoke)).append("\n");
                        }
                        byte[] getIvFieldAndIvoke = getIvFieldAndIvoke(IV);
                        if (getIvFieldAndIvoke != null) {
                            mStringBuilder.append("IV 的 UTF-8 编码    ").append(new String(getIvFieldAndIvoke, StandardCharsets.UTF_8)).append("\n");
                            mStringBuilder.append("IV 的 16 进制  编码    ").append(bytesToHexString(getIvFieldAndIvoke)).append("\n");
                        }

                        FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);

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
                        StringBuilder mStringBuilder = new StringBuilder(Init);

                        CLogUtils.e("参数 类型 int   key   AlgorithmParameterSpec SecureRandom ");
                        //这个方法 主要是 拿到 私钥
                        Object Key = param.args[1];
                        String algorithm = getAlgorithm(Key);
                        Object IV = param.args[2];

                        //getAlgorithm
                        mStringBuilder.append(algorithm == null ? "未找到  加密方式 " : ("       " + algorithm)).append("  加密初始化(CBC 模式  三个 参数   需要  IV)    方法名字   " + Init + "\n").
                                append(mSimpleDateFormat.format(new Date()) + "\n").
                                append(param.thisObject.getClass().getName()).append(".").append(Init).append("\n");


                        byte[] encodedMethodAndInvoke = getEncodedMethodAndInvoke(Key);
                        if (encodedMethodAndInvoke != null) {
                            mStringBuilder.append("私钥的 UTF-8 编码    ").append(new String(encodedMethodAndInvoke, StandardCharsets.UTF_8)).append("\n");
                            mStringBuilder.append("私钥的 16 进制  编码    ").append(bytesToHexString(encodedMethodAndInvoke)).append("\n");
                        }
                        byte[] getIvFieldAndIvoke = getIvFieldAndIvoke(IV);
                        if (getIvFieldAndIvoke != null) {
                            mStringBuilder.append("IV 的 UTF-8 编码    ").append(new String(getIvFieldAndIvoke, StandardCharsets.UTF_8)).append("\n");
                            mStringBuilder.append("IV 的 16 进制  编码    ").append(bytesToHexString(getIvFieldAndIvoke)).append("\n");
                        }

                        FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);

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
                        StringBuilder mStringBuilder = new StringBuilder(Init);

                        CLogUtils.e("参数 类型 int   key    SecureRandom ");
                        //这个方法 主要是 拿到 私钥
                        Object Key = param.args[1];
                        String algorithm = getAlgorithm(Key);
                        Object IV = param.args[2];

                        //getAlgorithm
                        mStringBuilder.append(algorithm == null ? "未找到  加密方式 " : ("       " + algorithm)).append("  加密初始化(CBC 模式  三个 参数   需要  IV)    方法名字   " + Init + "\n").
                                append(mSimpleDateFormat.format(new Date()) + "\n").
                                append(param.thisObject.getClass().getName()).append(".").append(Init).append("\n");


                        byte[] encodedMethodAndInvoke = getEncodedMethodAndInvoke(Key);
                        if (encodedMethodAndInvoke != null) {
                            mStringBuilder.append("私钥的 UTF-8 编码    ").append(new String(encodedMethodAndInvoke, StandardCharsets.UTF_8)).append("\n");
                            mStringBuilder.append("私钥的 16 进制  编码    ").append(bytesToHexString(encodedMethodAndInvoke)).append("\n");
                        }
                        byte[] getIvFieldAndIvoke = getIvFieldAndIvoke(IV);
                        if (getIvFieldAndIvoke != null) {
                            mStringBuilder.append("IV 的 UTF-8 编码    ").append(new String(getIvFieldAndIvoke, StandardCharsets.UTF_8)).append("\n");
                            mStringBuilder.append("IV 的 16 进制  编码    ").append(bytesToHexString(getIvFieldAndIvoke)).append("\n");
                        }

                        FileUtils.SaveString(mOtherContext, mStringBuilder.toString(), InvokPackage);

                    }

                }
        );





    }

    private byte[] getIvFieldAndIvoke(Object IV) {
        try {
            Method getEncoded = IV.getClass().getDeclaredMethod("getIV");
            getEncoded.setAccessible(true);
            try {
                return (byte[]) getEncoded.invoke(IV);
            } catch (IllegalAccessException | InvocationTargetException e) {
                CLogUtils.e("getEncodedMethodAndInvoke  异常   " + e.getMessage());
                e.printStackTrace();
            }
        } catch (NoSuchMethodException e) {
            CLogUtils.e("NoSuchMethodException  异常   " + e.getMessage());
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
                CLogUtils.e("getEncodedMethodAndInvoke  异常   " + e.getMessage());
                e.printStackTrace();
            }
        } catch (NoSuchMethodException e) {
            CLogUtils.e("NoSuchMethodException  异常   " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 获取 加密方法的 模式 是 AES  还是 RSA
     *
     * @param arg
     * @return
     */
    private String getAlgorithm(Object arg) {
        try {
            Method getEncoded = arg.getClass().getDeclaredMethod("getAlgorithm");
            getEncoded.setAccessible(true);
            try {
                return (String) getEncoded.invoke(arg);
            } catch (IllegalAccessException | InvocationTargetException e) {
                CLogUtils.e("getEncodedMethodAndInvoke  异常   " + e.getMessage());
                e.printStackTrace();
            }
        } catch (NoSuchMethodException e) {
            CLogUtils.e("NoSuchMethodException  异常   " + e.getMessage());
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
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return null;
    }
}
