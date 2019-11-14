package q296488320.xposedinto.XpHook;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;
import dalvik.system.PathClassLoader;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import q296488320.xposedinto.LogImp.LogInterceptorImp;
import q296488320.xposedinto.config.Key;
import q296488320.xposedinto.utils.CLogUtils;
import q296488320.xposedinto.utils.FileUtils;
import q296488320.xposedinto.utils.HookSpUtil;
import q296488320.xposedinto.utils.SpUtil;
import dalvik.system.*;

import static android.icu.lang.UCharacter.GraphemeClusterBreak.T;
//import org.jf.dexlib2.DexFileFactory;
//import org.jf.dexlib2.Opcodes;
//import org.jf.dexlib2.dexbacked.DexBackedClassDef;
//import org.jf.dexlib2.dexbacked.DexBackedDexFile;
//import org.jf.dexlib2.dexbacked.DexBackedField;


/**
 * Created by lyh on 2019/6/18.
 */

public class Hook implements IXposedHookLoadPackage, InvocationHandler {

    private final SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH时:mm分:ss秒");

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
    private ClassLoader mLoader = null;


    private String ClassPath = "okhttp3.OkHttpClient$Builder";
    //使用 共享 数据的 XSharedPreferences
    private XSharedPreferences shared;


    //OkHttp里面的 类
    private Class<?> OkHttpBuilder = null;
    private Class<?> OkHttpClient = null;

    //拦截器里面的类
    private Class<?> mHttpLoggingInterceptor;
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

    //存放 全部类名字的 集合
    private final List<String> AllClassNameList = new ArrayList<>();

    //存放 全部类的 集合
    public static CopyOnWriteArrayList<Class> mClassList = new CopyOnWriteArrayList<>();


    private volatile int Flag = 0;


    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            shared = new XSharedPreferences("Xposed.Okhttp.Cat", "config");
            shared.reload();

            InvokPackage = shared.getString("APP_INFO", "");
            MODEL = shared.getString("MODEL", "1");

            if (MODEL.equals("4")) {
                isShowStacktTrash = true;
            }

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
                        CLogUtils.e("拿到 classloader");
                        HookSpUtil.putString(mOtherContext,"OnCreateFlag","0");
                        HookSpUtil.putString(mOtherContext,"ConstructorFlag","0");
                        CLogUtils.e("保存 完毕  ");

                    }
                });


        XposedHelpers.findAndHookMethod(Application.class, "onCreate",
                new XC_MethodHook() {
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        String processName = getCurProcessName(mOtherContext);
                        CLogUtils.e("进程 名字   " + processName);
                        if (!TextUtils.isEmpty(processName)) {
                            boolean defaultProcess = processName.equals(mOtherContext.getPackageName());
                            //如果 比较是 0
                            boolean equals = HookSpUtil.getString(mOtherContext, "OnCreateFlag", "").equals("0");
                            //只在主线程 处理 并且 只处理一次
                            if (defaultProcess && equals) {
                                //当前应用的初始化
                                CLogUtils.e("Hook到    onCreate");
                                // 被添加多次 onCreate 和 构造 都有可能被执行多次
                                HookSpUtil.putString(mOtherContext,"OnCreateFlag","1");
                                if (MODEL.equals("3") || MODEL.equals("4")) {
                                    CLogUtils.e("使用了 通杀模式 ");
                                    HookGetOutPushStream();
                                    return;
                                }
                                HookOKClient();
                            }
                        }
                    }
                });
