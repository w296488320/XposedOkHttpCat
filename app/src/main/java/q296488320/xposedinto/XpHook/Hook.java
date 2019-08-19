package q296488320.xposedinto.XpHook;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import q296488320.xposedinto.config.Key;
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
    public static volatile String MODEL = null;


    //别的进程的 context
    private Context mOtherContext;
    //别的进程的 mLoader
    private volatile ClassLoader mLoader = null;

    private Class<?> OkHttpBuilder = null;

    private Class<?> OkHttpClient = null;

    private String ClassPath = "okhttp3.OkHttpClient$Builder";
    //使用 共享 数据的 XSharedPreferences
    private XSharedPreferences shared;
    private Class<?> mHttpLoggingInterceptor;
    private Class<?> mHttpLoggingInterceptorLoggerClass;
    private DexClassLoader mDexClassLoader;

    /**
     * 函数 可能被调用 多次
     * 防止被添加多次 拦截器 加一个 flags
     */
    private volatile int flag = 0;
    private volatile int flag1 = 0;
    private volatile int flag2 = 0;
    private volatile int flag3 = 0;
    private volatile int flag4 = 0;


    private int interceptorsFlage2 = 0;


    private ArrayList<String> OkHttpLoggerList = new ArrayList();

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        try {
            shared = new XSharedPreferences("Xposed.Okhttp.Cat", "config");
            shared.reload();
            InvokPackage = shared.getString("APP_INFO", "");
            MODEL = shared.getString("MODEL", "1");
            //先重启 选择 好 要进行Hook的 app
            if (InvokPackage == null || InvokPackage.equals("")) {
                return;
            } else {
                if (lpparam.packageName.equals(InvokPackage)) {
                    if (flag4 == 0) {
                        flag4 = 1;
                        CLogUtils.e("开始执行 HookAttach 对应的app是 " + InvokPackage);
                        HookAttach();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


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
                            flag = 1;
                            mOtherContext = (Context) param.args[0];
                            mLoader = mOtherContext.getClassLoader();
                            CLogUtils.e("拿到 classloader");
                            HookOKClient();
                        }
                    }
                });
    }


    /**
     * 判断 OkHttp是否存在 混淆 这步骤
     */
    private void HookOKClient() {
        try {
            //OkHttpBuilder =  Class.forName(ClassPath);
            if (OkHttpBuilder == null && flag3 == 0) {
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
            if (flag2 == 0) {
                flag2 = 1;
                CLogUtils.e("出现异常 对方没有使用OkHttp或者被 混淆了  开始尝试自动 获取 路径 " + e.getMessage());
                getClientClass();
            }
        }
    }


    private void getClientClass() {

        List<String> classNameList = getClassName(mOtherContext.getPackageName());


        List<Class> classList = new ArrayList<>();

        try {
            for (String path : classNameList) {
                //首先进行初始化
                classList.add(Class.forName(path, true, mLoader));
            }

            CLogUtils.e("初始化  跟 OkHttp类的个数 " + classList.size());

            //classList.add(Class.forName("okhttp3.w", true, mLoader));

            for (Class mClient : classList) {
                //判断 集合 个数 先拿到 四个集合 可以 拿到 Client
                if (isClient(mClient)) {
                    OkHttpClient = mClient;
                    CLogUtils.e("找到了 外层  开始 找内层 ");
                    getInterceptorList(classList);
                    return;
                }
            }
        } catch (ClassNotFoundException e) {
            CLogUtils.e("报错 路径实例化类  " + e.getMessage());
            e.printStackTrace();
        }

    }


    /**
     * 混淆 以后 获取  集合并添加 拦截器的方法
     *
     * @param classList
     */
    private void getInterceptorList(List<Class> classList) {
        if (OkHttpClient == null) {
            CLogUtils.e("混淆以后  OkHttpClient==null ");
        } else {
            //开始查找 build
            for (Class ccc : classList) {
                if (isBuilder(ccc)) {
                    OkHttpBuilder = ccc;
                    invoke2();
                }
            }

        }
    }

    private void invoke2() {
        //混淆 以后 只能 Hook 构造 目的 是为了 拿到 object

        //尝试 Hook 构造
        XposedHelpers.findAndHookConstructor(OkHttpBuilder, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                CLogUtils.e("Hook 到 构造函数  ");
                AddInterceptors2(param);
            }
        });
    }

    private ArrayList<Field> mFieldArrayList = new ArrayList<>();

    private boolean isBuilder(@NonNull Class ccc) {

        int ListTypeCount = 0;
        int FinalTypeCount = 0;
        Field[] fields = ccc.getDeclaredFields();
        List<Field> List = new ArrayList<>();
        for (Field field : fields) {
            String type = field.getType().getName();
            //四个 集合
            if (type.contains(Key.ListType)) {
                ListTypeCount++;
            }
            //2 个 为 final类型
            if (type.contains(Key.ListType) && Modifier.isFinal(field.getModifiers())) {
                List.add(field);
                FinalTypeCount++;
            }
        }
        //四个 List 两个 2 final  并且 包含父类名字
        if (ListTypeCount == 4 && FinalTypeCount == 2 && ccc.getName().contains(OkHttpClient.getName())) {
            CLogUtils.e(" 找到 Builer  " + ccc.getName());
            mFieldArrayList.addAll(List);
            return true;
        }
        return false;
    }

    private boolean isClient(@NonNull Class<?> mClass) {
        int typeCount = 0;
        //getDeclaredFields 是个 获取 全部的
        Field[] fields = mClass.getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            String type = field.getType().getName();

            //CLogUtils.e(" 复合 规则 该 Field是      " + type  +"是否包含      "+ type.contains(Key.ListType) +"   是否是 final类型  "+Modifier.isFinal(field.getModifiers()));

            //四个 集合 四个final 特征
            if (type.contains(Key.ListType) && Modifier.isFinal(field.getModifiers())) {
                //CLogUtils.e(" 复合 规则 该 Field是      " + field.getName() + " ");
                typeCount++;
            }
        }
        //CLogUtils.e("field 符合标准   个数 是    " + typeCount);

        if (typeCount == 6) {
            CLogUtils.e(" 该类的名字是  " + mClass.getName());
            return true;
        }
        // mFieldArrayList.clear();
        return false;
    }


    private List<String> getClassName(String packageName) {
        OkHttpLoggerList.clear();
        List<String> classNameList = new ArrayList<String>();
        try {
            //通过DexFile查找当前的APK中可执行文件
            // CLogUtils.e("this.getPackageCodePath()  "+this.getPackageCodePath());

            DexFile df = new DexFile(mOtherContext.getPackageCodePath());

            //获取df中的元素  这里包含了所有可执行的类名 该类名包含了包名+类名的方式
            Enumeration<String> enumeration = df.entries();
            while (enumeration.hasMoreElements()) {//遍历
                String className = (String) enumeration.nextElement();
                if (className.contains("okhttp3")) {//在当前所有可执行的类里面查找包含有该包名的所有类
                    classNameList.add(className);
                    if (className.contains("okhttp3.logging")) {
                        OkHttpLoggerList.add(className);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return classNameList;
    }

    private void invoke() {
        CLogUtils.e("发现对方使用 okhttp  开始hook ");
        if (MODEL.equals("1")) {
            //build
            XposedHelpers.findAndHookMethod(OkHttpBuilder, "build", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    CLogUtils.e("Hook 到 build函数 ");
                    AddInterceptors(param);
                }
            });
        } else if (MODEL.equals("2")) {
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


    /**
     * 函数 可能被调用 多次
     * 防止被添加多次 拦截器 加一个 flags
     */
    private int interceptorsFlage = 0;

    private void AddInterceptors(XC_MethodHook.MethodHookParam param) {
        try {
            if (interceptorsFlage == 0) {
                //interceptors
                List interceptors = (List) XposedHelpers.getObjectField(param.thisObject, "interceptors");
                CLogUtils.e("拿到了 拦截器  集合 ");
                Object httpLoggingInterceptor = getHttpLoggingInterceptor();
                interceptors.add(httpLoggingInterceptor);
                interceptorsFlage = 1;
            }
        } catch (Exception e) {
            CLogUtils.e("Hook到 build但出现 异常 " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void AddInterceptors2(XC_MethodHook.MethodHookParam param) {
        try {
            if (interceptorsFlage2 == 0) {
                if (mFieldArrayList.size() == 2) {
                    //CLogUtils.e("集合 个数是     "+ mFieldArrayList.size() + "   "+mFieldArrayList.get(0).getName() );
                    //随便 拿一个 添加 一个 log拦截器 进去
                    List interceptors = (List) XposedHelpers.getObjectField(param.thisObject, mFieldArrayList.get(0).getName());
                    CLogUtils.e("拿到 集合  ");
                    Object httpLoggingInterceptor = getHttpLoggingInterceptor();
                    if (httpLoggingInterceptor != null) {
                        CLogUtils.e("拿到 拦截器 实例  ");
                        interceptors.add(httpLoggingInterceptor);
                    }


                } else {
                    CLogUtils.e("mFieldArrayList 的 集合 不是 2  ");
                }

                interceptorsFlage2 = 1;
            }
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
            //okhttp3.logging.HttpLoggingInterceptor
            mHttpLoggingInterceptor = Class.forName("okhttp3.logging.HttpLoggingInterceptor", true, mLoader);
            //okhttp3.logging.HttpLoggingInterceptor$Logger
            mHttpLoggingInterceptorLoggerClass = Class.forName("okhttp3.logging.HttpLoggingInterceptor$Logger", true, mLoader);

            if (mHttpLoggingInterceptor != null && mHttpLoggingInterceptorLoggerClass != null) {
                CLogUtils.e("拿到了 拦截器 log");
                return InitInterceptor(false);
            }

        } catch (ClassNotFoundException e) {
            CLogUtils.e("拦截器初始化出现异常  ClassNotFoundException  " + e.getMessage());
            if (mFieldArrayList.size() == 2) {
                //混淆 无法 动态 加载

//                HookGetOutPushStream();

                getOkHttpLogging();
            }
            CLogUtils.e("开始 动态 加载 初始化 ");
            // 没有 混淆 可以动态 加载
            return initLoggingInterceptor();

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

    private void getOkHttpLogging() {
        if (OkHttpLoggerList.size() != 0) {
            try {
                //ArrayList<Class> objects = new ArrayList<>();
                for (String stringName : OkHttpLoggerList) {
                    //objects.add(Class.forName(stringName, true, mLoader));
                    Class<?> aClass = Class.forName(stringName, true, mLoader);
                    if(isHttpLoggingInterceptor(aClass)){
                        mHttpLoggingInterceptor=aClass;
                        //拿到 里面的接口类 名字
                        CLogUtils.e("找到了  Httplogger ");
                        initLoggingInterceptor2(aClass);
                        return;
                    }
                }
                //全部的类都没有找到复合相关的 就开始 Hook底层 函数
                HookGetOutPushStream();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            HookGetOutPushStream();
        }
    }

    private void initLoggingInterceptor2(Class<?> aClass) {
        try {
            CLogUtils.e("开始查找 logger ");
            for (String stringName : OkHttpLoggerList) {
                Class<?> stringNameClass = Class.forName(stringName, true, mLoader);
                //包含外部类 并且 是 接口
                if(stringName.contains(aClass.getName()+"$")&&stringNameClass.isInterface()){
                    mHttpLoggingInterceptorLoggerClass=stringNameClass;
                }
            }
            if(mHttpLoggingInterceptor != null){
                InitInterceptor2();
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }



    private boolean isHttpLoggingInterceptor(Class<?> aClass) {
        int CharsetCount = 0;
        int FinalCount = 0;
        Field[] declaredFields = aClass.getDeclaredFields();
        for (int i = 0; i < declaredFields.length; i++) {
            String type = declaredFields[i].getType().getName();
            //Object o = declaredFields[i].get(aClass);
            if (type.contains("java.nio.charset.Charset") && Modifier.isFinal(declaredFields[i].getModifiers())) {
                CharsetCount++;
            }
            if(Modifier.isFinal(declaredFields[i].getModifiers())){
                FinalCount++;
            }
        }
        if(FinalCount==2&&CharsetCount==1&&declaredFields.length>=3){
            return true;
        }
        return false;
    }

    private void HookGetOutPushStream() {
        CLogUtils.e("无法 找到 HttpLoggingInterceptor  和 HttpLoggingInterceptor$Logger  通过Hook底层 实现 ");

    }

    private void InitInterceptor2() {
        try {
            Object logger = Proxy.newProxyInstance(mLoader, new Class[]{mHttpLoggingInterceptorLoggerClass}, Hook.this);

            CLogUtils.e("拿到  动态代理的 class");
            Object loggingInterceptor = mHttpLoggingInterceptor.getConstructor(mHttpLoggingInterceptorLoggerClass).newInstance(logger);


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


    }



    private Object InitInterceptor(boolean isDexLoader) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
        CLogUtils.e("拿到  HttpLoggingInterceptor 和 logger ");
        //通过 动态代理 拿到 这个 接口 实体类
        Object logger;
        if (isDexLoader) {
            logger = Proxy.newProxyInstance(mDexClassLoader, new Class[]{mHttpLoggingInterceptorLoggerClass}, Hook.this);
        } else {
            logger = Proxy.newProxyInstance(mLoader, new Class[]{mHttpLoggingInterceptorLoggerClass}, Hook.this);
        }
        CLogUtils.e("拿到  动态代理的 class");
        Object loggingInterceptor = mHttpLoggingInterceptor.getConstructor(mHttpLoggingInterceptorLoggerClass).newInstance(logger);
        CLogUtils.e("拿到  拦截器的实体类  ");
        Object level;
        if (isDexLoader) {
            level = mDexClassLoader.loadClass("okhttp3.logging.HttpLoggingInterceptor$Level").getEnumConstants()[3];
        } else {
            level = mLoader.loadClass("okhttp3.logging.HttpLoggingInterceptor$Level").getEnumConstants()[3];
        }
        CLogUtils.e("拿到  Level 枚举   ");
        //调用 函数
        //setLevel
        XposedHelpers.findMethodBestMatch(mHttpLoggingInterceptor, "setLevel", level.getClass()).invoke(loggingInterceptor, level);
        CLogUtils.e("设置成功  ");
        return loggingInterceptor;
    }

    /**
     * 用 classloader 加载 这个jar 包 防止 classloader 不统一
     */
    private Object initLoggingInterceptor() {


        File dexOutputDir = mOtherContext.getDir("dex", 0);

        CLogUtils.e("dexOutputDir  dex  666 " + dexOutputDir.getAbsolutePath());
        // 定义DexClassLoader
        // 第一个参数：是dex压缩文件的路径
        // 第二个参数：是dex解压缩后存放的目录
        // 第三个参数：是C/C++依赖的本地库文件目录,可以为null
        // 第四个参数：是上一级的类加载器
        mDexClassLoader = new DexClassLoader(INTERCEPTORPATH, dexOutputDir.getAbsolutePath(), null, mLoader);
        try {
            mHttpLoggingInterceptor = mDexClassLoader.loadClass("okhttp3.logging.HttpLoggingInterceptor");
            mHttpLoggingInterceptorLoggerClass = mDexClassLoader.loadClass("okhttp3.logging.HttpLoggingInterceptor$Logger");
            if (mHttpLoggingInterceptorLoggerClass != null && mHttpLoggingInterceptor != null) {
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
