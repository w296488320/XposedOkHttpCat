package com.zx.Justmeplush.Hook;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.net.http.SslError;
import android.util.Log;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;

import com.zx.Justmeplush.config.Key;
import com.zx.Justmeplush.utils.CLogUtils;
import com.zx.Justmeplush.utils.ClassUtils;
import com.zx.Justmeplush.utils.ThreadUtils;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.HostNameResolver;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import dalvik.system.DexFile;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static com.zx.Justmeplush.utils.CLogUtils.TAG;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.newInstance;
import static de.robv.android.xposed.XposedHelpers.setObjectField;


public class JustHook implements IXposedHookLoadPackage {


    /**
     * 进行 注入的 app 名字
     */
    public static volatile String InvokPackage = null;
    //存放 全部类的 集合
    public static volatile List<Class> mClassList = new ArrayList<>();

    public static TrustManager tm = new X509TrustManager() {

        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    };
    //使用 共享 数据的 XSharedPreferences
    private XSharedPreferences shared;
    //别的进程的 context
    private Context mOtherContext;
    //别的进程的 mLoader
    private volatile ClassLoader mLoader = null;
    private List<String> classNameList = new ArrayList<String>();
    private XC_LoadPackage.LoadPackageParam lpparam;
    //OkHttp里面的 类
    private Class<?> OkHttpBuilder = null;
    private Class<?> OkHttpClient = null;

