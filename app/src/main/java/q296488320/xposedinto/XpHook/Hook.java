package q296488320.xposedinto.XpHook;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import q296488320.xposedinto.LogImp.LogInterceptorImp;
import q296488320.xposedinto.config.Key;
import q296488320.xposedinto.utils.CLogUtils;
import q296488320.xposedinto.utils.FileUtils;


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

    /**
     * 函数 可能被调用 多次
     * 防止被添加多次 拦截器 加一个 flags
     */
    private volatile int flag = 0;
    private volatile int flag1 = 0;
    private volatile int flag2 = 0;
    private volatile int flag3 = 0;
    private volatile int flag4 = 0;
    private volatile int flag5 = 0;

    /**
     * 函数 可能被调用 多次
     * 防止被添加多次 拦截器 加一个 flags
     */
    private int interceptorsFlage = 0;


    private int interceptorsFlage2 = 0;

    //存放 跟 Logger有关系的 全部的类
    private ArrayList<String> OkHttpLoggerList = new ArrayList();

    //存放 全部类名字的 集合
    private  final List<String> AllClassNameList = new ArrayList<>();

    //存放 全部类的 集合
    public static volatile List<Class> mClassList = new ArrayList<>();

    @Override
    public synchronized void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            shared = new XSharedPreferences("Xposed.Okhttp.Cat", "config");
            shared.reload();
            InvokPackage = shared.getString("APP_INFO", "");
            MODEL = shared.getString("MODEL", "1");
            if (MODEL.equals("4")) {
                isShowStacktTrash = true;
            }
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
    private synchronized void HookAttach() {
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
                        }
                    }
                });
        XposedHelpers.findAndHookMethod(Application.class, "onCreate",
                new XC_MethodHook() {

                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        if(flag5==0) {
                            CLogUtils.e("Hook到    onCreate");
                            if (MODEL.equals("3") || MODEL.equals("4")) {
                                CLogUtils.e("使用了 通杀模式 ");
                                HookGetOutPushStream();
                                return;
                            }
                            HookOKClient();
                            flag5=1;
                        }
                    }

                });
    }


    /**
     * 判断 OkHttp是否存在 混淆 这步骤
     */
    private synchronized void HookOKClient() {
        try {
            //OkHttpBuilder =  Class.forName(ClassPath);
            if (OkHttpBuilder == null && flag3 == 0) {
                OkHttpBuilder = Class.forName("okhttp3.OkHttpClient$Builder", true, mLoader);
                //OkHttpClient = Class.forName("okhttp3.OkHttpClient", true, mLoader);
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
                HookAndInitProguardClass();
            }
        }
    }

    private synchronized void HookAndInitProguardClass() {
        //先拿到 app里面全部的class
        getAllClassName();
        //第一步 先开始 拿到 OkHttp 里面的 类  如Client 和 Builder
        initAllClass();
        getClientClass();
        getBuilder();

        HookBuilderConstructor();
    }

    /**
     * 初始化 需要的 class的 方法
     */
    private synchronized void initAllClass() {
        mClassList.clear();
        CLogUtils.e("开始 初始化全部的类  " + AllClassNameList.size());
        Class<?> MClass = null;
        for (int i=0;i<AllClassNameList.size(); i++) {
            try {
                MClass = Class.forName(AllClassNameList.get(i), false, mLoader);

            } catch (Exception e) {
                e.printStackTrace();
                //CLogUtils.e("initAllClass   "+e.getMessage()+"   index  "+i);
            }
            if(MClass!=null) {
                mClassList.add(MClass);
            }
        }

        CLogUtils.e("初始化全部类的个数   " + mClassList.size());
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
    private void getBuilder() {
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

    private void HookBuilderConstructor() {
        CLogUtils.e("开始 Hook构造  ");
        //混淆 以后 只能 Hook 构造 目的 是为了 拿到 object
        //尝试 Hook 构造
        XposedHelpers.findAndHookConstructor(OkHttpBuilder, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                CLogUtils.e("Hook 到 构造函数  OkHttpBuilder");
                AddInterceptors2(param);
            }
        });
        XposedHelpers.findAndHookConstructor(OkHttpClient, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                CLogUtils.e("Hook 到 构造函数  OkHttpClient");
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
        int StaticCount = 0;

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
            if (type.contains(Key.ListType) && Modifier.isFinal(field.getModifiers()) && Modifier.isStatic(field.getModifiers())) {
                //CLogUtils.e(" 复合 规则 该 Field是      " + field.getName() + " ");
                StaticCount++;
            }
        }
        //CLogUtils.e("field 符合标准   个数 是    " + typeCount);

        if (StaticCount >= 2 && typeCount == 6 && mClass.getInterfaces().length >= 2) {
            CLogUtils.e(" 该类的名字是  " + mClass.getName());
            return true;
        }
        // mFieldArrayList.clear();
        return false;
    }


    private void getAllClassName() {
        //保证每次初始化 之前 保证干净
        AllClassNameList.clear();
        CLogUtils.e("开始 获取全部的类名  ");
        try {
            //系统的 classloader是 Pathclassloader需要 拿到他的 父类 BaseClassloader才有 pathList
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

    private void getDexFileClassName(DexFile dexFile, List<String> classNameList) {
        //获取df中的元素  这里包含了所有可执行的类名 该类名包含了包名+类名的方式
        Enumeration<String> enumeration = dexFile.entries();
        while (enumeration.hasMoreElements()) {//遍历
            String className = enumeration.nextElement();
            //if (className.contains("okhttp3")||className.contains("okio")) {//在当前所有可执行的类里面查找包含有该包名的所有类
            classNameList.add(className);
//                if (className.contains("okhttp3.logging")) {
//                    OkHttpLoggerList.add(className);
//                }
            //  }
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
                    //随便 拿一个 添加 一个 log拦截器 进去
                    List interceptors = (List) XposedHelpers.getObjectField(param.thisObject, mFieldArrayList.get(0).getName());
                    CLogUtils.e("拿到 集合  ");
                    Object httpLoggingInterceptor = getHttpLoggingInterceptor();
                    if (httpLoggingInterceptor != null) {
                        CLogUtils.e("拿到 混淆 以后的  拦截器 实例  ");
                        interceptors.add(httpLoggingInterceptor);
                    } else {
                        CLogUtils.e("没有 拿到 拦截器   开始 动态代理  自动识别   ");
                        Object httpLoggingInterceptorImp = getHttpLoggingInterceptorImp();
                        if (httpLoggingInterceptorImp != null) {
                            interceptors.add(httpLoggingInterceptorImp);
                            CLogUtils.e("动态代理   拦截器 设置成功  ");
                        }
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
     *
     * @param mClass
     * @return
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
            CLogUtils.e("getHttpLoggingInterceptor  拦截器初始化出现异常  ClassNotFoundException  " + e.getMessage());
            //判断 是否是 混淆类型
            if (mFieldArrayList.size() == 2) {
                //混淆 无法 动态 加载
                return getOkHttpLoggingInterceptor();
            }
            // 没有 混淆 可以动态 加载
            return initLoggingInterceptor();
        } catch (InstantiationException e) {
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
        }
        return null;
    }

    @Nullable
    private Object getOkHttpLoggingInterceptor() {
        try {
            if (OkHttpLoggerList.size() != 0) {
                for (String stringName : OkHttpLoggerList) {
                    Class<?> aClass = Class.forName(stringName, true, mLoader);
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
                            StringBuffer TraceString = new StringBuffer();
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

                            FileUtils.SaveString(new SimpleDateFormat("MM-dd-hh-mm-ss").format(new Date()) + "\n" + "  " +
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


    private Object InitInterceptor(boolean isDexLoader) throws
            InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
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
