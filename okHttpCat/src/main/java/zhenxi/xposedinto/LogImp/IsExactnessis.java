package zhenxi.xposedinto.LogImp;

import android.location.Address;
import android.widget.RelativeLayout;

import java.io.Closeable;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.Inflater;

import zhenxi.xposedinto.utils.CLogUtils;

/**
 * @author Zhenxi on 2020-09-09
 */
class IsExactnessis {
    private ClassLoader mClassLoader;

    public IsExactnessis(ClassLoader classLoader) {
        mClassLoader = classLoader;
    }

    public boolean isChain(Class mClass, Class interceptorClass) {


        // interface Chain
        // 接口类型 并且名字包含父类
        try {
            if (mClass.getName().contains(interceptorClass.getName())) {
                CLogUtils.e("当前class的名字 " + mClass.getName());
            }
            return mClass.getName().contains(interceptorClass.getName())
                    && mClass.isInterface()
                    && mClass.getName().contains("$");
        } catch (Throwable e) {
            CLogUtils.e("isChain error " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean isRequest(Class aClass, Class httpUrlClass, Class HeadClass) {
//        @Nullable
//        final RequestBody body;
//        private volatile d cacheControl;
//        final Headers headers;
//        private String ipAddrStr;
//        final String method;
//        final Object tag;
//        final HttpUrl url;

        try {
            //私有构造无法拿到
            Constructor[] constructors = aClass.getConstructors();
            if (constructors.length == 0 && Modifier.isFinal(aClass.getModifiers())) {

                Field[] declaredFields = aClass.getDeclaredFields();
                int StringCount = 0;
                int httpUrlCount = 0;
                int HeadCount = 0;

                for (Field field : declaredFields) {
                    if (field.getType().getName().equals(httpUrlClass.getName())) {
                        httpUrlCount++;
                    }
                    if (field.getType().getName().equals(String.class.getName())) {
                        StringCount++;
                    }
                    if (field.getType().getName().equals(HeadClass.getName())) {
                        HeadCount++;
                    }
                }
//                if(aClass.getName().equals("okhttp3.Request")){
//                    CLogUtils.e("StringCount "+StringCount+" httpUrlCount "+httpUrlCount+" HeadCount "+HeadCount);
//                }
                return StringCount == 2 && httpUrlCount == 1 && HeadCount == 1 &&! aClass.getName().contains("$");
            }

        } catch (Throwable e) {
            CLogUtils.e("isRequest error " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean isRequestBodyClass(Class mClass, Class MediaTypeClass) {
        try {
            if (Modifier.isAbstract(mClass.getModifiers()) && mClass.getDeclaredFields().length == 0) {
                int conut = 0;
                int selfCount = 0;
                int longCount = 0;

                Method[] declaredMethods = mClass.getDeclaredMethods();
                for (Method method : declaredMethods) {
                    if (Modifier.isAbstract(method.getModifiers()) && method.getReturnType().getName().equals(void.class.getName())) {
                        conut++;
                    }
                    if (Modifier.isAbstract(method.getModifiers()) && method.getReturnType().getName().equals(MediaTypeClass.getName())) {
                        conut++;
                    }
                    if (method.getReturnType().getName().equals(mClass.getName())) {
                        selfCount++;
                    }
                    if (method.getReturnType().getName().equals(long.class.getName())) {
                        longCount++;
                    }
                }
                return conut == 2 && selfCount == 5 && longCount == 1;
            }
        } catch (Throwable e) {
            CLogUtils.e("isRequestBodyClass error "+e.toString());
            e.printStackTrace();
        }
        return false;
    }

    public boolean isMediaTypeClass(Class mClass) {

        try {
            Class<Pattern> patternClass = Pattern.class;

            if (Modifier.isFinal(mClass.getModifiers())) {
                Field[] declaredFields = mClass.getDeclaredFields();
                if (declaredFields.length != 0) {
                    int StringFinalType = 0;
                    int PatternTypeCount = 0;
                    int charSetReturn = 0;
                    for (Field field : declaredFields) {
                        if (field.getType().getName().equals(String.class.getName())
                                && Modifier.isFinal(field.getModifiers()) &&
                                Modifier.isPrivate(field.getModifiers())) {
                            StringFinalType++;
                        }
                        if (field.getType().getName().equals(patternClass.getName())) {
                            PatternTypeCount++;
                        }
                    }
                    if (StringFinalType >= 4 && PatternTypeCount == 2) {
                        Method[] declaredMethods = mClass.getDeclaredMethods();
                        for (Method method : declaredMethods) {
                            if (method.getReturnType().getName().equals(Charset.class.getName())) {
                                charSetReturn++;
                            }
                        }
                        return charSetReturn >= 2;
                    }
                }
            }
        } catch (Throwable e) {
            CLogUtils.e("isMediaTypeClass error " + e.toString());
            e.printStackTrace();
        }
        return false;
    }

    public boolean isHttpUrlClass(Class mClass) {

//        private static final char[] HEX_DIGITS =
//                {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
//        static final String USERNAME_ENCODE_SET = " \"':;<=>@[]^`{}|/\\?#";
//        static final String PASSWORD_ENCODE_SET = " \"':;<=>@[]^`{}|/\\?#";
//        static final String PATH_SEGMENT_ENCODE_SET = " \"<>^`{}|/\\?#";
//        static final String PATH_SEGMENT_ENCODE_SET_URI = "[]";
//        static final String QUERY_ENCODE_SET = " \"'<>#";
//        static final String QUERY_COMPONENT_REENCODE_SET = " \"'<>#&=";
//        static final String QUERY_COMPONENT_ENCODE_SET = " !\"#$&'(),/:;<=>?@[]\\^`{|}~";
//        static final String QUERY_COMPONENT_ENCODE_SET_URI = "\\^`{|}";
//        static final String FORM_ENCODE_SET = " \"':;<=>@[]^`{}|/\\?#&!$(),~";
//        static final String FRAGMENT_ENCODE_SET = "";
//        static final String FRAGMENT_ENCODE_SET_URI = " \"#<>\\^`{|}";

        //11个static final 一个 char[]

        try {
            if (Modifier.isFinal(mClass.getModifiers())) {
                int staticFinalStrCount = 0;
                int staticFinalCharCount = 0;

                Field[] declaredFields = mClass.getDeclaredFields();
                for (Field field : declaredFields) {
                    if (Modifier.isFinal(field.getModifiers())
                            && Modifier.isStatic(field.getModifiers())
                            && field.getType().getName().equals(String.class.getName())) {
                        staticFinalStrCount++;
                    }
                    if (Modifier.isFinal(field.getModifiers())
                            && Modifier.isStatic(field.getModifiers())
                            && field.getType().getName().equals(char[].class.getName())) {
                        staticFinalCharCount++;
                    }
                }
                return staticFinalStrCount == 11 && staticFinalCharCount == 1;
            }
        } catch (Throwable e) {
            CLogUtils.e("isHttpUrlClass error "+e.toString() );
            e.printStackTrace();
        }
        return false;
    }

    public boolean isHeaderClass(Class mClass) {
        try {
            if (Modifier.isFinal(mClass.getModifiers())) {
                //构造方法又2种    Headers(Builder builder)   Headers(String[] namesAndValues)
                if (mClass.getDeclaredFields().length == 1 && mClass.getDeclaredConstructors().length == 2) {
                    Field declaredField = mClass.getDeclaredFields()[0];
                    return declaredField.getType().getName().equals(String[].class.getName())
                            && Modifier.isFinal(declaredField.getModifiers())
                            && Modifier.isPrivate(declaredField.getModifiers());
                }
            }
        } catch (Throwable e) {
            CLogUtils.e("isHeaderClass error "+e.toString());

            e.printStackTrace();
        }
        return false;
    }

    public boolean isBufferClass(Class mClass) {
        try {
            if (Modifier.isFinal(mClass.getModifiers())
                    && mClass.getSuperclass().getName().equals(Object.class.getName())
                    && mClass.getInterfaces().length >= 3) {
                int StaticCount = 0;
                int LongConut = 0;
                Field[] declaredFields = mClass.getDeclaredFields();
                if (declaredFields.length >= 3) {
                    for (Field field : declaredFields) {
                        // CLogUtils.e("当前的类名 是  "+Mclass.getName());
                        if (Modifier.isFinal(field.getModifiers())
                                && Modifier.isStatic(field.getModifiers())
                                && (field.getType().getName().equals(byte[].class.getName())
                                || field.getType().getName().equals(int.class.getName()))
                        ) {
                            StaticCount++;
                        }
                        if (field.getType().getName().equals(long.class.getName())) {
                            LongConut++;
                        }
                    }
    //                if (mClass.getName().equals("okio.Buffer")) {
    //                    CLogUtils.e("StaticCount  " + StaticCount + "LongConut   " +
    //                            LongConut + " declaredFields 个数   " + declaredFields.length);
    //                }
                    return StaticCount == 2 && LongConut == 1;
                }
            }
        } catch (Throwable e) {
            CLogUtils.e("isBufferClass error "+e.toString());
            e.printStackTrace();
        }
        return false;
    }

    public boolean isBufferedSinkClass(Class MClass) {
        try {
            if (MClass.isInterface()) {
                Class[] interfaces = MClass.getInterfaces();
                boolean isHaveWritableByteChannel = false;
                if (interfaces.length >= 1) {
                    for (Class lclass : interfaces) {
                        if (lclass.getName().equals(WritableByteChannel.class.getName())) {
                            isHaveWritableByteChannel = true;
                        }
                    }
                }
                return isHaveWritableByteChannel && MClass.getDeclaredFields().length == 0;
            }
        } catch (Throwable e) {
            CLogUtils.e("isBufferedSinkClass error "+e.toString());
            e.printStackTrace();
        }
        return false;
    }

    public boolean isGzipSourceClass(Class mClass) {
//        private static final byte FHCRC = 1;
//        private static final byte FEXTRA = 2;
//        private static final byte FNAME = 3;
//        private static final byte FCOMMENT = 4;
//
//        private static final byte SECTION_HEADER = 0;
//        private static final byte SECTION_BODY = 1;
//        private static final byte SECTION_TRAILER = 2;
//        private static final byte SECTION_DONE = 3;
        /** Checksum used to check both the GZIP header and decompressed body. */
//        private final CRC32 crc = new CRC32();
//        private final Inflater inflater;

        try {
            if (Modifier.isFinal(mClass.getModifiers())) {
                Field[] declaredFields = mClass.getDeclaredFields();

                int byteCount = 0;
                int InflaterCount = 0;
                int CRC32Count = 0;

                for (Field field : declaredFields) {

//                    if (mClass.getName().equals("okio.GzipSource")) {
//                        CLogUtils.e("字段类型 "+field.getType()+
//                                " 名字 "+field.getName()+
//                                " isFinal "+Modifier.isFinal(mClass.getModifiers())+
//                                " isStatic "+Modifier.isStatic(mClass.getModifiers())+
//                                " isPrivate "+Modifier.isPrivate(mClass.getModifiers())
//                        );
//                    }

                    if (field.getType().getName().equals(byte.class.getName())

                            && Modifier.isFinal(field.getModifiers())
                            && Modifier.isStatic(field.getModifiers())
                            && Modifier.isPrivate(field.getModifiers())) {
                        byteCount++;
                        continue;
                    }
                    if (field.getType().getName().equals(CRC32.class.getName())
                            && Modifier.isFinal(field.getModifiers())
                            && Modifier.isPrivate(field.getModifiers())
                    ) {
                        CRC32Count++;
                        continue;

                    }
                    if (field.getType().getName().equals(Inflater.class.getName())
                            && Modifier.isFinal(field.getModifiers())
                            && Modifier.isPrivate(field.getModifiers())
                    ) {
                        InflaterCount++;
                    }
                }
//                if (mClass.getName().equals("okio.GzipSource")) {
//                    CLogUtils.e("byteCount " + byteCount + "  CRC32Count  " + CRC32Count + " InflaterCount " + InflaterCount);
//                    CLogUtils.e("总共字段个数 " + declaredFields.length);
//                }
                return byteCount == 8 && CRC32Count == 1 && InflaterCount == 1;
            }
        } catch (Throwable e) {
            CLogUtils.e("isGzipSourceClass error " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean isSourceClass(Class mClass) {

        try {
            if (mClass.isInterface()) {
                Method[] declaredMethods = mClass.getDeclaredMethods();
                if (declaredMethods.length == 3) {
                    int returnTpye = 0;
                    int longTpye = 0;

                    for (Method method : declaredMethods) {
                        if (method.getReturnType().getName().equals(void.class.getName())) {
                            returnTpye++;
                        }
                        if (method.getReturnType().getName().equals(long.class.getName())
                                && method.getParameterTypes().length == 2
                                && method.getParameterTypes()[1].getName().equals(long.class.getName())
                        ) {
                            longTpye++;
                        }
                    }
                    return returnTpye == 1 && longTpye == 1;
                }
            }
        } catch (Throwable e) {
            CLogUtils.e("isSourceClass 失败 " + e.toString());
            e.printStackTrace();
        }
        return false;
    }

    public boolean isResponseBodyClass(Class mClass,Class MediaTypeClass) {

        try {
            Field[] declaredFields = mClass.getDeclaredFields();
            if (declaredFields.length == 1 && declaredFields[0].getType().getName().equals(Reader.class.getName())) {
                int MediaTypeCount = 0;
                Method[] declaredMethods = mClass.getDeclaredMethods();
                for (Method method : declaredMethods) {
                    if (method.getReturnType().getName().equals(MediaTypeClass.getName())) {
                        MediaTypeCount++;
                    }
                }
                if (MediaTypeCount >= 1) {
                    return true;
                }
            }
        } catch (Throwable e) {
            CLogUtils.e("isResponseBodyClass error "+e.toString());

            e.printStackTrace();
        }
        return false;
    }

    public boolean isConnection(Class aClass, Class RouteClass) {
        try {
            if(aClass.isInterface()){
                Method[] declaredMethods = aClass.getDeclaredMethods();
                int RouteClassCount=0 ;
                for(Method method:declaredMethods){
                    if(method.getReturnType().getName().equals(RouteClass.getName())
                            &&method.getParameterTypes().length==0
                    ){
                        RouteClassCount++;
                    }
                }
                return RouteClassCount == 1 && declaredMethods.length <= 5;
            }
        } catch (Throwable e) {
            CLogUtils.e("isConnection error "+e.toString());
            e.printStackTrace();
        }
        return false;
    }

    public boolean isProtocol(Class mClass) {
        try {

            if (mClass.isEnum()) {

                Method[] declaredMethods = mClass.getDeclaredMethods();
                int staticCount=0;
                for (Method method : declaredMethods) {
                    if(method.getParameterTypes().length==1) {
                        if (method.getReturnType().getName().equals(mClass.getName()) &&
                                Modifier.isPublic(method.getModifiers()) &&
                                Modifier.isStatic(method.getModifiers()) &&
                                method.getParameterTypes()[0].getName().equals(String.class.getName())&&
                                mClass.getConstructors().length==0
                        ) {
                            staticCount++;
                        }
                    }
                }

                if(staticCount==1) {
                    Field[] declaredFields = mClass.getDeclaredFields();
                    int selfCount = 0;
                    for (Field field : declaredFields) {
                        if (field.getType().getName().equals(mClass.getName())) {
                            selfCount++;
                        }
                    }
                    if (selfCount >= 3) {
                        return true;
                    }
                }
            }
        } catch (Throwable e) {
            CLogUtils.e("isProtocol error "+e.toString());

            e.printStackTrace();
        }
        return false;
    }

    public boolean isRouteClass(Class mClass) {
        try {
            if (Modifier.isFinal(mClass.getModifiers())) {
                Field[] declaredFields = mClass.getDeclaredFields();
                int AdressCount=0;
                int ProxyCount=0;

                for(Field field:declaredFields){
                    if(field.getType().getName().equals(InetSocketAddress.class.getName())
                    &&Modifier.isFinal(field.getModifiers())
                    ){
                        AdressCount++;
                    }
                    if(field.getType().getName().equals(Proxy.class.getName())
                            &&Modifier.isFinal(field.getModifiers())
                    ){
                        ProxyCount++;
                    }
                }
                return AdressCount == 1 && ProxyCount == 1 && declaredFields.length <= 3;
            }
        } catch (Throwable e) {
            CLogUtils.e("isRouteClass error "+e.toString());

            e.printStackTrace();
        }
        return false;
    }
}