    public static javax.net.ssl.SSLSocketFactory getEmptySSLFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new ImSureItsLegitTrustManager()}, null);
            return sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            return null;
        }
    }

    //This function creates a ThreadSafeClientConnManager that trusts everyone!
    public static ClientConnectionManager getTSCCM(HttpParams params) {

        KeyStore trustStore;
        try {

            trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            SSLSocketFactory sf = new TrustAllSSLSocketFactory(trustStore);
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            registry.register(new Scheme("https", sf, 443));

            ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

            return ccm;

        } catch (Throwable e) {
            return null;
        }
    }

    //This function determines what object we are dealing with.
    public static ClientConnectionManager getCCM(Object o, HttpParams params) {

        String className = o.getClass().getSimpleName();

        if (className.equals("SingleClientConnManager")) {
            return getSCCM();
        } else if (className.equals("ThreadSafeClientConnManager")) {
            return getTSCCM(params);
        }

        return null;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        shared = new XSharedPreferences("com.zx.Justmeplush", "config");
        shared.reload();
        InvokPackage = shared.getString("APP_INFO", "");
        //先重启 选择 好 要进行Hook的 app
        if (InvokPackage == null || InvokPackage.equals("")) {
            return;
        } else {
            if (lpparam.packageName.equals(InvokPackage)) {
                CLogUtils.e("发现被Hook的App "+lpparam.packageName);
                this.lpparam = lpparam;
                HookAttach();
            }
        }
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

    void processHttpClientAndroidLib(ClassLoader classLoader) {
        /* httpclientandroidlib Hooks */
        /* public final void verify(String host, String[] cns, String[] subjectAlts, boolean strictWithSubDomains) throws SSLException */

        try {
            classLoader.loadClass("ch.boye.httpclientandroidlib.conn.ssl.AbstractVerifier");
            findAndHookMethod("ch.boye.httpclientandroidlib.conn.ssl.AbstractVerifier", classLoader, "verify",
                    String.class, String[].class, String[].class, boolean.class,
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                            return null;
                        }
                    });
        } catch (Throwable e) {
            // pass
        }
    }

    /**
     * hook  Attach方法
     */
    private synchronized void HookAttach() {

        XposedHelpers.findAndHookMethod(Application.class, "attach",
                Context.class,
                new XC_MethodHook() {

                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        mOtherContext = (Context) param.args[0];
                        mLoader = mOtherContext.getClassLoader();
                        //JustTrushMe 原方法
                        processOkHttp(mLoader);
                        processHttpClientAndroidLib(mLoader);
                        processXutils(mLoader);

                        JustMePlush();

                        boolean defaultProcess = getCurProcessName(mOtherContext).equals(mOtherContext.getPackageName());
                        if (defaultProcess) {
                            CLogUtils.e("Hook到    attach");
                            //在 oncteate的 执行完毕 这个时候 壳的 类以及初始化 完毕了
                            getAllClassNameAndInit();
                        }
                        CLogUtils.e("Hook执行完毕");
                    }
                });
        XposedHelpers.findAndHookMethod(Application.class, "onCreate",
                new XC_MethodHook() {
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);


                    }
                });
    }

    private void JustMePlush() {
        /* Apache Hooks */
        /* external/apache-http/src/org/apache/http/impl/client/DefaultHttpClient.java */
        /* public DefaultHttpClient() */
        try {
            findAndHookConstructor(DefaultHttpClient.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                    setObjectField(param.thisObject, "defaultParams", null);
                    setObjectField(param.thisObject, "connManager", getSCCM());
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
        }

        /* external/apache-http/src/org/apache/http/impl/client/DefaultHttpClient.java */
        /* public DefaultHttpClient(HttpParams params) */
        try {
            findAndHookConstructor(DefaultHttpClient.class, HttpParams.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                    setObjectField(param.thisObject, "defaultParams", (HttpParams) param.args[0]);
                    setObjectField(param.thisObject, "connManager", getSCCM());
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
        }

        /* external/apache-http/src/org/apache/http/impl/client/DefaultHttpClient.java */
        /* public DefaultHttpClient(ClientConnectionManager conman, HttpParams params) */
        try {
            findAndHookConstructor(DefaultHttpClient.class, ClientConnectionManager.class, HttpParams.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                    HttpParams params = (HttpParams) param.args[1];

                    setObjectField(param.thisObject, "defaultParams", params);
                    setObjectField(param.thisObject, "connManager", getCCM(param.args[0], params));
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
        }

        /* external/apache-http/src/org/apache/http/conn/ssl/SSLSocketFactory.java */
        /* public SSLSocketFactory( ... ) */
        try {
            findAndHookConstructor(SSLSocketFactory.class, String.class, KeyStore.class, String.class, KeyStore.class,
                    SecureRandom.class, HostNameResolver.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                            String algorithm = (String) param.args[0];
                            KeyStore keystore = (KeyStore) param.args[1];
                            String keystorePassword = (String) param.args[2];
                            SecureRandom random = (SecureRandom) param.args[4];

                            KeyManager[] keymanagers = null;
                            TrustManager[] trustmanagers = null;

                            if (keystore != null) {
                                keymanagers = (KeyManager[]) callStaticMethod(SSLSocketFactory.class, "createKeyManagers", keystore, keystorePassword);
                            }

                            trustmanagers = new TrustManager[]{new ImSureItsLegitTrustManager()};

                            setObjectField(param.thisObject, "sslcontext", SSLContext.getInstance(algorithm));
                            callMethod(getObjectField(param.thisObject, "sslcontext"), "init", keymanagers, trustmanagers, random);
                            setObjectField(param.thisObject, "socketfactory",
                                    callMethod(getObjectField(param.thisObject, "sslcontext"), "getSocketFactory"));
                        }

                    });
        } catch (Throwable e) {
            e.printStackTrace();
        }


        /* external/apache-http/src/org/apache/http/conn/ssl/SSLSocketFactory.java */
        /* public static SSLSocketFactory getSocketFactory() */
        try {
            findAndHookMethod("org.apache.http.conn.ssl.SSLSocketFactory", lpparam.classLoader,
                    "getSocketFactory", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    return (SSLSocketFactory) newInstance(SSLSocketFactory.class);
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
        }

        /* external/apache-http/src/org/apache/http/conn/ssl/SSLSocketFactory.java */
        /* public boolean isSecure(Socket) */
        try {
            findAndHookMethod("org.apache.http.conn.ssl.SSLSocketFactory", lpparam.classLoader, "isSecure", Socket.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    return true;
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
        }

        /* JSSE Hooks */
        /* libcore/luni/src/main/java/javax/net/ssl/TrustManagerFactory.java */
        /* public final TrustManager[] getTrustManager() */
        try {
            findAndHookMethod("javax.net.ssl.TrustManagerFactory", lpparam.classLoader, "getTrustManagers", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                    if (hasTrustManagerImpl()) {
                        Class<?> cls = findClass("com.android.org.conscrypt.TrustManagerImpl", lpparam.classLoader);

                        TrustManager[] managers = (TrustManager[]) param.getResult();
                        if (managers.length > 0 && cls.isInstance(managers[0]))
                            return;
                    }

                    param.setResult(new TrustManager[]{new ImSureItsLegitTrustManager()});
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
        }

        /* libcore/luni/src/main/java/javax/net/ssl/HttpsURLConnection.java */
        /* public void setDefaultHostnameVerifier(HostnameVerifier) */
        try {
            findAndHookMethod("javax.net.ssl.HttpsURLConnection", lpparam.classLoader,
                    "setDefaultHostnameVerifier",
                    HostnameVerifier.class, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            return null;
                        }
                    });

        } catch (Throwable e) {
            e.printStackTrace();
        }

        /* libcore/luni/src/main/java/javax/net/ssl/HttpsURLConnection.java */
        /* public void setSSLSocketFactory(SSLSocketFactory) */
        try {
            findAndHookMethod("javax.net.ssl.HttpsURLConnection", lpparam.classLoader, "setSSLSocketFactory", javax.net.ssl.SSLSocketFactory.class,
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            return null;
                        }
                    });
        } catch (Throwable e) {
            e.printStackTrace();
        }

        /* libcore/luni/src/main/java/javax/net/ssl/HttpsURLConnection.java */
        /* public void setHostnameVerifier(HostNameVerifier) */
        try {
            findAndHookMethod("javax.net.ssl.HttpsURLConnection", lpparam.classLoader, "setHostnameVerifier", HostnameVerifier.class,
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            return null;
                        }
                    });
        } catch (Throwable e) {
            e.printStackTrace();
        }


        /* WebView Hooks */
        /* frameworks/base/core/java/android/webkit/WebViewClient.java */
        /* public void onReceivedSslError(Webview, SslErrorHandler, SslError) */

        try {
            findAndHookMethod("android.webkit.WebViewClient", lpparam.classLoader, "onReceivedSslError",
                    WebView.class, SslErrorHandler.class, SslError.class, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            ((SslErrorHandler) param.args[1]).proceed();
                            return null;
                        }
                    });
        } catch (Throwable e) {
            e.printStackTrace();
        }

        /* frameworks/base/core/java/android/webkit/WebViewClient.java */
        /* public void onReceivedError(WebView, int, String, String) */

        try {
            findAndHookMethod("android.webkit.WebViewClient", lpparam.classLoader, "onReceivedError",
                    WebView.class, int.class, String.class, String.class, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            return null;
                        }
                    });
        } catch (Throwable e) {
            e.printStackTrace();
        }

        //SSLContext.init >> (null,ImSureItsLegitTrustManager,null)
        try {
            findAndHookMethod("javax.net.ssl.SSLContext", lpparam.classLoader, "init",
                    KeyManager[].class, TrustManager[].class, SecureRandom.class, new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                    param.args[0] = null;
                    param.args[1] = new TrustManager[]{new ImSureItsLegitTrustManager()};
                    param.args[2] = null;

                }
            });
        } catch (Throwable e) {
            CLogUtils.e("SSLContext init error  "+e.toString());

            e.printStackTrace();
        }



        /* Only for newer devices should we try to hook TrustManagerImpl */
        if (hasTrustManagerImpl()) {
            /* TrustManagerImpl Hooks */
            /* external/conscrypt/src/platform/java/org/conscrypt/TrustManagerImpl.java */

            /* public void checkServerTrusted(X509Certificate[] chain, String authType) */
            try {
                findAndHookMethod("com.android.org.conscrypt.TrustManagerImpl", lpparam.classLoader,
                        "checkServerTrusted", X509Certificate[].class, String.class,
                        new XC_MethodReplacement() {
                            @Override
                            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                                return 0;
                            }
                        });
            } catch (Throwable e) {
                e.printStackTrace();
            }

            /* public List<X509Certificate> checkServerTrusted(X509Certificate[] chain,
                                    String authType, String host) throws CertificateException */
            try {
                findAndHookMethod("com.android.org.conscrypt.TrustManagerImpl", lpparam.classLoader,
                        "checkServerTrusted", X509Certificate[].class, String.class,
                        String.class, new XC_MethodReplacement() {
                            @Override
                            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                                ArrayList<X509Certificate> list = new ArrayList<X509Certificate>();
                                return list;
                            }
                        });
            } catch (Throwable e) {
                e.printStackTrace();
            }


            /* public List<X509Certificate> checkServerTrusted(X509Certificate[] chain,
                                    String authType, SSLSession session) throws CertificateException */
            try {
                findAndHookMethod("com.android.org.conscrypt.TrustManagerImpl", lpparam.classLoader,
                        "checkServerTrusted", X509Certificate[].class, String.class,
                        SSLSession.class, new XC_MethodReplacement() {
                            @Override
                            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                                ArrayList<X509Certificate> list = new ArrayList<X509Certificate>();
                                return list;
                            }
                        });
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

    } // End Hooks

    /* Helpers */
    // Check for TrustManagerImpl class
    public boolean hasTrustManagerImpl() {

        try {
            Class.forName("com.android.org.conscrypt.TrustManagerImpl");
        } catch (ClassNotFoundException e) {
            return false;
        }
        return true;
    }

    //Create a SingleClientConnManager that trusts everyone!
    private static ClientConnectionManager getSCCM() {

        KeyStore trustStore;
        try {

            trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            SSLSocketFactory sf = new TrustAllSSLSocketFactory(trustStore);
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            registry.register(new Scheme("https", sf, 443));

            ClientConnectionManager ccm = new SingleClientConnManager(null, registry);

            return ccm;

        } catch (Throwable e) {
            return null;
        }
    }

    //This function determines what object we are dealing with.
