package zhenxi.xposedinto.XpHook;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import zhenxi.xposedinto.LogImp.LogInterceptorImp;
import zhenxi.xposedinto.utils.CLogUtils;
//import org.jf.dexlib2.DexFileFactory;
//import org.jf.dexlib2.Opcodes;
//import org.jf.dexlib2.dexbacked.DexBackedClassDef;
//import org.jf.dexlib2.dexbacked.DexBackedDexFile;
//import org.jf.dexlib2.dexbacked.DexBackedField;


/**
 * Created by lyh on 2019/6/18.
 */

public class Hook implements IXposedHookLoadPackage, InvocationHandler {

    //每个线程独立的InputStreamWrapper
    private static final ThreadLocal<InputStreamWrapper> CACHE = new ThreadLocal<>();
    /**
     * 进行 注入的 app 名字
     */
    public static volatile String InvokPackage = null;
    //存放 全部类的 集合
    public static ArrayList<Class> mClassList = new ArrayList<>();
    /**
     * HTTP请求相关类和方法
     */
    private static String httpUrlConnClass = "com.android.okhttp.internal.huc.HttpURLConnectionImpl";
    private static String httpUrlConnGetInputStreamMethod = "getInputStream";
    private static String httpUrlConnGetUrlMethod = "getURL";
    private static String httpUrlConnGetContentEncodingMethod = "getContentEncoding";
    private static String httpUrlConngetHeaderFieldsMethod = "getHeaderFields";
    private static String httpUrlConngetGetResponseMethod = "getResponse";
    private static boolean flag = true;
    /**
     * 已经初始化过的拦截器实例
     */
    private static Object httpLoggingInterceptor = null;
    private final SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH时:mm分:ss秒");
    //存放 全部类名字的 集合
    private final List<String> AllClassNameList = new ArrayList<>();
    int getHttpLoggingInterceptorFlag = 0;
    Class interceptorClass;
    private String INTERCEPTORPATH = "/storage/emulated/0/OkHttpCat/bbb.dex";
    private String OUTRPATH = "/storage/emulated/0/OkHttpCat/Out";
    /**
     * 模式 hook 不同的 构造 函数
     */
//    public static volatile String MODEL = null;


    //别的进程的 context
    private Context mOtherContext;
    //别的进程的 mLoader
    private ClassLoader mLoader = null;
    private String ClassPath = "okhttp3.OkHttpClient$Builder";
    //使用 共享 数据的 XSharedPreferences
    private XSharedPreferences shared;
    //OkHttp里面的 类
    private Class<?> OkHttpBuilder = null;
    private Class<?> OkHttpClient = null;
    //拦截器里面的类
    private Class<?> mHttpLoggingInterceptorClass;
    private Class<?> mHttpLoggingInterceptorLoggerClass;
    private Class<?> mHttpLoggingInterceptorLoggerEnum;
    private DexClassLoader mDexClassLoader;
    private Class<?> SocketInputStreamClass;
    /**
     * 是否打印 栈信息的 开关
     */
    private boolean isShowStacktTrash = false;
    //存放 跟 Logger有关系的 全部的类
    private ArrayList<String> OkHttpLoggerList = new ArrayList();
    //    private void processDex(File file) {
//        try {
//
//            DexBackedDexFile dexFile = DexFileFactory.loadDexFile(file, Opcodes.getDefault());
//
//            for (DexBackedClassDef classDef : dexFile.getClasses()) {
//                int typeCount = 0;
//                int staticCount = 0;
//
//                String name = classDef.getType();
//                name = name.substring(1, name.length() - 1).replace('/', '.');
//
//                if (name.startsWith("android")
//                        || name.startsWith("java")
//                        || name.startsWith("kotlin")
//                        || name.startsWith("com")
//                        || name.startsWith("org")
//                        || name.startsWith("retrofit")
//                        || name.startsWith("butterknife")
//                        || name.startsWith("io.reactivex")
//                ) continue;
//
//                for (DexBackedField field : classDef.getFields()) {
//                    // 判断是否final
//                    if (!AccessFlags.FINAL.isSet(field.getAccessFlags()))
//                        continue;
//                    // 判断类型是否List
//                    if (!field.getType().equals("Ljava/util/List;"))
//                        continue;
//                    typeCount++;
//                    if (AccessFlags.STATIC.isSet(field.getAccessFlags()))
//                        staticCount++;
//                }
//                if (typeCount == 6 && staticCount == 2) {
//                    log(name);
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
    //存放 这个 app全部的 classloader
    private ArrayList<ClassLoader> AppAllCLassLoaderList = new ArrayList<>();
    private Object mProxyInstance;


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
//        CLogUtils.e("调用方法名字 "+method.getName());
//        HttpLoggingInterceptor httpLoggingInterceptor =new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
//            @Override
//            public void log(String message) {
//
//            }
//        });
        CLogUtils.NetLogger((String) args[0]);

        return null;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            shared = new XSharedPreferences("Xposed.Okhttp.Cat", "config");
            shared.reload();

            InvokPackage = shared.getString("APP_INFO", "");

            //MODEL = shared.getString("MODEL", "1");

//            if (MODEL.equals("4")) {
//                isShowStacktTrash = true;
//            }

