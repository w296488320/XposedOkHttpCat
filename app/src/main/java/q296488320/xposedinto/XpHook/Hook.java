package q296488320.xposedinto.XpHook;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import dalvik.system.DexClassLoader;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import q296488320.xposedinto.utils.CLogUtils;


/**
 * Created by lyh on 2019/6/18.
 */

public class Hook implements IXposedHookLoadPackage, InvocationHandler {


    private String INTERCEPTORPATH = "/storage/emulated/0/OkHttpCat/bbb.dex";
    private String OUTRPATH = "/storage/emulated/0/OkHttpCat/Out";


    /**
     * 进行 注入的 app 名字
     */
    public static volatile String InvokPackage = null;
    /**
     * 模式 hook 不同的 构造 函数
     */
    public static volatile String MODEL=null;



    //别的进程的 context
    private Context mOtherContext;
    //别的进程的 mLoader
    private volatile ClassLoader mLoader = null;

    Class<?> OkHttpBuilder = null;
    Class<?> OkHttpClient = null;

    private String ClassPath = "okhttp3.OkHttpClient$Builder";
    //使用 共享 数据的 XSharedPreferences
    private XSharedPreferences shared;
    private Class<?> mHttpLoggingInterceptor;
    private Class<?> mLoggerClass;
    private DexClassLoader mDexClassLoader;


    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        try {
            shared = new XSharedPreferences("Xposed.Okhttp.Cat", "config");
            shared.reload();
            InvokPackage = shared.getString("APP_INFO", "");
            MODEL= shared.getString("MODEL", "1");
            //先重启 选择 好 要进行Hook的 app
            if (InvokPackage == null || InvokPackage.equals("")) {
                return;
            } else {
                if (lpparam.packageName.equals(InvokPackage)) {
                    CLogUtils.e("开始执行 HookAttach 对应的app是 " + InvokPackage);
                    HookAttach();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private int flag = 0;
    private int flag1 = 0;

    /**
     * hook  Attach方法
     */
    private void HookAttach() {
        XposedHelpers.findAndHookMethod(Application.class, "attach",
                Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        if (flag == 0) {
                            mOtherContext = (Context) param.args[0];
                            mLoader = mOtherContext.getClassLoader();
                            CLogUtils.e("拿到 classloader");
                            flag = 1;
                            HookOKClient();
                        }
                    }
                });
    }

    private void HookOKClient() {
        try {
            //OkHttpBuilder =  Class.forName(ClassPath);
            if (OkHttpBuilder == null) {

                OkHttpBuilder = Class.forName("okhttp3.OkHttpClient$Builder", true, mLoader);

                //OkHttpClient = Class.forName("okhttp3.OkHttpClient", true, mLoader);

               // OkHttpBuilder = Class.forName("okhttp3.u$a", true, mLoader);
            }
            if (OkHttpBuilder != null && flag1 == 0) {
                CLogUtils.e("拿到 Builder class ");
                invoke();
                flag1 = 1;
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            CLogUtils.e("出现异常 对方没有使用OkHttp或者被 混淆了" + e.getMessage());
        }
    }

    private void invoke() {
        CLogUtils.e("发现对方使用 okhttp  开始hook ");
        if(MODEL.equals("1")) {
            //a
            XposedHelpers.findAndHookMethod(OkHttpBuilder, "build", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    CLogUtils.e("Hook 到 build函数 ");
                    AddInterceptors(param);
                }
            });
        }else if(MODEL.equals("2")) {
            //尝试 Hook 构造
            XposedHelpers.findAndHookConstructor(OkHttpBuilder, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    CLogUtils.e("Hook 到 构造函数  ");
                    AddInterceptors(param);
                }
            });
        }
    }

    private void AddInterceptors(XC_MethodHook.MethodHookParam param) {
        try {

            //interceptors
            List interceptors = (List) XposedHelpers.getObjectField(param.thisObject, "interceptors");
            CLogUtils.e("拿到了 拦截器  集合 ");
            interceptors.add(getHttpLoggingInterceptor());

        } catch (Exception e) {
            CLogUtils.e("Hook到 build但出现 异常 " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Log.e("XposedInto===", (String) args[0]);
        return null;
    }


    private void getAllMethod(Class<?> bin) {
        Method[] methods = bin.getMethods();
        for (Method m : methods) {
            CLogUtils.e(m.getName());
        }
    }


    @NonNull
    private Object getHttpLoggingInterceptor() {

        try {
            //第一步 首先  判断 本身项目里 是否存在 拦截器
            mHttpLoggingInterceptor = Class.forName("okhttp3.logging.HttpLoggingInterceptor", true, mLoader);
            mLoggerClass = Class.forName("okhttp3.logging.HttpLoggingInterceptor$Logger", true, mLoader);

            if (mHttpLoggingInterceptor != null && mLoggerClass != null) {
                CLogUtils.e("拿到了 拦截器 log");
                return InitInterceptor(false);
            }

        } catch (ClassNotFoundException e) {
            CLogUtils.e("拦截器初始化出现异常  ClassNotFoundException  " + e.getMessage());
            CLogUtils.e("开始 动态加载 ");
            return  initLoggingInterceptor();

        } catch (InstantiationException e) {
            CLogUtils.e("拦截器初始化出现异常  InstantiationException  " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            CLogUtils.e("拦截器初始化出现异常  IllegalAccessException  " + e.getMessage());

            e.printStackTrace();
        } catch (InvocationTargetException e) {
            CLogUtils.e("拦截器初始化出现异常  InvocationTargetException  " + e.getMessage());

            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            CLogUtils.e("拦截器初始化出现异常  NoSuchMethodException  " + e.getMessage());

            e.printStackTrace();
        }
        return null;
    }

    private Object InitInterceptor(boolean isDexLoader) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
        CLogUtils.e("拿到  HttpLoggingInterceptor 和 logger ");
        //通过 动态代理 拿到 这个 接口 实体类
        Object logger;
        if(isDexLoader) {
            logger = Proxy.newProxyInstance(mDexClassLoader, new Class[]{mLoggerClass}, Hook.this);
        }else {
            logger = Proxy.newProxyInstance(mLoader, new Class[]{mLoggerClass}, Hook.this);
        }
        CLogUtils.e("拿到  动态代理的 class");
        Object loggingInterceptor = mHttpLoggingInterceptor.getConstructor(mLoggerClass).newInstance(logger);
        CLogUtils.e("拿到  拦截器的实体类  ");
        Object level;
        if(isDexLoader) {
            level  =mDexClassLoader.loadClass("okhttp3.logging.HttpLoggingInterceptor$Level").getEnumConstants()[3];
        }else {
            level  =mLoader.loadClass("okhttp3.logging.HttpLoggingInterceptor$Level").getEnumConstants()[3];
        }
        CLogUtils.e("拿到  Level 枚举   ");
        //调用 函数
        XposedHelpers.findMethodBestMatch(mHttpLoggingInterceptor, "setLevel", level.getClass()).invoke(loggingInterceptor, level);
        CLogUtils.e("设置成功  ");
        return loggingInterceptor;
    }

    /**
     * 用 classloader 加载 这个jar 包 防止 classloader 不统一
     */
    private Object initLoggingInterceptor() {
        File dexOutputDir = mOtherContext.getDir("dex", 0);

        CLogUtils.e("dexOutputDir  dex  666 " +dexOutputDir.getAbsolutePath() );
        // 定义DexClassLoader
        // 第一个参数：是dex压缩文件的路径
        // 第二个参数：是dex解压缩后存放的目录
        // 第三个参数：是C/C++依赖的本地库文件目录,可以为null
        // 第四个参数：是上一级的类加载器
        mDexClassLoader = new DexClassLoader(INTERCEPTORPATH, dexOutputDir.getAbsolutePath(), null, mLoader);
        try {
            mHttpLoggingInterceptor = mDexClassLoader.loadClass("okhttp3.logging.HttpLoggingInterceptor");
            mLoggerClass = mDexClassLoader.loadClass("okhttp3.logging.HttpLoggingInterceptor$Logger");
            if (mLoggerClass != null && mHttpLoggingInterceptor != null) {
                CLogUtils.e("动态 加载 classloader 成功 ");
                return InitInterceptor(true);
            }
        } catch (ClassNotFoundException e) {
            CLogUtils.e("动态 加载异常  ClassNotFoundException" + e.getMessage());
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            CLogUtils.e("动态 加载异常  NoSuchMethodException" + e.getMessage());
            e.printStackTrace();
        } catch (InstantiationException e) {
            CLogUtils.e("动态 加载异常  InstantiationException" + e.getMessage());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            CLogUtils.e("动态 加载异常  IllegalAccessException" + e.getMessage());
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            CLogUtils.e("动态 加载异常  InvocationTargetException" + e.getMessage());

            e.printStackTrace();
        }
        return null;

    }


}