//    public ClientConnectionManager getCCM(Object o, HttpParams params) {
//
//        String className = o.getClass().getSimpleName();
//
//        if (className.equals("SingleClientConnManager")) {
//            return getSCCM();
//        } else if (className.equals("ThreadSafeClientConnManager")) {
//            return getTSCCM(params);
//        }
//
//        return null;
//    }

    private void processXutils(ClassLoader classLoader) {
        try {
            classLoader.loadClass("org.xutils.http.RequestParams");
            findAndHookMethod("org.xutils.http.RequestParams", classLoader, "setSslSocketFactory", javax.net.ssl.SSLSocketFactory.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    param.args[0] = getEmptySSLFactory();
                }
            });
            findAndHookMethod("org.xutils.http.RequestParams", classLoader, "setHostnameVerifier", HostnameVerifier.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    param.args[0] = new ImSureItsLegitHostnameVerifier();
                }
            });
        } catch (Throwable e) {
        }
    }

    private void processOkHttp(ClassLoader classLoader) {



        /* hooking OKHTTP by SQUAREUP */
        /* com/squareup/okhttp/CertificatePinner.java available online @ https://github.com/square/okhttp/blob/master/okhttp/src/main/java/com/squareup/okhttp/CertificatePinner.java */
        /* public void check(String hostname, List<Certificate> peerCertificates) throws SSLPeerUnverifiedException{}*/
        /* Either returns true or a exception so blanket return true */
        /* Tested against version 2.5 */

        try {
            classLoader.loadClass("com.squareup.okhttp.CertificatePinner");
            findAndHookMethod("com.squareup.okhttp.CertificatePinner",
                    classLoader,
                    "check",
                    String.class,
                    List.class,
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                            return true;
                        }
                    });
        } catch (Throwable e) {
            // pass
        }

        //https://github.com/square/okhttp/blob/parent-3.0.1/okhttp/src/main/java/okhttp3/CertificatePinner.java#L144

        try {
            classLoader.loadClass("okhttp3.CertificatePinner");
            findAndHookMethod("okhttp3.CertificatePinner",
                    classLoader,
                    "check",
                    String.class,
                    List.class,
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                            return null;
                        }
                    });
        } catch (Throwable e) {
            // pass
        }

        //https://github.com/square/okhttp/blob/parent-3.0.1/okhttp/src/main/java/okhttp3/internal/tls/OkHostnameVerifier.java
        try {
            classLoader.loadClass("okhttp3.internal.tls.OkHostnameVerifier");
            findAndHookMethod("okhttp3.internal.tls.OkHostnameVerifier",
                    classLoader,
                    "verify",
                    String.class,
                    javax.net.ssl.SSLSession.class,
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                            return true;
                        }
                    });
        } catch (ClassNotFoundException e) {
            // pass
        }

        //https://github.com/square/okhttp/blob/parent-3.0.1/okhttp/src/main/java/okhttp3/internal/tls/OkHostnameVerifier.java
        try {
            classLoader.loadClass("okhttp3.internal.tls.OkHostnameVerifier");
            findAndHookMethod("okhttp3.internal.tls.OkHostnameVerifier",
                    classLoader,
                    "verify",
                    String.class,
                    java.security.cert.X509Certificate.class,
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                            return true;
                        }
                    });
        } catch (Throwable e) {
            // pass
        }

        //https://github.com/square/okhttp/blob/okhttp_4.2.x/okhttp/src/main/java/okhttp3/CertificatePinner.kt

        try {
            classLoader.loadClass("okhttp3.CertificatePinner");
            findAndHookMethod("okhttp3.CertificatePinner",
                    classLoader,
                    "check$okhttp",
                    String.class,
                    "kotlin.jvm.functions.Function0",
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                            return null;
                        }
                    });
        } catch (Throwable e) {
            // pass
        }
    }

    /**
     * 主要Hook这几个方法
     * <p>
     * //证书检测
     * builder.sslSocketFactory()
     * //域名验证
     * .hostnameVerifier()
     * //证书锁定
     * .certificatePinner()
     */
    private void HookOkHttpClient() {
        CLogUtils.e("开始执行 自识别 okHttp  Hook ");
        initAllClass();
        getClientClass();
        getBuilder();

        //方法 1   证书检测   2个 参数类型
        if (OkHttpBuilder != null) {
            Class SSLSocketFactoryClass = getClass("javax.net.ssl.SSLSocketFactory");
            Class X509TrustManagerClass = getClass("javax.net.ssl.X509TrustManager");


            //先拿到 参数类型的 类
            Method SslSocketFactoryMethod = getSslSocketFactoryMethod(SSLSocketFactoryClass, X509TrustManagerClass);
            if (SslSocketFactoryMethod != null) {
                CLogUtils.e("拿到  SslSocketFactoryMethod " + SslSocketFactoryMethod.getName());
                //需要先拿到方法名字
                try {
                    XposedHelpers.findAndHookMethod(OkHttpBuilder, SslSocketFactoryMethod.getName(),
                            SSLSocketFactoryClass,
                            X509TrustManagerClass,
                            new XC_MethodHook() {

                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    super.beforeHookedMethod(param);
                                    CLogUtils.e("Hook到 sslSocketFactory 2个参数类型 ");
                                    param.args[0] = getEmptySSLFactory();
                                    param.args[1] = new MyX509TrustManager();
                                    CLogUtils.e(" sslSocketFactory 2个参数类型  替换成功 ");
                                }
                            });
                } catch (Throwable e) {
                    e.printStackTrace();
                }

            } else {
                CLogUtils.e("没有拿到  SslSocketFactoryMethod ");
            }


            Method sslSocketFactoryMethodOneType = getSslSocketFactoryMethodOneType(SSLSocketFactoryClass);
            //方法 2  证书检测   1个 参数类型
            if (sslSocketFactoryMethodOneType != null) {
                CLogUtils.e("拿到  sslSocketFactoryMethodOneType   " + sslSocketFactoryMethodOneType.getName());
                //需要先拿到方法名字
                try {
                    XposedHelpers.findAndHookMethod(OkHttpBuilder, sslSocketFactoryMethodOneType.getName(),
                            SSLSocketFactoryClass,
                            new XC_MethodHook() {

                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    super.beforeHookedMethod(param);
                                    CLogUtils.e("Hook到 sslSocketFactory 1个参数类型 ");
                                    param.args[0] = getEmptySSLFactory();
                                    CLogUtils.e(" sslSocketFactory 1个参数类型  替换成功 ");
                                }
                            });
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            } else {
                CLogUtils.e("没有拿到  sslSocketFactoryMethodOneType ");
            }


            //这两个是为了防止魔改的okHttp
            Method sslSocketFactoryMethodOneTypeforClient = getSslSocketFactoryMethodOneTypeForClient(X509TrustManagerClass);
            //方法 2  证书检测   1个 参数类型
            if (sslSocketFactoryMethodOneTypeforClient != null) {
                CLogUtils.e("拿到  sslSocketFactoryMethodOneTypeforClient   " + sslSocketFactoryMethodOneTypeforClient.getName());
                //需要先拿到方法名字
                try {
                    XposedHelpers.findAndHookMethod(OkHttpClient, sslSocketFactoryMethodOneTypeforClient.getName(),
                            SSLSocketFactoryClass,
                            new XC_MethodHook() {

                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    super.beforeHookedMethod(param);
                                    CLogUtils.e("Hook到 sslSocketFactory 1个参数类型 ");
                                    param.args[0] = getEmptySSLFactory();
                                    CLogUtils.e(" sslSocketFactory 1个参数类型  替换成功 ");
                                }
                            });
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            } else {
                CLogUtils.e("没有拿到  sslSocketFactoryMethodOneTypeForClient ");
            }


            //域名 验证
            Class HostnameVerifierClass = getClass("javax.net.ssl.HostnameVerifier");
            if (HostnameVerifierClass != null) {
                CLogUtils.e("拿到  HostnameVerifier 类  ");
            }
            Method hostnameVerifierMethod = gethostnameVerifierMethod(HostnameVerifierClass);
            if (hostnameVerifierMethod != null) {
                CLogUtils.e(" 拿到 hostnameVerifierMethod  " + hostnameVerifierMethod.getName());
                //需要先拿到方法名字
                try {
                    XposedHelpers.findAndHookMethod(OkHttpBuilder, hostnameVerifierMethod.getName(),
                            HostnameVerifierClass,
                            new XC_MethodHook() {

                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    super.beforeHookedMethod(param);
                                    CLogUtils.e("Hook到 hostnameVerifier  ");
                                    param.args[0] = new MyHostnameVerifier();
                                    CLogUtils.e(" hostnameVerifier 替换成功 ");
                                }
                            });
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            } else {
                CLogUtils.e("没有拿到  hostnameVerifierMethod ");
            }


            //证书 锁定
            Class CertificatePinnerClass = getCertificatePinnerClass();
            if (CertificatePinnerClass != null) {
                CLogUtils.e("得到  CertificatePinner  名字是  " + CertificatePinnerClass.getName());


                Method CertificatePinnerCheckMethod = getCertificatePinnerCheckMethod(CertificatePinnerClass);
                Method okHttpCertificatePinnerCheckMethod = getOkHttpCertificatePinnerCheckMethod(CertificatePinnerClass);

                if (CertificatePinnerCheckMethod != null) {
                    CLogUtils.e("得到  CertificatePinnerCheckMethod  名字是  " + CertificatePinnerCheckMethod.getName());


                    try {
                        findAndHookMethod(CertificatePinnerClass,
                                CertificatePinnerCheckMethod.getName(),
                                String.class,
                                List.class,
                                new XC_MethodReplacement() {

                                    protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                                        return null;
                                    }
                                });
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }

                    try {
                        //将添加的全部证书验证都删掉
                        XposedBridge.hookAllConstructors(CertificatePinnerClass, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                super.afterHookedMethod(param);
                                Set pins = (Set)getObjectField(param.thisObject, "pins");
                                pins.clear();
                            }
                        });
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                } else {
                    CLogUtils.e("没有拿到  CertificatePinnerCheckMethod ");
                }
                if (okHttpCertificatePinnerCheckMethod != null) {
                    CLogUtils.e("拿到  okHttpCertificatePinnerCheckMethod    名字是 " + okHttpCertificatePinnerCheckMethod.getName());
                    try {
                        findAndHookMethod(OkHttpBuilder,
                                okHttpCertificatePinnerCheckMethod.getName(),
                                CertificatePinnerClass,
                                new XC_MethodReplacement() {
                                    protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                                        return null;
                                    }
                                });
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                } else {
                    CLogUtils.e("没有拿到  okHttpCertificatePinnerCheckMethod ");
                }
            } else {
                CLogUtils.e("没有拿到  CertificatePinnerClass ");
            }


            Class OkHostnameVerifierClass = OkHostnameVerifierClass();
            if (OkHostnameVerifierClass != null) {
                try {

                    CLogUtils.e("得到  OkHostnameVerifierClass  名字是  " + OkHostnameVerifierClass.getName());
                    //这个 有几率为 null
                    Class<?> HostnameVerifier = Class.forName("javax.net.ssl.HostnameVerifier", true, mLoader);
                    if (HostnameVerifier == null) {
                        CLogUtils.e("没有 得到 HostnameVerifier  ");
                    }
                    Method hostnameVerifierVerifyMethod = getHostnameVerifierVerifyMethod(HostnameVerifier);
                    if (hostnameVerifierVerifyMethod != null) {
                        CLogUtils.e("得到  hostnameVerifierVerifyMethod  名字是  " + hostnameVerifierVerifyMethod.getName());

                        try {
                            findAndHookMethod(OkHostnameVerifierClass,
                                    hostnameVerifierVerifyMethod.getName(),
                                    String.class,
                                    SSLSession.class,
                                    new XC_MethodReplacement() {

                                        protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {

                                            CLogUtils.e("Hook到   hostnameVerifierVerifyMethod  方法1 " + hostnameVerifierVerifyMethod.getName());
                                            return true;
                                        }
                                    });
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                        try {
                            findAndHookMethod(OkHostnameVerifierClass,
                                    hostnameVerifierVerifyMethod.getName(),
                                    String.class,
                                    X509Certificate.class,
                                    new XC_MethodReplacement() {

                                        protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                                            CLogUtils.e("Hook到   hostnameVerifierVerifyMethod  方法2" + hostnameVerifierVerifyMethod.getName());
                                            return true;
                                        }
                                    });
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    } else {
                        CLogUtils.e("没有 得到 hostnameVerifierVerifyMethod  ");
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                CLogUtils.e("没有找到 OkHostnameVerifierClass");
            }


        }


    }

    private Method getHostnameVerifierVerifyMethod(Class<?> hostnameVerifier) {
        Method[] declaredMethods = null;
        try {
            declaredMethods = hostnameVerifier.getDeclaredMethods();
        } catch (Throwable e) {
            CLogUtils.e("getHostnameVerifierVerifyMethod   hostnameVerifier ==null");
            e.printStackTrace();
        }
        for (Method method : declaredMethods) {
            if (method.getParameterTypes().length == 2) {
                if (method.getParameterTypes()[0].getName().equals(String.class.getName()) &&
                        method.getParameterTypes()[1].getName().equals(SSLSession.class.getName())) {
                    return method;
                }
            }
        }
        return null;
    }

    private Method getCertificatePinnerCheckMethod(Class certificatePinnerClass) {
        Method[] declaredMethods = certificatePinnerClass.getDeclaredMethods();
        for (Method method : declaredMethods) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 2) {
                if (parameterTypes[0].getName().equals(String.class.getName()) && parameterTypes[1].getName().equals(List.class.getName())) {
                    return method;
                }
            }
        }
        return null;
    }

    private Method getOkHttpCertificatePinnerCheckMethod(Class certificatePinnerClass) {
        Method[] declaredMethods = OkHttpBuilder.getDeclaredMethods();
        for (Method method : declaredMethods) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 1) {
                if (parameterTypes[0].getName().equals(certificatePinnerClass.getName())) {
                    return method;
                }
            }
        }
        return null;
    }

    private Class OkHostnameVerifierClass() {

//                public final class OkHostnameVerifier implements HostnameVerifier {
//                public static final OkHostnameVerifier INSTANCE = new OkHostnameVerifier();

//                private static final int ALT_DNS_NAME = 2;
//                private static final int ALT_IPA_NAME = 7;

        try {
            Class<?> HostnameVerifier = Class.forName("javax.net.ssl.HostnameVerifier", true, mLoader);

            if (HostnameVerifier == null) {
                CLogUtils.e("OkHostnameVerifierClass  HostnameVerifier  没有找到    ");
            } else {
                CLogUtils.e("找到了 HostnameVerifierClass     ");
            }
            for (Class mClass : mClassList) {
                int privateCount = 0;
                if (mClass.getInterfaces().length == 1 && mClass.getInterfaces()[0].getName().equals("javax.net.ssl.HostnameVerifier")) {
                    CLogUtils.e("找到接口类型是    HostnameVerifier      ");

                    //接口类型 是 HostnameVerifier 并且是 final类型
                    if (Modifier.isFinal(mClass.getModifiers())) {
                        Field[] declaredFields = mClass.getDeclaredFields();
                        CLogUtils.e("发现这个类是 final  开始 遍历 字段个数       field个数    " + declaredFields.length
                                + "  该类的名字是    " + mClass.getName());


                        // 三个变量都是 final和 static类型
                        if (declaredFields.length == 3) {
                            CLogUtils.e("发现这个类是 final   字段个数    为 三个    " + mClass.getName());

                            for (Field field : declaredFields) {
                                if (Modifier.isPrivate(field.getModifiers()) && Modifier.isStatic(field.getModifiers())) {
                                    privateCount++;
                                }
                            }
                            if (privateCount == 2) {
                                return mClass;
                            }
                            CLogUtils.e("Field 静态 并且 私有的个数 的个数 是       " + privateCount);
                        }
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            CLogUtils.e("OkHostnameVerifierClass  ClassNotFoundException  " + e.toString());
            e.printStackTrace();
        }
        return null;
    }

    private Class getCertificatePinnerClass() {
//            public final class CertificatePinner {
//            public static final CertificatePinner DEFAULT = (new CertificatePinner.Builder()).build();
//            private final Set<CertificatePinner.Pin> pins;
//            @Nullable
//            private final CertificateChainCleaner certificateChainCleaner;

        //本身是 final类型  三个变量 都是final类型
        //两个是 private 一个是 pubulic
        // 有一个遍历的类型是 set
        for (Class mClass : mClassList) {
            int privateCount = 0;
            int publicCount = 0;
            int SetTypeCount = 0;

            Field[] declaredFields = mClass.getDeclaredFields();
            //长度 是 3 本身是 final类型
            if (declaredFields.length == 3 && Modifier.isFinal(mClass.getModifiers())) {
                for (Field field : declaredFields) {
                    //私有 并且是 final类型
                    if (Modifier.isFinal(field.getModifiers()) && Modifier.isPrivate(field.getModifiers())) {
                        privateCount++;
                        if (field.getType().getName().equals(Set.class.getName())) {
                            SetTypeCount++;
                        }
                    }
                    if (Modifier.isFinal(field.getModifiers()) && Modifier.isPublic(field.getModifiers())) {
                        publicCount++;
                    }
                }
                if (publicCount == 1 && SetTypeCount == 1 && privateCount == 2) {
                    return mClass;
                }
            }
        }
        return null;
    }

    private Method gethostnameVerifierMethod(Class HostnameVerifierClass) {
        Method[] declaredMethods = OkHttpBuilder.getDeclaredMethods();
        for (Method declaredMethod : declaredMethods) {
            declaredMethod.setAccessible(true);
            Class<?>[] parameterTypes = declaredMethod.getParameterTypes();
            if (parameterTypes.length == 1) {
                if (parameterTypes[0].getName().equals(HostnameVerifierClass.getName())) {
                    return declaredMethod;
                }
            }
        }
        return null;
    }



    private Method getSslSocketFactoryMethodForClient(Class sslSocketFactoryClass, Class x509TrustManagerClass) {
        try {

            Method[] ClientdeclaredMethods = OkHttpClient.getDeclaredMethods();

            for (int i = 0; i < ClientdeclaredMethods.length; i++) {
                ClientdeclaredMethods[i].setAccessible(true);
                Class<?>[] parameterTypes = ClientdeclaredMethods[i].getParameterTypes();
                if (parameterTypes.length == 2) {
                    if (parameterTypes[0].getName().equals(sslSocketFactoryClass.getName()) &&
                            parameterTypes[1].getName().equals(x509TrustManagerClass.getName())) {
                        return ClientdeclaredMethods[i];
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    private Method getSslSocketFactoryMethodOneTypeForClient(Class sslSocketFactoryClass) {
        Method[] declaredMethods = OkHttpClient.getDeclaredMethods();
        try {
            for (int i = 0; i < declaredMethods.length; i++) {
                declaredMethods[i].setAccessible(true);
                Class<?>[] parameterTypes = declaredMethods[i].getParameterTypes();
                if (parameterTypes.length == 1) {
                    if (parameterTypes[0].getName().equals(sslSocketFactoryClass.getName())) {
                        return declaredMethods[i];
                    }
                }
//                if(declaredMethods[i].getName().equals("a")){
//                    CLogUtils.e("参数1 "+parameterTypes[0].getName());
//                    CLogUtils.e("参数1 "+sslSocketFactoryClass.getName());
//
//                }
            }

            //ClassUtils.getClassMethodInfo(OkHttpClient);

        } catch (Throwable e) {
            e.printStackTrace();
        }

        return null;
    }






    private Method getSslSocketFactoryMethod(Class sslSocketFactoryClass, Class x509TrustManagerClass) {
        Method[] declaredMethods = OkHttpBuilder.getDeclaredMethods();
        try {
            for (int i = 0; i < declaredMethods.length; i++) {
                declaredMethods[i].setAccessible(true);
                Class<?>[] parameterTypes = declaredMethods[i].getParameterTypes();
                if (parameterTypes.length == 2) {
                    if (parameterTypes[0].getName().equals(sslSocketFactoryClass.getName()) &&
                            parameterTypes[1].getName().equals(x509TrustManagerClass.getName())) {
                        return declaredMethods[i];
                    }
                }
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    private Method getSslSocketFactoryMethodOneType(Class sslSocketFactoryClass) {
        Method[] declaredMethods = OkHttpBuilder.getDeclaredMethods();
        try {
            for (int i = 0; i < declaredMethods.length; i++) {
                declaredMethods[i].setAccessible(true);
                Class<?>[] parameterTypes = declaredMethods[i].getParameterTypes();
                if (parameterTypes.length == 1) {
                    if (parameterTypes[0].getName().equals(sslSocketFactoryClass.getName())) {
                        return declaredMethods[i];
                    }
                }

            }


        } catch (Throwable e) {
            e.printStackTrace();
        }

        return null;
    }

    private Class getClass(String path) {
        if (path != null && !path.equals("")) {
            Class<?> aClass;
            try {
                aClass = Class.forName(path, true, mLoader);
                if (aClass == null) {
                    aClass = Class.forName(path);
                }
                if (aClass != null) {
                    return aClass;
                }
            } catch (ClassNotFoundException e) {
                CLogUtils.e("getclass 反射类 失败   " + e.toString());
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 混淆 以后 获取  集合并添加 拦截器的方法
     */
    private void getBuilder() {
        try {
            if (OkHttpClient == null) {
                CLogUtils.e("没有找到OkHttpClient  App可能存在被混淆  OkHttpClient==Null ");
                for(Class c:mClassList){
                    CLogUtils.e("可疑的Class名字是 "+c.getName());
                }
                return;
            }
            CLogUtils.e("开始执行 getBuilder");
            //开始查找 build
            for (Class builder : mClassList) {
                if (isBuilder(builder)) {
                    OkHttpBuilder = builder;
                }
            }
            if(OkHttpBuilder==null) {
                CLogUtils.e("没有找到 OkHttpBuilder");
                for(Class c:mClassList){
                    CLogUtils.e("可疑的Class名字是 "+c.getName());
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private boolean isBuilder(Class ccc) {

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
            CLogUtils.e("获取 Build error "+e.toString());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 获取 ClientCLass的方法
     */
    private void getClientClass() {
        try {
            if (mClassList.size() == 0) {
                CLogUtils.e("全部的 集合 mClassList  的个数 为 0  ");
                return;
            }
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
    }

    private boolean isClient(Class<?> mClass) {
        try {
            int typeCount = 0;
            int staticCount = 0;

            //getDeclaredFields 是个 获取 全部的
            Field[] fields = mClass.getDeclaredFields();

            for (Field field : fields) {
                field.setAccessible(true);
                String type = field.getType().getName();

                //6个 集合 6个final 特征
                if (type.contains(List.class.getName()) && Modifier.isFinal(field.getModifiers())) {
                    //CLogUtils.e(" 复合 规则 该 Field是      " + field.getName() + " ");
                    typeCount++;
                    if(Modifier.isStatic(field.getModifiers())){
                        staticCount++;
                    }
                }
            }

            if (typeCount == 6 && staticCount==2) {
                CLogUtils.e(" OkHttpClient该类的名字是  " + mClass.getName());
                return true;
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 初始化 需要的 class的 方法
     */
    private void initAllClass() {

        try {
            for (String path : classNameList) {
                //首先进行初始化
                try {
                    Class<?> aClass = Class.forName(path, false, mLoader);
                    if(aClass!=null) {
                        mClassList.add(aClass);
                    }
                } catch (Throwable e) {
                    CLogUtils.e("initAllClass  报错 路径实例化类  " + e.toString());
                    e.printStackTrace();
                }

            }
            CLogUtils.e("初始化  跟 OkHttp类的个数 " + mClassList.size());
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

    private synchronized void getAllClassNameAndInit() {
        CLogUtils.e("开始 获取全部的类名  ");

        classNameList.clear();

        ThreadUtils.runOnNonUIThread(() -> {
            try {
                //系统的 classloader是 Pathclassloader需要 拿到他的 父类 BaseClassloader才有 pathList
                Field pathListField = Objects.requireNonNull(mLoader.getClass().getSuperclass()).getDeclaredField("pathList");
                pathListField.setAccessible(true);
                Object dexPathList = pathListField.get(mLoader);
                Field dexElementsField = dexPathList.getClass().getDeclaredField("dexElements");
                dexElementsField.setAccessible(true);
                Object[] dexElements = (Object[]) dexElementsField.get(dexPathList);
                for (Object dexElement : dexElements) {
                    Field dexFileField = dexElement.getClass().getDeclaredField("dexFile");
                    dexFileField.setAccessible(true);
                    DexFile dexFile = (DexFile) dexFileField.get(dexElement);
                    getDexFileClassName(dexFile);
                }
                HookOkHttpClient();

            } catch (Throwable e) {
                CLogUtils.e("getAllClassNameAndInit error " + e.toString());
                e.printStackTrace();
            }
        });
    }

    private void getDexFileClassName(DexFile dexFile) {
        if(dexFile!=null) {
            //获取df中的元素  这里包含了所有可执行的类名 该类名包含了包名+类名的方式
            Enumeration<String> enumeration = dexFile.entries();
            while (enumeration.hasMoreElements()) {//遍历
                String className = enumeration.nextElement();
                if (className.contains("okhttp")) {//在当前所有可执行的类里面查找包含有该包名的所有类
                    classNameList.add(className);
                }
            }
        }
    }

    public static class MyX509TrustManager implements X509TrustManager {


        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }


        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }


        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    public static class MyHostnameVerifier implements HostnameVerifier {


        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    public static class ImSureItsLegitHostnameVerifier implements HostnameVerifier {


        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    public static class ImSureItsLegitTrustManager implements X509TrustManager {

        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }


        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }


        public List<X509Certificate> checkServerTrusted(X509Certificate[] chain, String authType, String host) throws CertificateException {
            ArrayList<X509Certificate> list = new ArrayList<X509Certificate>();
            return list;
        }


        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    /* This class creates a SSLSocket that trusts everyone. */
    public static class TrustAllSSLSocketFactory extends SSLSocketFactory {

        SSLContext sslContext = SSLContext.getInstance("TLS");

        public TrustAllSSLSocketFactory(KeyStore truststore) throws
                NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
            super(truststore);

            sslContext.init(null, new TrustManager[]{tm}, null);
        }


        public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
            return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
        }


        public Socket createSocket() throws IOException {
            return sslContext.getSocketFactory().createSocket();
        }
    }

    class MySSLSocketFactory extends javax.net.ssl.SSLSocketFactory {


        public String[] getDefaultCipherSuites() {
            return new String[0];
        }


        public String[] getSupportedCipherSuites() {
            return new String[0];
        }


        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
            return null;
        }


        public Socket createSocket(String host, int port) throws IOException {
            return null;
        }


        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
            return null;
        }


        public Socket createSocket(InetAddress host, int port) throws IOException {
            return null;
        }


        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
            return null;
        }
    }
}