            //先重启 选择 好 要进行Hook的 app
            if (!"".equals(InvokPackage) && lpparam.packageName.equals(InvokPackage)) {
                HookLoadClass();
                HookAttach();
            }
        } catch (Throwable e) {
            e.printStackTrace();
            CLogUtils.e("Test  Exception  " + e.toString());
        }

    }

    private void HookLoadClass() {
        XposedBridge.hookAllMethods(ClassLoader.class,
                "loadClass",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        if (param == null) {
                            return;
                        }
                        Class cls = (Class) param.getResult();
                        //在这块 做个判断 是否 存在 如果 不存在 在保存  否则 影响效率
                        //如果 不存在
                        if (cls == null) {
                            return;
                        }

                        isNeedAddClassloader(cls.getClassLoader());
                    }
                });
    }

    /**
     * 对 每个 classloader进行判断
     *
     * @param classLoader 需要 的classloader
     * @return
     */
    private void isNeedAddClassloader(ClassLoader classLoader) {

        //先过滤掉系统类 ,如果是系统预装的 类也 不要
//        if (classLoader.getClass().getName().equals("java.lang.BootClassLoader")) {
//            return;
//        }
        for (ClassLoader loader : AppAllCLassLoaderList) {
            if (loader.hashCode() == classLoader.hashCode()) {
                return;
            }
        }
        //CLogUtils.e("加入的classloader名字  " + classLoader.getClass().getName());
        AppAllCLassLoaderList.add(classLoader);
    }

    /**
     * 手动申请SD卡权限
     */
    private void initPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (mOtherContext.checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE")
                    != PackageManager.PERMISSION_GRANTED) {
                XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        final Activity firstActivity = (Activity) param.thisObject;

                        ActivityCompat.requestPermissions(firstActivity, new String[]{
                                "android.permission.WRITE_EXTERNAL_STORAGE",
                                "android.permission.ACCESS_COARSE_LOCATION",
                                "android.permission.ACCESS_FINE_LOCATION",
                                "android.permission.READ_PHONE_STATE"}, 0);
                    }
                });
            }
        }
    }

    /**
     * 遍历当前进程的Classloader 尝试进行获取指定类
     *
     * @param className
     * @return
     */
    private Class getClass(String className) {
        Class<?> aClass = null;
        try {
            try {
                aClass = Class.forName(className);
            } catch (ClassNotFoundException classNotFoundE) {

                try {
                    aClass = Class.forName(className, false, mLoader);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                if (aClass != null) {
                    return aClass;
                }
                try {
                    for (ClassLoader classLoader : AppAllCLassLoaderList) {
                        try {
                            aClass = Class.forName(className, false, classLoader);
                        } catch (Throwable e) {
                            continue;
                        }
                        if (aClass != null) {
                            return aClass;
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }

            return aClass;
        } catch (Throwable e) {

        }
        return null;
    }

    /**
     * hook  Attach方法
     */
    private void HookAttach() {

//
//        Class OkHttpClientClass = getClass("com.squareup.okhttp.OkHttpClient");
//
//        if(OkHttpClientClass!=null){
//            CLogUtils.e("成功拿到 系统okHttpClient");
//        }


        XposedHelpers.findAndHookMethod(Application.class,
                "attach",
                Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        super.beforeHookedMethod(param);
                        mOtherContext = (Context) param.args[0];
                        mLoader = mOtherContext.getClassLoader();
                        CLogUtils.e("拿到Classloader ");
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        String processName = getCurProcessName(mOtherContext);
                        if (processName != null && processName.equals(mOtherContext.getPackageName())) {
                            HookOKClient();
                        }
                    }
                });


//        XposedHelpers.findAndHookMethod(Application.class,
//                "attachBaseContext",
//                Context.class,
//                new XC_MethodHook() {
//                    @Override
//                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                        super.beforeHookedMethod(param);
//
//                    }
//
//                    @Override
//                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                        super.afterHookedMethod(param);
//
//                    }
//                });

//        XposedHelpers.findAndHookMethod(Application.class, "onCreate",
//                new XC_MethodHook() {
//                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                        super.afterHookedMethod(param);
//
//                    }
//                });
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

    /**
     * 判断 OkHttp是否存在 混淆 这步骤
     */
    private synchronized void HookOKClient() {
        try {
            if (OkHttpClient == null) {
                OkHttpClient = Class.forName("okhttp3.OkHttpClient", false, mLoader);
            }
        } catch (ClassNotFoundException e) {
            CLogUtils.e("出现异常 发现对方没有使用OkHttp或者被混淆,开始尝试自动获取路径 ");
            HookAndInitProguardClass();
            return;
        }
        try {
            if (OkHttpBuilder == null) {
                OkHttpBuilder = Class.forName("okhttp3.OkHttpClient$Builder", false, mLoader);
            }
        } catch (ClassNotFoundException e) {
            CLogUtils.e("出现异常 发现对方没有使用OkHttp或者被混淆,开始尝试自动获取路径 ");
            HookAndInitProguardClass();
            return;
        }
        if (isExactness()) {
            CLogUtils.e("okHttp本身未混淆");
            HookClientAndBuilderConstructor();
        } else {
            HookAndInitProguardClass();
        }
    }

    private synchronized void HookAndInitProguardClass() {
        //放在子线程去执行，防止卡死
        //先拿到 app里面全部的

        getAllClassName();

        initAllClass();


        //第一步 先开始 拿到 OkHttp 里面的 类  如Client 和 Builder
        getClientClass();
        getBuilder();

        if (OkHttpBuilder != null && OkHttpClient != null) {
            CLogUtils.e("使用了okHttp 开始添加拦截器");
            HookClientAndBuilderConstructor();
        } else {
            CLogUtils.e("对方App可能没有使用okHttp 开始Hook底层方法");
            //可能对方没有使用OkHttp
            HookGetOutPushStream();
        }


    }

    /**
     * 添加IO目录
     * <p>
     * 这个方法 主要是 有的 io目录 被混淆了 导致后面的 拦截器方法  被 干掉了
     * 需要先 找到 okio的 路径
     * 通过  FormBodyz这个方法 进行 参数1 拿到 路径
     * public void writeTo(BufferedSink sink) throws IOException
     * <p>
     * 比如 okio 目录被混淆成 abc这样的路径 我没做识别
     * 没有加到 mClassList 这个里面也就导致后面的程序 会 找不到 抛异常
     * 为了保险 在此添加一次 跟 Okio有关的类
     */
    private synchronized void getIOPath() {

        for (Class mClass : mClassList) {
            int ListCount = 0;
            int staticCount = 0;
            if (Modifier.isFinal(mClass.getModifiers())) {
                Field[] declaredFields = mClass.getDeclaredFields();
                if (declaredFields.length == 3) {
                    for (Field field : declaredFields) {
                        if (field.getType().getName().equals(List.class.getName()) &&
                                Modifier.isFinal(field.getModifiers()) &&
                                Modifier.isPrivate(field.getModifiers())
                        ) {
                            ListCount++;
                        }
                        if (Modifier.isFinal(field.getModifiers())
                                && Modifier.isPrivate(field.getModifiers()) &&
                                Modifier.isStatic(field.getModifiers())
                        ) {
                            staticCount++;
                        }
                    }
                    if (ListCount == 2 && staticCount == 1) {
                        String ioPath = getWriterToParameterTypeString(mClass);
                        if (ioPath.equals("okio")) {
                            CLogUtils.e("IO目录名字 是okio 直接return ");
                            return;
                        } else {
                            CLogUtils.e("IO目录名字 " + ioPath);
                            //将 OKIO 目录 加进去
                            for (String string : AllClassNameList) {
                                if (string.startsWith(ioPath)) {
                                    Class<?> aClass = null;
                                    try {
                                        aClass = getClass(string);
                                    } catch (Throwable e) {
                                        e.printStackTrace();
                                    }
                                    if (aClass != null) {
                                        mClassList.add(aClass);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private String getWriterToParameterTypeString(Class mClass) {
        Method[] declaredMethods = mClass.getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (method.getReturnType().getName().equals(void.class.getName())
                    && Modifier.isPublic(method.getModifiers())
                    && method.getParameterTypes().length == 1
            ) {
                //String str="jjj.kkk.yyy.uuu";
                String parameterType = method.getParameterTypes()[0].getName();
                String[] split = parameterType.split("\\.");
                StringBuilder stringBuffer = new StringBuilder();
                for (int i = 0; i < split.length - 1; i++) {
                    stringBuffer.append(split[i]).append(".");
                }
                return stringBuffer.toString();
            }
        }
        return "";
    }

    /**
     * 初始化 需要的 class的 方法
     */
    private void initAllClass() {
        mClassList.clear();

        try {
            CLogUtils.e("需要初始化Class的个数是  " + AllClassNameList.size());

            Class<?> MClass = null;

            for (int i = 0; i < AllClassNameList.size(); i++) {

                MClass = getClass(AllClassNameList.get(i));
                if (MClass != null) {
                    //CLogUtils.e("添加成功 "+MClass.getName());
                    mClassList.add(MClass);
                }

            }

            CLogUtils.e("初始化全部类的个数   " + mClassList.size());
        } catch (Throwable e) {
            CLogUtils.e("initAllClass error " + e.toString());
        }
    }

    /**
     * 获取 ClientCLass的方法
     */
    private void getClientClass() {
        if (mClassList.size() == 0) {
            CLogUtils.e("全部的 集合 mClassList  的个数 为 0  ");
            return;
        }
        CLogUtils.e("开始 查找   ClientClass");
        try {
            for (Class mClient : mClassList) {
                //判断 集合 个数 先拿到 四个集合 可以 拿到 Client
                if (isClient(mClient)) {
                    OkHttpClient = mClient;
                    return;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        CLogUtils.e("没找到 client ");
    }

    /**
     * 混淆 以后 获取  集合并添加 拦截器的方法
     */
    private void getBuilder() {
        if (OkHttpClient != null) {
            //开始查找 build
            for (Class builder : mClassList) {
                if (isBuilder(builder)) {
                    OkHttpBuilder = builder;
                }
            }
        }
    }

    /**
     * 当 OkHttpBuilderClass 和OkHttpClient不为Null
     * Hook Client和 Builder 的构造
     * <p>
     * 如果两个都没找到可能没有使用okHttp
     * 尝试Hook 底层函数
     */
    private void HookClientAndBuilderConstructor() {

        try {
            interceptorClass = getInterceptorClass();
            if (interceptorClass == null) {
                CLogUtils.e("HookClientAndBuilderConstructor 出现问题没有拿到解释器类 ");
                return;
            }
        } catch (Throwable e) {
            CLogUtils.e("getInterceptorClass  error " + e.toString());
        }


        if (OkHttpClient != null) {
            try {
                XposedHelpers.findAndHookConstructor(
                        OkHttpClient,
                        OkHttpBuilder,
                        new XC_MethodHook() {
//                            @Override
//                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                                super.afterHookedMethod(param);

//                            }

                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                super.beforeHookedMethod(param);
                                CLogUtils.e("Hook 到 构造函数  OkHttpClient");
                                AddInterceptors2(param);
                            }
                        });
            } catch (Throwable e) {
                CLogUtils.e("findAndHookConstructor OkHttpClient error " + e.toString());
                e.printStackTrace();
            }
        }

    }

    private Method getAddInterceptorMethod(Class<?> okHttpBuilder) {
        Method[] declaredMethods = okHttpBuilder.getDeclaredMethods();

        Class interceptorClass = getInterceptorClass();
        if (interceptorClass != null) {
            for (Method method : declaredMethods) {
                if (method.getParameterTypes().length == 1 && method.getParameterTypes()[0].getName().equals(interceptorClass.getName())) {
                    method.setAccessible(true);
                    return method;
                }
            }
        }
        CLogUtils.e("没找到AddInterceptorMethod");
        return null;
    }

    private boolean isBuilder(@NonNull Class ccc) {

        try {
            int ListTypeCount = 0;
            int FinalTypeCount = 0;
            Field[] fields = ccc.getDeclaredFields();
            for (Field field : fields) {
                String type = field.getType().getName();
                //四个 集合
                if (type.contains(List.class.getName())) {
                    ListTypeCount++;
                }
                //2 个 为 final类型
                if (type.contains(List.class.getName()) && Modifier.isFinal(field.getModifiers())) {
                    FinalTypeCount++;
                }
            }
            //四个 List 两个 2 final  并且 包含父类名字
            if (ListTypeCount == 4 && FinalTypeCount == 2 && ccc.getName().contains(OkHttpClient.getName())) {
                CLogUtils.e(" 找到 Builer  " + ccc.getName());
                return true;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean isClient(@NonNull Class<?> mClass) {
        try {
            int typeCount = 0;
            int StaticCount = 0;

            //getDeclaredFields 是个 获取 全部的
            Field[] fields = mClass.getDeclaredFields();

            for (Field field : fields) {
                field.setAccessible(true);
                String type = field.getType().getName();

                //CLogUtils.e(" 复合 规则 该 Field是      " + type  +"是否包含      "+ type.contains(Key.ListType) +"   是否是 final类型  "+Modifier.isFinal(field.getModifiers()));

                //四个 集合 四个final 特征
                if (type.contains(List.class.getName()) && Modifier.isFinal(field.getModifiers())) {
                    //CLogUtils.e(" 复合 规则 该 Field是      " + field.getName() + " ");
                    typeCount++;
                }
                if (type.contains(List.class.getName()) && Modifier.isFinal(field.getModifiers()) && Modifier.isStatic(field.getModifiers())) {
                    //CLogUtils.e(" 复合 规则 该 Field是      " + field.getName() + " ");
                    StaticCount++;
                }
            }
            //CLogUtils.e("field 符合标准   个数 是    " + typeCount);

            if (StaticCount >= 2 && typeCount == 6 && mClass.getInterfaces().length >= 1) {
                CLogUtils.e("找到OkHttpClient  该类的名字是  " + mClass.getName());
                return true;
            }
        } catch (Throwable e) {
            CLogUtils.e("isClient error " + e.toString());
            e.printStackTrace();
        }
        // mFieldArrayList.clear();
        return false;
    }

    private void getDexFileClassName(DexFile dexFile) {
        if (dexFile == null) {
            return;
        }

        //获取df中的元素  这里包含了所有可执行的类名 该类名包含了包名+类名的方式
        Enumeration<String> enumeration = dexFile.entries();
        while (enumeration.hasMoreElements()) {//遍历
            String className = enumeration.nextElement();
            //添加过滤信息
            if (className.contains("okhttp") || className.contains("okio")
            ) {
                AllClassNameList.add(className);
            }
        }
    }

    /**
     * 核心方法
     * <p>
     * 尝试添加拦截器
     *
     * @param param
     */
    private synchronized void AddInterceptors2(XC_MethodHook.MethodHookParam param) {
        if (interceptorClass == null) {
            CLogUtils.e("interceptorClass == null");
            return;
        }
        try {
//            if (flag) {
            //找到添加拦截器的方法
            Object httpLoggingInterceptor = getHttpLoggingInterceptorClass();

            if (httpLoggingInterceptor != null) {
                CLogUtils.e("拿到的拦截器,动态代理实现，开始添加名字是 " + httpLoggingInterceptor.getClass().getName());
                Hook.httpLoggingInterceptor = httpLoggingInterceptor;
                //添加拦截器到集合里面
                if (AddInterceptorForList(param, httpLoggingInterceptor)) {
                    //将状态修改为False
                    flag = false;
                    //CLogUtils.e("添加拦截器完毕");
                }
            } else {
                CLogUtils.e("添加拦截器失败 getHttpLoggingInterceptorClass==null");
            }
//            }
        } catch (Throwable e) {
            CLogUtils.e("AddInterceptors2 异常 " + e.toString());
        }
    }


    private boolean AddInterceptorForList(XC_MethodHook.MethodHookParam param, Object httpLoggingInterceptor)
            throws IllegalAccessException {
        //参数1 是 interceptors 可以修改
        try {
            Object object;
            if (param.args == null || param.args.length == 0) {
                object = param.thisObject;
                CLogUtils.e("object=param.thisObject");
            } else {
                object = param.args[0];
                CLogUtils.e("object=param.args[0] " + param.args.length);
            }
            for (Field field : object.getClass().getDeclaredFields()) {
                if (field.getType().getName().equals(List.class.getName())) {
                    Type genericType = field.getGenericType();
                    if (null != genericType) {
                        ParameterizedType pt = (ParameterizedType) genericType;
                        // 得到泛型里的class类型对象
                        Class<?> actualTypeArgument = (Class<?>) pt.getActualTypeArguments()[0];
                        if (actualTypeArgument.getName().equals(interceptorClass.getName())) {
                            field.setAccessible(true);
                            List list;
                            if (param.args.length == 0) {
                                list = (List) field.get(param.thisObject);
                            } else {
                                list = (List) field.get(param.args[0]);
                            }
//                            if (isNeedAddLogging(list, httpLoggingInterceptor)) {
                            list.add(httpLoggingInterceptor);
                            CLogUtils.e("添加拦截器成功");
//                            }
                            return true;
                        }
                    }
                }
            }
        } catch (Throwable e) {
            CLogUtils.e("AddInterceptorForList error  " + e.getMessage());
            CLogUtils.e("AddInterceptorForList error  " + Log.getStackTraceString(e));
        }
        return false;
    }

    private boolean isNeedAddLogging(List list, Object httpLoggingInterceptor) {
        CLogUtils.e("当前拦截器集合的个数 " + list.size());
        CLogUtils.e("当前要添加拦截器的HashCode  " + httpLoggingInterceptor.hashCode());

        for (int i = 0; i < list.size(); i++) {
            CLogUtils.e("集合内部HashCode  " + list.get(i).hashCode());

            if (list.get(i).hashCode() == httpLoggingInterceptor.hashCode()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 自实现 loggerInterceptor 类
     */
    private Object getHttpLoggingInterceptorImp() {

        if (mProxyInstance != null) {
            return mProxyInstance;
        }
        try {
            if (interceptorClass != null) {
                CLogUtils.e("在init函数执行之前，拦截器Class名字  " + interceptorClass.getName());
                if (LogInterceptorImp.init(mClassList, interceptorClass, mLoader)) {
                    LogInterceptorImp logInterceptorImp = new LogInterceptorImp();
                    Object proxyInstance = Proxy.newProxyInstance(mLoader, new Class[]{interceptorClass}, logInterceptorImp);
                    mProxyInstance = proxyInstance;
                    return proxyInstance;
                } else {
                    //动态初始化拦截器失败
                    CLogUtils.e("动态初始化My拦截器失败");
                }
                return null;
            } else {
                CLogUtils.e("getInterceptorClass 返回 Null");
                return null;
            }
        } catch (Throwable e) {
            CLogUtils.e("getHttpLoggingInterceptorImp error " + e.toString());
            e.printStackTrace();
        }
        return null;
    }

    private Class getInterceptorClass() {
        try {
            Class aClass = getClass("okhttp3.Interceptor");
            if (aClass != null) {
                return aClass;
            }

            for (Class mClass : mClassList) {
                if (isInterceptorClass(mClass)) {
                    CLogUtils.e("找到了 Interceptor 类名是 " + mClass.getName());
                    return mClass;
                }
            }
        } catch (Throwable e) {
            CLogUtils.e("getInterceptorClass error " + e.toString());
        }
        return null;
    }

    /**
     * 判断是否是 Interceptor
     */
    private boolean isInterceptorClass(Class mClass) {
        if (mClass == null) {
            return false;
        }
        try {
            Method[] declaredMethods = mClass.getDeclaredMethods();
            //一个方法 并且 方法参数 是 内部的接口
            if (declaredMethods.length == 1
                    && mClass.isInterface()
            ) {
                Method declaredMethod = declaredMethods[0];
                Class<?>[] parameterTypes = declaredMethod.getParameterTypes();

                return parameterTypes.length == 1 &&
                        parameterTypes[0].getName().contains(mClass.getName()) &&
                        declaredMethod.getExceptionTypes().length == 1 &&
                        declaredMethod.getExceptionTypes()[0].getName().equals(IOException.class.getName());

            }
        } catch (Throwable e) {
            CLogUtils.e("isInterceptorClass error " + e.toString());
        }
        return false;
    }


    private void getAllMethod(Class<?> bin) {
        Method[] methods = bin.getMethods();
        for (Method m : methods) {
            CLogUtils.e(m.getName());
        }
    }

    /**
     * 尝试获取拦截器
     *
     * @return
     */
    @NonNull
    private synchronized Object getHttpLoggingInterceptorClass() {
        //防止多次初始化影响性能
        if (httpLoggingInterceptor != null) {
            return httpLoggingInterceptor;
        }

        try {
            //第一步 首先  判断 本身项目里 是否存在 拦截器
            //okhttp3.logging.HttpLoggingInterceptor
            try {
                mHttpLoggingInterceptorClass = getClass("okhttp3.logging.HttpLoggingInterceptor");
                mHttpLoggingInterceptorLoggerClass = getClass("okhttp3.logging.HttpLoggingInterceptor$Logger");

            } catch (Throwable e) {

            }
            if (mHttpLoggingInterceptorClass != null && mHttpLoggingInterceptorLoggerClass != null) {
                CLogUtils.e("拿到了App本身的 拦截器 log");
                return InitInterceptor();
            }

            if (mHttpLoggingInterceptorLoggerClass == null || mHttpLoggingInterceptorClass == null) {
                //当前 App 使用了OkHttp 三种种情况，需分别进行处理
                //1,App没有被混淆，没有拦截器
                if (isExactness()) {
                    CLogUtils.e("当前App的OkHttp没有被混淆,可直接动态添加拦截器");
                    //直接尝试动态加载即可
                    return initLoggingInterceptor();
                } else {
                    CLogUtils.e("当前App的OkHttp被混淆");
                    //2,App被混淆，有拦截器,根据拦截器特征获取
                    Object httpLoggingInterceptorForClass = getHttpLoggingInterceptorForClass();
                    if (httpLoggingInterceptorForClass == null) {
                        CLogUtils.e("App Okhttp被混淆 并且没有拦截器");
                        //3,App被混淆，没有拦截器
                        return getHttpLoggingInterceptorImp();
                    } else {
                        CLogUtils.e("App Okhttp被混淆 存在拦截器");
                        return httpLoggingInterceptorForClass;
                    }
                }


            }

        } catch (Throwable e) {
            CLogUtils.e("getHttpLoggingInterceptor  拦截器初始化出现异常    " + e.toString());
            e.printStackTrace();
        }
        return null;
    }


    private boolean isHttpLoggingInterceptor(Class MClass) {
        //Class本身是final类型 并且实现了拦截器接口，拦截器接口个数1
        try {
            if (Modifier.isFinal(MClass.getModifiers()) && MClass.getInterfaces().length == 1) {

                Field[] declaredFields = MClass.getDeclaredFields();
                for (Field field : declaredFields) {
                    int setCount = 0;
                    int charSetCount = 0;
                    //  private volatile Set<String> headersToRedact = Collections.emptySet();
                    if (field.getType().getName().equals(Set.class.getName())
                            && Modifier.isPrivate(field.getModifiers())
                            && Modifier.isVolatile(field.getModifiers())
                    ) {
                        setCount++;
                    }
                    //  private static final Charset UTF8 = Charset.forName("UTF-8");
                    if (field.getType().getName().equals(Charset.class.getName())
                            && Modifier.isPrivate(field.getModifiers())
                            && Modifier.isStatic(field.getModifiers())
                            && Modifier.isFinal(field.getModifiers())
                    ) {
                        charSetCount++;
                    }
                    if (setCount == 1 && charSetCount == 1) {
                        CLogUtils.e("发现HttpLoggingInterceptor名字是 " + MClass.getName());
                        return true;
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * App okHttp混淆以后尝试获取混淆以后的拦截器
     * 对HttpLoggingInterceptor 进行判断
     *
     * @return
     */
    private Object getHttpLoggingInterceptorForClass() {
        for (Class MClass : mClassList) {
            if (isHttpLoggingInterceptor(MClass)) {
                mHttpLoggingInterceptorClass = MClass;
                return getOkHttpLoggingInterceptorLogger();
            }
        }
        return null;
    }

    /**
     * @return 这个App是否是被混淆的okHttp
     */
    private boolean isExactness() {
        return OkHttpClient.getName().equals("okhttp3.OkHttpClient")
                && OkHttpBuilder.getName().equals("okhttp3.OkHttpClient$Builder")
                //拦截器里面常用的类不等于Null 才可以保证插件正常加载
                && getClass("okio.Buffer") != null
                && getClass("okio.BufferedSource") != null
                && getClass("okio.GzipSource") != null
                && getClass("okhttp3.Request") != null
                && getClass("okhttp3.Response") != null
                && getClass("okio.Okio") != null
                && getClass("okio.Base64") != null
                ;
    }


    private Object getOkHttpLoggingInterceptorLogger() {
        try {
            if (mHttpLoggingInterceptorClass == null) {
                CLogUtils.e("getOkHttpLoggingInterceptorLogger  mHttpLoggingInterceptor==null");
                return null;
            }
            CLogUtils.e("开始查找 logger 和 Leave");

            for (Class stringName : mClassList) {
                String ClassName = stringName.getName();
                //包含外部类 并且 是 接口
                if (ClassName.contains(mHttpLoggingInterceptorClass.getName() + "$") && stringName.isInterface()) {
                    mHttpLoggingInterceptorLoggerClass = stringName;
                    CLogUtils.e("找到了 mHttpLoggingInterceptorLoggerClass  " + mHttpLoggingInterceptorLoggerClass.getName());
                }
                if (ClassName.contains(mHttpLoggingInterceptorClass.getName() + "$") && stringName.isEnum()) {
                    mHttpLoggingInterceptorLoggerEnum = stringName;
                    CLogUtils.e("找到了 mHttpLoggingInterceptorLoggerEnum   " + mHttpLoggingInterceptorLoggerEnum.getName());
                }
            }
            if (mHttpLoggingInterceptorClass != null &&
                    mHttpLoggingInterceptorLoggerClass != null &&
                    mHttpLoggingInterceptorLoggerEnum != null) {
                return InitInterceptor2();
            } else {
                CLogUtils.e("没有找到 mHttpLoggingInterceptorLoggerClass 和 mHttpLoggingInterceptorLoggerEnum");
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * Hook 底层的方法
     * 这个是不管 什么 框架请求都会走的 函数
     */
    private void HookGetOutPushStream() {

        CLogUtils.e("开始 Hook底层 实现 ");
        //java.net.SocketInputStream


        try {
            XposedHelpers.findAndHookMethod(
                    XposedHelpers.findClass("java.net.SocketOutputStream", mLoader),
                    "write",
                    byte[].class,
                    int.class,
                    int.class,
//                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            StringBuilder TraceString = new StringBuilder();
                            if (isShowStacktTrash) {
                                try {
                                    int b = 1 / 0;
                                } catch (Throwable e) {
                                    StackTraceElement[] stackTrace = e.getStackTrace();
                                    TraceString.append(" --------------------------  >>>> " + "\n");
                                    for (StackTraceElement stackTraceElement : stackTrace) {
                                        //FileUtils.SaveString(  );
                                        TraceString.append("   栈信息      ").append(stackTraceElement.getClassName()).append(".").append(stackTraceElement.getMethodName()).append("行数  ").append(stackTraceElement.getLineNumber()).append("\n");
                                    }
                                    TraceString.append("<<<< --------------------------  " + "\n");
                                }
                            }
                            TraceString.append("<<<<------------------------------>>>>>  \n")
                                    .append(new String((byte[]) param.args[0], StandardCharsets.UTF_8))
                                    .append("\n <<<<------------------------------>>>>>").append("\n");

                            CLogUtils.NetLogger(TraceString.toString());
//                            FileUtils.SaveString(mSimpleDateFormat.format(new Date(System.currentTimeMillis())) + "\n" + "  " +
//                                    TraceString.toString(), mOtherContext.getPackageName());
                        }
                    });


//            XposedHelpers.findAndHookMethod(httpUrlConnClass, mLoader,
//                    httpUrlConnGetInputStreamMethod, new XC_MethodHook() {
//                        @Override
//                        protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws IOException {
//                            final Object httpUrlConnection = param.thisObject;
//                            URL url = (URL) XposedHelpers.callMethod(httpUrlConnection, httpUrlConnGetUrlMethod);
//                            InputStream in = (InputStream) param.getResult();
//
//                            InputStreamWrapper wrapper = CACHE.get();
//
//                            if (wrapper != null) {
//                                param.setResult(wrapper);
//                                return;
//                            }
//                            //流只能读取一次 用完在放回去
//                            wrapper = new InputStreamWrapper(in) {
//                                public void close() {
//                                    try {
//                                        super.close();
//                                    } catch (IOException e) {
//                                        e.printStackTrace();
//                                    }
//                                    CACHE.remove();
//                                }
//                            };
//                            //把数据流放到指定ThreadLocal里面保存起来
//                            CACHE.set(wrapper);
//                            param.setResult(wrapper);
//                            //判断是否保存成功
//                            if (wrapper.size() > 0) {
//                                //拿到解码类型
//                                String contentEncoding = (String) XposedHelpers.callMethod(httpUrlConnection,
//                                        httpUrlConnGetContentEncodingMethod);
//                                BufferedReader reader = null;
//                                StringBuffer responseBody = new StringBuffer();
//                                try {
//                                    if (contentEncoding != null) {
//                                        if ("gzip".equals(contentEncoding)) {
//                                            in = new GZIPInputStream(in);
//                                        }
//                                        CLogUtils.e("走了 gzip 解码");
//
//                                        //对gzip进行解析
//                                        reader = new BufferedReader(new InputStreamReader(in));
//
//                                        String line;
//                                        while ((line = reader.readLine()) != null) {
//                                            responseBody.append(line);
//                                        }
//                                    } else {
//                                        CLogUtils.e("走了 Body 解码");
//
//                                        //返回的是一个HttpEngine
//                                        Object HttpEngine = XposedHelpers.callMethod(httpUrlConnection,
//                                                httpUrlConngetGetResponseMethod);
//                                        Object respons = XposedHelpers.callMethod(HttpEngine,
//                                                httpUrlConngetGetResponseMethod);
//                                        //先判断是否含有Body
//                                        boolean hasBody = (boolean) XposedHelpers.callMethod(HttpEngine,
//                                                "hasBody", respons);
//                                        if (hasBody) {
//                                            Object body = XposedHelpers.callMethod(respons,
//                                                    "body");
//                                            String string = (String) XposedHelpers.callMethod(body,
//                                                    "string");
//                                            responseBody.append(string);
//
//                                        }
//                                    }
//                                    Map<String, List<String>> requestHeadersMap =
//                                            (Map<String, List<String>>) XposedHelpers.callMethod(httpUrlConnection,
//                                                    httpUrlConngetHeaderFieldsMethod);
//
//                                    StringBuilder RequestHeaders = new StringBuilder();
//
//                                    if (requestHeadersMap != null) {
//
//                                        for (String key : requestHeadersMap.keySet()) {
//                                            RequestHeaders.append(key);
//                                            List<String> values = requestHeadersMap.get(key);
//                                            for (String value : values) {
//                                                RequestHeaders.append(value).append(" ");
//                                            }
//                                            RequestHeaders.append("\n");
//                                        }
//                                    }
//
//                                    CLogUtils.e("当前Url \n" + url.toString() + "\n");
//                                    CLogUtils.e("请求头部信息 \n" + RequestHeaders.toString() + "\n");
//
//                                    CLogUtils.e("当前响应 \n" + responseBody.toString() + "\n");
//
//                                    StringBuilder TraceString = new StringBuilder();
//
//
//                                    if (isShowStacktTrash) {
//                                        try {
//                                            int b = 1 / 0;
//                                        } catch (Throwable e) {
//                                            StackTraceElement[] stackTrace = e.getStackTrace();
//                                            TraceString.append(" --------------------------  >>>> " + "\n");
//                                            for (StackTraceElement stackTraceElement : stackTrace) {
//                                                //FileUtils.SaveString(  );
//                                                TraceString.append("   栈信息      ").append(stackTraceElement.getClassName()).append(".").append(stackTraceElement.getMethodName()).append("行数  ").append(stackTraceElement.getLineNumber()).append("\n");
//                                            }
//                                            TraceString.append("<<<< --------------------------  " + "\n");
//                                        }
//                                    }
//
////                                    TraceString.append("<<<<------------------------------>>>>>  ")
////                                            .append("Url :  ").append(url.toString()).append("\n")
////                                            .append("请求头部信息 :  ").append(RequestHeaders.toString()).append("\n")
////                                            .append("响应信息 :  ").append(responseBody.toString()).append("\n")
////                                            .append(" <<<<------------------------------>>>>>").append("\n");
////
////                                    FileUtils.SaveString(mSimpleDateFormat.format(new Date(System.currentTimeMillis())) + "\n" + "  " +
////                                            TraceString.toString(), mOtherContext.getPackageName());
//
//                                } catch (Throwable e) {
//                                    CLogUtils.e("转换出现错误  " + e.toString() + "  line  " + e.getStackTrace());
//
//                                } finally {
//                                    try {
//                                        if (reader != null) {
//                                            reader.close();
//                                        }
//                                    } catch (Throwable e) {
//                                        CLogUtils.e("inputStream hook handle close reader error. url:" + url + ", contentEncoding:" + contentEncoding);
//                                    }
//                                }
//                            }
//                        }
//                    });
//
//
        } catch (Throwable e) {
            CLogUtils.e("HookGetOutPushStream     " + e.toString());
            e.printStackTrace();
        }
    }


    private Object InitInterceptor2() {
        try {

            Object logger = Proxy.newProxyInstance(mLoader, new Class[]{mHttpLoggingInterceptorLoggerClass}, Hook.this);

            CLogUtils.e("拿到  动态代理的 class");
            Object loggingInterceptor = mHttpLoggingInterceptorClass.getConstructor(mHttpLoggingInterceptorLoggerClass).newInstance(logger);
            CLogUtils.e("拿到  拦截器的实体类  ");

            Object level = mHttpLoggingInterceptorLoggerEnum.getEnumConstants()[3];
            CLogUtils.e("拿到  Level 枚举   ");

            Method setLevelMethod = getSetLevelMethod(mHttpLoggingInterceptorLoggerEnum);

            if (setLevelMethod == null) {
                CLogUtils.e("没有找到 setLevelMethod 方法体   ");
                //HookGetOutPushStream();
            } else {
                setLevelMethod.setAccessible(true);
                Object invoke = setLevelMethod.invoke(loggingInterceptor, level);
                CLogUtils.e("调用 setLevel成功  返回对应的  对象    " + invoke.getClass().getName());
                return invoke;
            }


        } catch (Throwable e) {
            CLogUtils.e("拦截器初始化出现异常  InitInterceptor2  " + e.toString());
            e.printStackTrace();
        }
        return null;
    }

    @Nullable
    private Method getSetLevelMethod(Class level) {
        CLogUtils.e("getSetLevelMethod 需要的参数类型 是 " + level.getName());

        Method[] declaredMethods = mHttpLoggingInterceptorClass.getDeclaredMethods();
        for (Method declaredMethod : declaredMethods) {
            Class<?>[] parameterTypes = declaredMethod.getParameterTypes();
            CLogUtils.e("该方法的 名字是   " + declaredMethod.getName() + "  参数的 个数是  " + parameterTypes.length);
            if (parameterTypes.length == 1) {
                CLogUtils.e("长度是 1 参数类型是  " + parameterTypes[0].getName());
                //比较 传入的类型 是 HttpLoggingInterceptor.Level 类的 方法
                if (parameterTypes[0].getName().equals(level.getName())) {
                    return declaredMethod;
                }
            }
        }
        return null;
    }


    private Object InitInterceptor() throws
            InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
//        CLogUtils.e("拿到  HttpLoggingInterceptor 和 logger ");
        //通过 动态代理 拿到 这个 接口 实体类
        Object logger;
//        if (isDexLoader) {
//            logger = Proxy.newProxyInstance(mDexClassLoader, new Class[]{mHttpLoggingInterceptorLoggerClass}, Hook.this);
//        } else {
        logger = Proxy.newProxyInstance(mLoader, new Class[]{mHttpLoggingInterceptorLoggerClass}, Hook.this);
//        }
//        CLogUtils.e("拿到  动态代理的 class");
        Object loggingInterceptor = mHttpLoggingInterceptorClass.getConstructor(mHttpLoggingInterceptorLoggerClass).newInstance(logger);
//        CLogUtils.e("拿到  拦截器的实体类  ");
        Object level;
//        if (isDexLoader) {
//            level = mDexClassLoader.loadClass("okhttp3.logging.HttpLoggingInterceptor$Level").getEnumConstants()[3];
//        } else {
        level = mLoader.loadClass("okhttp3.logging.HttpLoggingInterceptor$Level").getEnumConstants()[3];
//        }
//        CLogUtils.e("拿到  Level 枚举   ");
        //调用 函数
        //setLevel
        XposedHelpers.findMethodBestMatch(mHttpLoggingInterceptorClass, "setLevel",
                level.getClass()).invoke(loggingInterceptor, level);
        CLogUtils.e("拦截器实例初始化成功  ");
        return loggingInterceptor;
    }

    /**
     * 用 classloader 加载 这个jar 包 防止 classloader 不统一
     */
    private Object initLoggingInterceptor() {
        CLogUtils.e("开始 动态 加载 初始化 ");

        File dexOutputDir = mOtherContext.getDir("dex", 0);

        CLogUtils.e("dexOutputDir  dex  666 " + dexOutputDir.getAbsolutePath());
        // 定义DexClassLoader
        // 第一个参数：是dex压缩文件的路径
        // 第二个参数：是dex解压缩后存放的目录
        // 第三个参数：是C/C++依赖的本地库文件目录,可以为null
        // 第四个参数：是上一级的类加载器
        mDexClassLoader = new DexClassLoader(INTERCEPTORPATH, dexOutputDir.getAbsolutePath(), null, mLoader);
        try {

            if (AddElements()) {

                //mHttpLoggingInterceptor = mDexClassLoader.loadClass("okhttp3.logging.HttpLoggingInterceptor");
                //mHttpLoggingInterceptorLoggerClass = mDexClassLoader.loadClass("okhttp3.logging.HttpLoggingInterceptor$Logger");

                mHttpLoggingInterceptorClass = mLoader.loadClass("okhttp3.logging.HttpLoggingInterceptor");
                mHttpLoggingInterceptorLoggerClass = mLoader.loadClass("okhttp3.logging.HttpLoggingInterceptor$Logger");
                if (mHttpLoggingInterceptorLoggerClass != null && mHttpLoggingInterceptorClass != null) {
                    CLogUtils.e("动态 加载 classloader 成功 ");

                    return InitInterceptor();
                } else {
                    return null;
                }
            }
            return null;


        } catch (Throwable e) {
            CLogUtils.e("initLoggingInterceptor 动态加载异常  " + e.toString());
            e.printStackTrace();
        }
        return null;

    }


    /**
     * @return Dex 是否合并 成功
     */
    private boolean AddElements() {
        //自己的 classloader 里面的 element数组
        Object[] myDexClassLoaderElements = getMyDexClassLoaderElements();
        if (myDexClassLoaderElements == null) {
            CLogUtils.e("AddElements  myDexClassLoaderElements null");
            return false;
        } else {
            CLogUtils.e("AddElements  成功 拿到 myDexClassLoaderElements 自己的Elements 长度是   " + myDexClassLoaderElements.length);
        }
        //系统的  classloader 里面的 element数组
        Object[] classLoaderElements = getClassLoaderElements();
        //将数组合并
        if (classLoaderElements == null) {
            CLogUtils.e("AddElements  classLoaderElements null");
            return false;
        } else {
            CLogUtils.e("AddElements  成功 拿到 classLoaderElements 系统的Elements 长度是   " + classLoaderElements.length);
        }

        //DexElements合并
        Object[] combined = (Object[]) Array.newInstance(classLoaderElements.getClass().getComponentType(),
                classLoaderElements.length + myDexClassLoaderElements.length);

        System.arraycopy(classLoaderElements, 0, combined, 0, classLoaderElements.length);
        System.arraycopy(myDexClassLoaderElements, 0, combined, classLoaderElements.length, myDexClassLoaderElements.length);


        //Object[] dexElementsResut = concat(myDexClassLoaderElements, classLoaderElements);

        if ((classLoaderElements.length + myDexClassLoaderElements.length) != combined.length) {
            CLogUtils.e("合并 elements数组 失败  null");
        }
        //合并成功 重新 加载
        return SetDexElements(combined, myDexClassLoaderElements.length + classLoaderElements.length);
    }

    /**
     * 将 Elements 数组 set回原来的 classloader里面
     *
     * @param dexElementsResut
     */
    private boolean SetDexElements(Object[] dexElementsResut, int conunt) {
        try {
            Field pathListField = mLoader.getClass().getSuperclass().getDeclaredField("pathList");
            if (pathListField != null) {
                pathListField.setAccessible(true);
                Object dexPathList = pathListField.get(mLoader);
                Field dexElementsField = dexPathList.getClass().getDeclaredField("dexElements");
                if (dexElementsField != null) {
                    dexElementsField.setAccessible(true);
                    //先 重新设置一次
                    dexElementsField.set(dexPathList, dexElementsResut);
                    //重新 get 用
                    Object[] dexElements = (Object[]) dexElementsField.get(dexPathList);
                    if (dexElements.length == conunt && Arrays.hashCode(dexElements) == Arrays.hashCode(dexElementsResut)) {
                        return true;
                    } else {
                        CLogUtils.e("合成   长度  " + dexElements.length + "传入 数组 长度   " + conunt);

                        CLogUtils.e("   dexElements hashCode " + Arrays.hashCode(dexElements) + "  " + Arrays.hashCode(dexElementsResut));

                        return false;
                    }
                } else {
                    CLogUtils.e("SetDexElements  获取 dexElements == null");
                }
            } else {
                CLogUtils.e("SetDexElements  获取 pathList == null");
            }
        } catch (NoSuchFieldException e) {
            CLogUtils.e("SetDexElements  NoSuchFieldException   " + e.toString());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            CLogUtils.e("SetDexElements  IllegalAccessException   " + e.toString());
            e.printStackTrace();
        }
        return false;
    }

    private synchronized void getAllClassName() {

        //保证每次初始化 之前 保证干净
        AllClassNameList.clear();
        CLogUtils.e("开始 获取全部的类名  ");

        try {
            //系统的 classloader是 Pathclassloader需要 拿到他的 父类 BaseClassloader才有 pathList
            if (mLoader == null) {
                return;
            }
            Field pathListField = mLoader.getClass().getSuperclass().getDeclaredField("pathList");
            if (pathListField != null) {
                pathListField.setAccessible(true);
                Object dexPathList = pathListField.get(mLoader);
                Field dexElementsField = dexPathList.getClass().getDeclaredField("dexElements");
                if (dexElementsField != null) {
                    dexElementsField.setAccessible(true);
                    Object[] dexElements = (Object[]) dexElementsField.get(dexPathList);
                    for (Object dexElement : dexElements) {
                        Field dexFileField = dexElement.getClass().getDeclaredField("dexFile");
                        if (dexFileField != null) {
                            dexFileField.setAccessible(true);
                            DexFile dexFile = (DexFile) dexFileField.get(dexElement);
                            getDexFileClassName(dexFile);
                        } else {
                            CLogUtils.e("获取 dexFileField Null ");
                        }
                    }
                } else {
                    CLogUtils.e("获取 dexElements Null ");
                }
            } else {
                CLogUtils.e("获取 pathListField Null ");
            }
            //获取 app包里的
            //DexFile df = new DexFile(mOtherContext.getPackageCodePath());
        } catch (Throwable e) {
            CLogUtils.e("getAllClassName   Throwable   " + e.toString());
            e.printStackTrace();
        }
    }

    /**
     * 数组合并
     * @param a 原数组
     * @param b 新数组
     * @return 合并以后的数组
     */
//    static Object[] concat(Object[] a, Object[] b) {
//
//        //Object[] c= new Object[a.length+b.length];
//
//
//        System.arraycopy(original, 0, combined, 0, original.length);
//        System.arraycopy(extraElements, 0, combined, original.length, extraElements.length);
//
//        return c;
//
//    }


    /**
     * 将自己 创建的 classloader 里面的 内容添加到 原来的 classloader里面
     */
    private Object[] getMyDexClassLoaderElements() {
        try {
            Field pathListField = mDexClassLoader.getClass().getSuperclass().getDeclaredField("pathList");
            if (pathListField != null) {
                pathListField.setAccessible(true);
                Object dexPathList = pathListField.get(mDexClassLoader);
                Field dexElementsField = dexPathList.getClass().getDeclaredField("dexElements");
                if (dexElementsField != null) {
                    dexElementsField.setAccessible(true);
                    Object[] dexElements = (Object[]) dexElementsField.get(dexPathList);
                    if (dexElements != null) {
                        return dexElements;
                    } else {
                        CLogUtils.e("AddElements  获取 dexElements == null");
                    }
                    //ArrayUtils.addAll(first, second);
                } else {
                    CLogUtils.e("AddElements  获取 dexElements == null");
                }
            } else {
                CLogUtils.e("AddElements  获取 pathList == null");
            }
        } catch (NoSuchFieldException e) {
            CLogUtils.e("AddElements  NoSuchFieldException   " + e.toString());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            CLogUtils.e("AddElements  IllegalAccessException   " + e.toString());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取系统的 classaLoder
     */
    private Object[] getClassLoaderElements() {
        try {
            Field pathListField = mLoader.getClass().getSuperclass().getDeclaredField("pathList");
            if (pathListField != null) {
                pathListField.setAccessible(true);
                Object dexPathList = pathListField.get(mLoader);
                Field dexElementsField = dexPathList.getClass().getDeclaredField("dexElements");
                if (dexElementsField != null) {
                    dexElementsField.setAccessible(true);
                    Object[] dexElements = (Object[]) dexElementsField.get(dexPathList);
                    if (dexElements != null) {
                        return dexElements;
                    } else {
                        CLogUtils.e("AddElements  获取 dexElements == null");
                    }
                    //ArrayUtils.addAll(first, second);
                } else {
                    CLogUtils.e("AddElements  获取 dexElements == null");
                }
            } else {
                CLogUtils.e("AddElements  获取 pathList == null");
            }
        } catch (NoSuchFieldException e) {
            CLogUtils.e("AddElements  NoSuchFieldException   " + e.toString());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            CLogUtils.e("AddElements  IllegalAccessException   " + e.toString());
            e.printStackTrace();
        }
        return null;
    }

}