//        }
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
            CLogUtils.e("出现异常 对方没有使用OkHttp或者被 混淆了  开始尝试自动 获取 路径 " + e.getMessage());
            HookAndInitProguardClass();
            return;
        }
        try {
            if (OkHttpBuilder == null) {
                OkHttpBuilder = Class.forName("okhttp3.OkHttpClient$Builder", false, mLoader);
            }
        } catch (ClassNotFoundException e) {
            CLogUtils.e("出现异常 对方没有使用OkHttp或者被 混淆了  开始尝试自动 获取 路径 " + e.getMessage());
            HookAndInitProguardClass();
            return;
        }
        if (OkHttpBuilder != null && OkHttpClient != null) {
            invoke();
        }
    }


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


    private synchronized void HookAndInitProguardClass() {
        //先拿到 app里面全部的class
        getAllClassName();
        initAllClass();
        AddClassOkIOPackage();
        //第一步 先开始 拿到 OkHttp 里面的 类  如Client 和 Builder
        getClientClass();
        getBuilder();

        HookBuilderConstructor();
    }

    /**
     * 这个方法 主要是 有的 io目录 被混淆了 导致后面的 拦截器方法  被 干掉了
     * 需要先 找到 okIO的 路径
     * 通过  FormBodyz这个方法 进行 参数1 拿到 路径
     * public void writeTo(BufferedSink sink) throws IOException
     * <p>
     * 比如 okio 目录被混淆成 abc这样的路径 我没做识别
     * 没有加到 mClassList 这个里面也就导致后面的程序 会 找不到 抛异常
     * 为了保险 在此添加一次 跟 Okio有关的类
     */
    private synchronized void AddClassOkIOPackage() {

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
                            return;
                        } else {
                            //将 OKIO 目录 加进去
                            for (String string : AllClassNameList) {
                                if (string.startsWith(ioPath)) {
                                    Class<?> aClass = null;
                                    try {
                                        aClass = Class.forName(string, false, mLoader);
                                    } catch (ClassNotFoundException e) {
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
    private synchronized void initAllClass() {
        mClassList.clear();
        CLogUtils.e("开始 初始化全部的类  " + AllClassNameList.size());
        Class<?> MClass = null;
        for (int i = 0; i < AllClassNameList.size(); i++) {
            try {
                if (AllClassNameList.get(i).contains("okhttp3") || AllClassNameList.get(i).contains("okio")) {//在当前所有可执行的类里面查找包含有该包名的所有类
                    MClass = Class.forName(AllClassNameList.get(i), false, mLoader);
                    if (AllClassNameList.get(i).contains("okhttp3.logging")) {
                        OkHttpLoggerList.add(AllClassNameList.get(i));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                //CLogUtils.e("initAllClass   "+e.getMessage()+"   index  "+i);
            }
            if (MClass != null) {
                mClassList.add(MClass);
            }
        }

        CLogUtils.e("初始化全部类的个数   " + mClassList.size());
    }


    /**
     * 获取 ClientCLass的方法
     */
    private synchronized void getClientClass() {
        if (mClassList.size() == 0) {
            CLogUtils.e("全部的 集合 mClassList  的个数 为 0  ");
            return;
        }
        CLogUtils.e("开始 查找   ClientClass");
        for (Class mClient : mClassList) {
            //判断 集合 个数 先拿到 四个集合 可以 拿到 Client
            if (isClient(mClient)) {
                OkHttpClient = mClient;
                CLogUtils.e("找到了 外层  开始 找内层 ");
                return;
            }
        }
        CLogUtils.e("没找到 client ");

        //没找到 OkHttp的 主要类 直接 Hook底层
        //HookGetOutPushStream();
    }


    /**
     * 混淆 以后 获取  集合并添加 拦截器的方法
     */
    private synchronized void getBuilder() {
        if (OkHttpClient == null) {
            CLogUtils.e("混淆以后  OkHttpClient==Null ");
            //没找到 OkHttp的 主要类 直接 Hook底层
            //HookGetOutPushStream();
        } else {
            //开始查找 build
            for (Class builder : mClassList) {
                if (isBuilder(builder)) {
                    OkHttpBuilder = builder;
                }
            }
        }
    }

    private synchronized void HookBuilderConstructor() {
        CLogUtils.e("开始 Hook构造  ");
        //混淆 以后 只能 Hook 构造 目的 是为了 拿到 object
        //尝试 Hook 构造
        XposedHelpers.findAndHookConstructor(OkHttpBuilder, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                boolean equals = HookSpUtil.getString(mOtherContext, "ConstructorFlag", "").equals("0");
                if(equals) {
                    CLogUtils.e("Hook 到 构造函数  OkHttpBuilder");
                    AddInterceptors2(param);
                    HookSpUtil.putString(mOtherContext,"ConstructorFlag","1");
                }
            }
        });
        XposedHelpers.findAndHookConstructor(OkHttpClient, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                boolean equals = HookSpUtil.getString(mOtherContext,"ConstructorFlag", "").equals("0");
                if(equals) {
                    CLogUtils.e("Hook 到 构造函数  OkHttpClent");
                    AddInterceptors2(param);
                    HookSpUtil.putString(mOtherContext,"ConstructorFlag","1");
                }
            }
        });
    }

    private ArrayList<Field> mFieldArrayList = new ArrayList<>();

    private boolean isBuilder(@NonNull Class ccc) {

        int ListTypeCount = 0;
        int FinalTypeCount = 0;
        Field[] fields = ccc.getDeclaredFields();
        List<Field> List66 = new ArrayList<>();
        for (Field field : fields) {
            String type = field.getType().getName();
            //四个 集合
            if (type.contains(List.class.getName())) {
                ListTypeCount++;
            }
            //2 个 为 final类型
            if (type.contains(List.class.getName()) && Modifier.isFinal(field.getModifiers())) {
                List66.add(field);
                FinalTypeCount++;
            }
        }
        //四个 List 两个 2 final  并且 包含父类名字
        if (ListTypeCount == 4 && FinalTypeCount == 2 && ccc.getName().contains(OkHttpClient.getName())) {
            CLogUtils.e(" 找到 Builer  " + ccc.getName());
            mFieldArrayList.addAll(List66);
            return true;
        }
        return false;
    }

    private boolean isClient(@NonNull Class<?> mClass) {
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
            CLogUtils.e(" 该类的名字是  " + mClass.getName());
            return true;
        }
        // mFieldArrayList.clear();
        return false;
    }




    private void getDexFileClassName(DexFile dexFile, List<String> classNameList) {
        //获取df中的元素  这里包含了所有可执行的类名 该类名包含了包名+类名的方式
        Enumeration<String> enumeration = dexFile.entries();
        while (enumeration.hasMoreElements()) {//遍历
            String className = enumeration.nextElement();
            classNameList.add(className);
        }
    }


    private void invoke() {
        CLogUtils.e("发现对方使用 okhttp  开始hook ");
        if (MODEL.equals("1")) {
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


    private void AddInterceptors(XC_MethodHook.MethodHookParam param) {
        try {
            //interceptors
            List interceptors = (List) XposedHelpers.getObjectField(param.thisObject, "interceptors");
            CLogUtils.e("拿到了 拦截器  集合 ");
            if(interceptors!=null) {
                Object httpLoggingInterceptor = getHttpLoggingInterceptor();
                interceptors.add(httpLoggingInterceptor);
            }
        } catch (Exception e) {
            CLogUtils.e("Hook到 build但出现 异常 111111     " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int InterceptorsFlag = 0;

    private void AddInterceptors2(XC_MethodHook.MethodHookParam param) {
        try {
            if (mFieldArrayList.size() == 2) {
                //随便 拿一个 添加 一个 log拦截器 进去
                List interceptors = (List) XposedHelpers.getObjectField(param.thisObject, mFieldArrayList.get(0).getName());
                CLogUtils.e("拿到 集合  ");
                Object httpLoggingInterceptor = getHttpLoggingInterceptor();
                if (httpLoggingInterceptor != null) {
                    CLogUtils.e("拿到 混淆 以后的  拦截器 实例  ");
                    interceptors.add(httpLoggingInterceptor);
                } else {
                    CLogUtils.e("没有 拿到 拦截器   开始 动态代理  自动识别   ");
                    //防止 拦截器多次添加
                    Object httpLoggingInterceptorImp = getHttpLoggingInterceptorImp();
                    if (httpLoggingInterceptorImp != null && InterceptorsFlag == 0) {
                        interceptors.add(httpLoggingInterceptorImp);
                        InterceptorsFlag = 1;
                        CLogUtils.e("动态代理   拦截器 设置成功  ");
                    }
                }
            } else {
                CLogUtils.e("mFieldArrayList 的 集合 不是 2  ");
            }

        } catch (Exception e) {
            CLogUtils.e("Hook到 build但出现 异常 22222" + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * 自实现 loggerInterceptor 类
     */
    private Object getHttpLoggingInterceptorImp() {
        Class InterceptorClass = getInterceptorClass();
        Object InterceptorObject;
        if (InterceptorClass != null) {
            CLogUtils.e("拿到 拦截器 接口class   " + InterceptorClass.getName());

            LogInterceptorImp logInterceptorImp = new LogInterceptorImp();

            InterceptorObject = Proxy.newProxyInstance(mLoader, new Class[]{InterceptorClass}, logInterceptorImp);
            CLogUtils.e("动态代理   设置成功  ");
            return InterceptorObject;
        } else {
            CLogUtils.e("getInterceptorClass 返回 Null");
            return null;
        }
    }

    private Class getInterceptorClass() {
        for (Class mClass : mClassList) {
            if (isInterceptorClass(mClass)) {
                return mClass;
            }
        }
        return null;
    }

    /**
     * 判断是否是 Interceptor
     */
    private boolean isInterceptorClass(Class mClass) {
        Method[] declaredMethods = mClass.getDeclaredMethods();
        //一个方法 并且 方法参数 是 内部的接口
        if (declaredMethods.length == 1 && mClass.isInterface()) {
            Method declaredMethod = declaredMethods[0];
            Class<?>[] parameterTypes = declaredMethod.getParameterTypes();
            return parameterTypes.length == 1 && parameterTypes[0].getName().contains(mClass.getName());
        }
        return false;
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        CLogUtils.NetLogger((String) args[0]);
        return null;
    }


    private void getAllMethod(Class<?> bin) {
        Method[] methods = bin.getMethods();
        for (Method m : methods) {
            CLogUtils.e(m.getName());
        }
    }

    int getHttpLoggingInterceptorFlag = 0;

    @NonNull
    private Object getHttpLoggingInterceptor() {
        try {
            //第一步 首先  判断 本身项目里 是否存在 拦截器
            //okhttp3.logging.HttpLoggingInterceptor
            try {
                mHttpLoggingInterceptor = Class.forName("okhttp3.logging.HttpLoggingInterceptor", false, mLoader);
            } catch (ClassNotFoundException e) {
                CLogUtils.e("getHttpLoggingInterceptor  拦截器初始化出现异常  ClassNotFoundException  " + e.getMessage());
                if (mFieldArrayList.size() == 2) {
                    //混淆 无法 动态 加载
                    return getOkHttpLoggingInterceptor();
                }
                // 没有 混淆 可以动态 加载
                return initLoggingInterceptor();
            }
            try {
                mHttpLoggingInterceptorLoggerClass = Class.forName("okhttp3.logging.HttpLoggingInterceptor$Logger", false, mLoader);
            } catch (ClassNotFoundException e) {
                CLogUtils.e("getHttpLoggingInterceptor  拦截器初始化出现异常  ClassNotFoundException  " + e.getMessage());
                if (mFieldArrayList.size() == 2) {
                    //混淆 无法 动态 加载
                    return getOkHttpLoggingInterceptor();
                }
                // 没有 混淆 可以动态 加载
                return initLoggingInterceptor();
            }
            if (mHttpLoggingInterceptor != null && mHttpLoggingInterceptorLoggerClass != null) {
                CLogUtils.e("拿到了 拦截器 log");
                return InitInterceptor();
            }
        }
        catch (InstantiationException e) {
            CLogUtils.e("getHttpLoggingInterceptor  拦截器初始化出现异常  InstantiationException  " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            CLogUtils.e("getHttpLoggingInterceptor  拦截器初始化出现异常  IllegalAccessException  " + e.getMessage());
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            CLogUtils.e("getHttpLoggingInterceptor  拦截器初始化出现异常  InvocationTargetException  " + e.getMessage());
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            CLogUtils.e("getHttpLoggingInterceptor  拦截器初始化出现异常  NoSuchMethodException  " + e.getMessage());
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            CLogUtils.e("getHttpLoggingInterceptor  拦截器初始化出现异常  ClassNotFoundException  " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    @Nullable
    private Object getOkHttpLoggingInterceptor() {
        try {
            if (OkHttpLoggerList.size() != 0) {
                for (String stringName : OkHttpLoggerList) {
                    Class<?> aClass = Class.forName(stringName, false, mLoader);
                    if (isHttpLoggingInterceptor(aClass)) {
                        mHttpLoggingInterceptor = aClass;
                        //拿到 里面的接口类 名字
                        CLogUtils.e("找到了  HttpLoggingInterceptor ");
                        return getOkHttpLoggingInterceptorLogger();
                    }
                }
                //全部的类都没有找到复合相关的 就开始 Hook底层 函数
                //HookGetOutPushStream();
            } else {
                //HookGetOutPushStream();
                return null;
            }
        } catch (ClassNotFoundException e) {
            CLogUtils.e("getOkHttpLoggingInterceptor    异常   ClassNotFoundException ");

            e.printStackTrace();
        }
        return null;
    }

    private Object getOkHttpLoggingInterceptorLogger() {
        try {
            if (mHttpLoggingInterceptor == null) {
                CLogUtils.e("getOkHttpLoggingInterceptorLogger  mHttpLoggingInterceptor==null");
                return null;
            }
            CLogUtils.e("开始查找 logger 和 Leave");

            for (String stringName : OkHttpLoggerList) {
                Class<?> stringNameClass = Class.forName(stringName, true, mLoader);
                //包含外部类 并且 是 接口
                if (stringName.contains(mHttpLoggingInterceptor.getName() + "$") && stringNameClass.isInterface()) {
                    mHttpLoggingInterceptorLoggerClass = stringNameClass;
                    CLogUtils.e("找到了 mHttpLoggingInterceptorLoggerClass  " + mHttpLoggingInterceptorLoggerClass.getName());
                }
                if (stringName.contains(mHttpLoggingInterceptor.getName() + "$") && stringNameClass.isEnum()) {
                    mHttpLoggingInterceptorLoggerEnum = stringNameClass;
                    CLogUtils.e("找到了 mHttpLoggingInterceptorLoggerEnum   " + mHttpLoggingInterceptorLoggerEnum.getName());
                }
            }
            if (mHttpLoggingInterceptor != null && mHttpLoggingInterceptorLoggerClass != null && mHttpLoggingInterceptorLoggerEnum != null) {
                return InitInterceptor2();
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }


    private boolean isHttpLoggingInterceptor(Class<?> aClass) {
        int CharsetCount = 0;
        int FinalCount = 0;
        Field[] declaredFields = aClass.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            String type = declaredField.getType().getName();
            //Object o = declaredFields[i].get(aClass);
            if (type.contains("java.nio.charset.Charset") && Modifier.isFinal(declaredField.getModifiers())) {
                CharsetCount++;
            }
            if (Modifier.isFinal(declaredField.getModifiers())) {
                FinalCount++;
            }
        }
        return FinalCount == 2 && CharsetCount == 1 && declaredFields.length >= 3;
    }

    /**
     * Hook 底层的方法
     * 这个是不管 什么 框架请求都会走的 函数
     */
    private void HookGetOutPushStream() {

        CLogUtils.e("开始 Hook底层 实现 ");
        //java.net.SocketInputStream
        try {
            SocketInputStreamClass = Class.forName("java.net.SocketOutputStream", true, mLoader);
            if (SocketInputStreamClass == null) {
                SocketInputStreamClass = Class.forName("java.net.SocketOutputStream");
            }
            if (SocketInputStreamClass != null) {
                CLogUtils.e("拿到 InputStream ");
            }
            XposedHelpers.findAndHookMethod(SocketInputStreamClass, "write",
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
                                } catch (Exception e) {
                                    StackTraceElement[] stackTrace = e.getStackTrace();
                                    TraceString.append(" --------------------------  >>>> " + "\n");
                                    for (StackTraceElement stackTraceElement : stackTrace) {
                                        //FileUtils.SaveString(  );
                                        TraceString.append("   栈信息      ").append(stackTraceElement.getClassName()).append(".").append(stackTraceElement.getMethodName()).append("\n");
                                    }
                                    TraceString.append("<<<< --------------------------  " + "\n");
                                }
                            }
                            TraceString.append("<<<<------------------------------>>>>>  \n").append(new String((byte[]) param.args[0], StandardCharsets.UTF_8)).append("\n <<<<------------------------------>>>>>").append("\n");

                            FileUtils.SaveString(mSimpleDateFormat.format(new Date(System.currentTimeMillis())) + "\n" + "  " +
                                    TraceString.toString(), mOtherContext.getPackageName());
                        }
                    });
        } catch (ClassNotFoundException e) {
            CLogUtils.e("HookGetOutPushStream   ClassNotFoundException  " + e.getMessage());
            e.printStackTrace();
        }
    }


    private Object InitInterceptor2() {
        try {

            Object logger = Proxy.newProxyInstance(mLoader, new Class[]{mHttpLoggingInterceptorLoggerClass}, Hook.this);

            CLogUtils.e("拿到  动态代理的 class");
            Object loggingInterceptor = mHttpLoggingInterceptor.getConstructor(mHttpLoggingInterceptorLoggerClass).newInstance(logger);
            CLogUtils.e("拿到  拦截器的实体类  ");

            Object level = mHttpLoggingInterceptorLoggerEnum.getEnumConstants()[3];
            CLogUtils.e("拿到  Level 枚举   ");

            Method setLevelMethod = getSetLevelMethod(mHttpLoggingInterceptorLoggerEnum);

            if (setLevelMethod == null) {
                CLogUtils.e("没有找到 setLevelMethod 方法体   ");
                //HookGetOutPushStream();
            } else {
                Object invoke = setLevelMethod.invoke(loggingInterceptor, level);
                CLogUtils.e("调用 setLevel成功  返回对应的  对象    " + invoke.getClass().getName());
                return invoke;
            }


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

    @Nullable
    private Method getSetLevelMethod(Class level) {
        CLogUtils.e("getSetLevelMethod 需要的参数类型 是 " + level.getName());

        Method[] declaredMethods = mHttpLoggingInterceptor.getDeclaredMethods();
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
        CLogUtils.e("拿到  HttpLoggingInterceptor 和 logger ");
        //通过 动态代理 拿到 这个 接口 实体类
        Object logger;
//        if (isDexLoader) {
//            logger = Proxy.newProxyInstance(mDexClassLoader, new Class[]{mHttpLoggingInterceptorLoggerClass}, Hook.this);
//        } else {
            logger = Proxy.newProxyInstance(mLoader, new Class[]{mHttpLoggingInterceptorLoggerClass}, Hook.this);
//        }
        CLogUtils.e("拿到  动态代理的 class");
        Object loggingInterceptor = mHttpLoggingInterceptor.getConstructor(mHttpLoggingInterceptorLoggerClass).newInstance(logger);
        CLogUtils.e("拿到  拦截器的实体类  ");
        Object level;
//        if (isDexLoader) {
//            level = mDexClassLoader.loadClass("okhttp3.logging.HttpLoggingInterceptor$Level").getEnumConstants()[3];
//        } else {
            level = mLoader.loadClass("okhttp3.logging.HttpLoggingInterceptor$Level").getEnumConstants()[3];
//        }
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

            if (AddElements()){

                //mHttpLoggingInterceptor = mDexClassLoader.loadClass("okhttp3.logging.HttpLoggingInterceptor");
                //mHttpLoggingInterceptorLoggerClass = mDexClassLoader.loadClass("okhttp3.logging.HttpLoggingInterceptor$Logger");

                mHttpLoggingInterceptor = mLoader.loadClass("okhttp3.logging.HttpLoggingInterceptor");
                mHttpLoggingInterceptorLoggerClass = mLoader.loadClass("okhttp3.logging.HttpLoggingInterceptor$Logger");
                if (mHttpLoggingInterceptorLoggerClass != null && mHttpLoggingInterceptor != null) {
                    CLogUtils.e("动态 加载 classloader 成功 ");

                    return InitInterceptor();
                }else {
                    return null;
                }
            }
           return null;


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


    /**
     * @return Dex 是否合并 成功
     */
    private boolean AddElements() {
        //自己的 classloader 里面的 element数组
        Object[] myDexClassLoaderElements = getMyDexClassLoaderElements();
        if(myDexClassLoaderElements==null){
            CLogUtils.e("AddElements  myDexClassLoaderElements null");
            return false;
        }else {
            CLogUtils.e("AddElements  成功 拿到 myDexClassLoaderElements 自己的Elements 长度是   "+myDexClassLoaderElements.length);
        }
        //系统的  classloader 里面的 element数组
        Object[] classLoaderElements = getClassLoaderElements();
        //将数组合并
        if(classLoaderElements==null){
            CLogUtils.e("AddElements  classLoaderElements null");
            return false;
        }else {
            CLogUtils.e("AddElements  成功 拿到 classLoaderElements 系统的Elements 长度是   "+classLoaderElements.length);
        }

        //DexElements合并
        Object[] combined = (Object[]) Array.newInstance(classLoaderElements.getClass().getComponentType(),
                classLoaderElements.length + myDexClassLoaderElements.length);

        System.arraycopy(classLoaderElements, 0, combined, 0, classLoaderElements.length);
        System.arraycopy(myDexClassLoaderElements, 0, combined, classLoaderElements.length, myDexClassLoaderElements.length);


        //Object[] dexElementsResut = concat(myDexClassLoaderElements, classLoaderElements);

        if((classLoaderElements.length+myDexClassLoaderElements.length)!=combined.length){
            CLogUtils.e("合并 elements数组 失败  null");
        }
        //合并成功 重新 加载
        return SetDexElements(combined,myDexClassLoaderElements.length+classLoaderElements.length);
    }

    /**
     * 将 Elements 数组 set回原来的 classloader里面
     * @param dexElementsResut
     */
    private boolean SetDexElements(Object[] dexElementsResut,int conunt) {
        try {
            Field pathListField = mLoader.getClass().getSuperclass().getDeclaredField("pathList");
            if (pathListField != null) {
                pathListField.setAccessible(true);
                Object dexPathList = pathListField.get(mLoader);
                Field dexElementsField = dexPathList.getClass().getDeclaredField("dexElements");
                if (dexElementsField != null) {
                    dexElementsField.setAccessible(true);
                    //先 重新设置一次
                    dexElementsField.set(dexPathList,dexElementsResut);
                    //重新 get 用
                    Object[] dexElements = (Object[]) dexElementsField.get(dexPathList);
                    if(dexElements.length==conunt&& Arrays.hashCode(dexElements) == Arrays.hashCode(dexElementsResut)){
                        return true;
                    }else {
                        CLogUtils.e("合成   长度  "+dexElements.length+"传入 数组 长度   "+conunt);

                        CLogUtils.e("   dexElements hashCode "+Arrays.hashCode(dexElements)+"  "+Arrays.hashCode(dexElementsResut));

                        return false;
                    }
                }else {
                    CLogUtils.e("SetDexElements  获取 dexElements == null");
                }
            }else {
                CLogUtils.e("SetDexElements  获取 pathList == null");
            }
        } catch (NoSuchFieldException e) {
            CLogUtils.e("SetDexElements  NoSuchFieldException   "+e.getMessage());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            CLogUtils.e("SetDexElements  IllegalAccessException   "+e.getMessage());
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
                            getDexFileClassName(dexFile, AllClassNameList);
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
        } catch (NoSuchFieldException e) {
            CLogUtils.e("getAllClassName   NoSuchFieldException   " + e.getMessage());

            e.printStackTrace();
        } catch (IllegalAccessException e) {
            CLogUtils.e("getAllClassName   IllegalAccessException   " + e.getMessage());

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
    private Object[] getMyDexClassLoaderElements()  {
        try {
            Field pathListField = mDexClassLoader.getClass().getSuperclass().getDeclaredField("pathList");
            if (pathListField != null) {
                pathListField.setAccessible(true);
                Object dexPathList = pathListField.get(mDexClassLoader);
                Field dexElementsField = dexPathList.getClass().getDeclaredField("dexElements");
                if (dexElementsField != null) {
                    dexElementsField.setAccessible(true);
                    Object[] dexElements = (Object[]) dexElementsField.get(dexPathList);
                    if(dexElements!=null){
                        return dexElements;
                    }else {
                        CLogUtils.e("AddElements  获取 dexElements == null");
                    }
                    //ArrayUtils.addAll(first, second);
                }else {
                    CLogUtils.e("AddElements  获取 dexElements == null");
                }
            }else {
                CLogUtils.e("AddElements  获取 pathList == null");
            }
        } catch (NoSuchFieldException e) {
            CLogUtils.e("AddElements  NoSuchFieldException   "+e.getMessage());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            CLogUtils.e("AddElements  IllegalAccessException   "+e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    /**
     * 获取系统的 classaLoder
     */
    private Object[] getClassLoaderElements()  {
        try {
            Field pathListField = mLoader.getClass().getSuperclass().getDeclaredField("pathList");
            if (pathListField != null) {
                pathListField.setAccessible(true);
                Object dexPathList = pathListField.get(mLoader);
                Field dexElementsField = dexPathList.getClass().getDeclaredField("dexElements");
                if (dexElementsField != null) {
                    dexElementsField.setAccessible(true);
                    Object[] dexElements = (Object[]) dexElementsField.get(dexPathList);
                    if(dexElements!=null){
                        return dexElements;
                    }else {
                        CLogUtils.e("AddElements  获取 dexElements == null");
                    }
                    //ArrayUtils.addAll(first, second);
                }else {
                    CLogUtils.e("AddElements  获取 dexElements == null");
                }
            }else {
                CLogUtils.e("AddElements  获取 pathList == null");
            }
        } catch (NoSuchFieldException e) {
            CLogUtils.e("AddElements  NoSuchFieldException   "+e.getMessage());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            CLogUtils.e("AddElements  IllegalAccessException   "+e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

}
