package zhenxi.xposedinto.LogImp;

import android.util.Log;

import java.io.EOFException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import zhenxi.xposedinto.XpHook.Hook;
import zhenxi.xposedinto.utils.CLogUtils;

import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;

/**
 * Created by ZhenXi
 * 2019/8/22
 * <p>
 * 版本号
 * implementation 'com.squareup.okhttp3:logging-interceptor:3.11.0'
 */
public class LogInterceptorImp implements InvocationHandler {
    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private static final int HTTP_CONTINUE = 100;


    private static Class MediaTypeClass;
    private static Class HttpUrlClass;
    private static Class HeaderClass;
    private static Class BufferClass;
    private static Class GzipSourceClass;

    private static Class SourceClass;


    private static Class BufferedSinkClass;
    private static Class ConnectionClass;

    private static Class ProtocolClass;

    private static Class RouteClass;

    private static Class ResponseBodyClass;

    private static Class ChainClass;
    private static Class RequestClass;


    private static Class RequestBodyClass;
    /**
     * 记录是否需要初始化的字段 ，反正多次初始化降低效率
     */
    private static boolean isInit = false;
    /**
     * 方便在异常中打印
     */

    public synchronized static boolean init(ArrayList<Class> mClassList, Class interceptorClass, ClassLoader classLoader) {
        try {
            if (isInit) {
                return true;
            }

            IsExactnessis logInterceptorImp = new IsExactnessis(classLoader);
            CLogUtils.e("开始第一轮查找");

            Method[] declaredMethods = interceptorClass.getDeclaredMethods();
            if (declaredMethods.length == 1) {
                ChainClass = declaredMethods[0].getParameterTypes()[0];
                CLogUtils.e("找到 ChainClass ");
            }
            for (Class mClass : mClassList) {
                if (logInterceptorImp.isMediaTypeClass(mClass)) {
                    CLogUtils.e("找到 MediaTypeClass " + mClass.getName());
                    MediaTypeClass = mClass;
                } else if (logInterceptorImp.isHttpUrlClass(mClass)) {
                    CLogUtils.e("找到 HttpUrlClass  " + mClass.getName());
                    HttpUrlClass = mClass;
                } else if (logInterceptorImp.isHeaderClass(mClass)) {
                    CLogUtils.e("找到 HeaderClass  " + mClass.getName());
                    HeaderClass = mClass;
                } else if (logInterceptorImp.isBufferClass(mClass)) {
                    CLogUtils.e("找到 BufferClass  " + mClass.getName());
                    BufferClass = mClass;
                } else if (logInterceptorImp.isBufferedSinkClass(mClass)) {
                    CLogUtils.e("找到 BufferedSinkClass  " + mClass.getName());
                    BufferedSinkClass = mClass;
                } else if (logInterceptorImp.isGzipSourceClass(mClass)) {
                    CLogUtils.e("找到 GzipSourceClass  " + mClass.getName());
                    GzipSourceClass = mClass;
                } else if (logInterceptorImp.isSourceClass(mClass)) {
                    CLogUtils.e("找到 SourceClass  " + mClass.getName());
                    SourceClass = mClass;
                } else if (logInterceptorImp.isRouteClass(mClass)) {
                    CLogUtils.e("找到 isRouteClass  " + mClass.getName());
                    RouteClass = mClass;
                } else if (logInterceptorImp.isProtocol(mClass)) {
                    CLogUtils.e("找到 ProtocolClass  " + mClass.getName());
                    ProtocolClass = mClass;
                }
            }
            CLogUtils.e("开始第二轮查找");
            for (Class mClass : mClassList) {

                if (logInterceptorImp.isRequestBodyClass(mClass, MediaTypeClass)) {
                    RequestBodyClass = mClass;
                    CLogUtils.e("找到 RequestBodyClass  " + mClass.getName());
                } else if (logInterceptorImp.isRequest(mClass, HttpUrlClass, HeaderClass)) {
                    RequestClass = mClass;
                    CLogUtils.e("找到 RequestClass    " + mClass.getName());
                } else if (logInterceptorImp.isResponseBodyClass(mClass, MediaTypeClass)) {
                    ResponseBodyClass = mClass;
                    CLogUtils.e("找到 ResponseBody    " + mClass.getName());
                } else if (logInterceptorImp.isConnection(mClass, RouteClass)) {
                    CLogUtils.e("找到 ConnectionClass  " + mClass.getName());
                    ConnectionClass = mClass;
                }
            }
            if (RequestClass != null
                    && RequestBodyClass != null
                    && ChainClass != null
                    && SourceClass != null
                    && GzipSourceClass != null
                    && BufferedSinkClass != null
                    && BufferClass != null
                    && HeaderClass != null
                    && HttpUrlClass != null
                    && MediaTypeClass != null
                    && ConnectionClass != null
                    && ResponseBodyClass != null
                    && ProtocolClass != null
                    && RouteClass != null
            ) {

                isInit = true;
                return true;
            }
        } catch (Throwable e) {
            CLogUtils.e("init 时候 出现异常  " + e.toString());
            return false;
        }
        return false;
    }

    private static long stringToLong(String s) {
        if (s == null) return -1;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        method.setAccessible(true);
        try {
            if(args==null||args.length==0||method.getName().equals("hashCode")){
                //当调用该对象实例里面的其他方法适合直接return掉
                //比如HashCode 方法
                return proxy.hashCode();
            }
            //里面只有 一个方法
            Object ChainObject = args[0];

            StringBuilder RequestUrlStr = new StringBuilder();

            Object connection = InvokeConnection(ChainObject);


            Object RequestObject = getRequestObject(ChainObject, RequestClass);
            CLogUtils.NetLogger(" ");

            //请求的 Url信息
            RequestUrlStr.append("请求Url --> ").append("\n").
                    append(getMethodInRequest(RequestObject)).
                    append("  ").append(getUrlInRequest(RequestObject)).
                    append((connection != null ? " " + getProtocol(connection) : "")).
                    append("\n");

            CLogUtils.NetLogger(RequestUrlStr.toString());


//            //打印Length和type
//            if(requestBodyObject!=null) {
//                Object contentType = getContentTypeMethodAndInvokeFromRequestBody(requestBodyObject);
//                if(contentType!=null) {
//                    mStringBuffer.append("Content-Type: ").append(contentType);
//                }
//                Long contentLength = getContentLengthMethodAndInvokeFromRequestBody(requestBodyObject);
//                if(contentLength!=-1){
//                    mStringBuffer.append("Content-Length: ").append(contentLength);
//                }
//            }
            StringBuilder RequestHeadersBuilderStr = new StringBuilder();
            RequestHeadersBuilderStr.append("请求头部信息:").append("\n");
            String RequestHeadersStr = getHeadersToStringMethodAndInvoke(HeaderClass, RequestObject);
            //直接调用toString方法打印全部的Header
            RequestHeadersBuilderStr.append(RequestHeadersStr);
            //打印请求头部信息
            CLogUtils.NetLogger(RequestHeadersBuilderStr.toString());
            CLogUtils.e(" ");

            Object RequestHeadersObject = getHeadersInRe(RequestObject);

            StringBuilder RequestBodyStr = new StringBuilder();
            //判断是否含有RequestBody,先拿到然后判断是否为Null
            Object requestBodyObject = getRequestBodyObject(RequestObject, RequestBodyClass);
            //是否存在Body
            if (requestBodyObject == null) {
                RequestBodyStr.append("Get 请求 Body == null --> END \n");
            } else if (bodyHasUnknownEncoding(RequestHeadersObject)) {
                //是否请求Body支持解码
                RequestBodyStr.append("请求 Body 不支持解码  --> END \n");
            } else {
                RequestBodyStr.append("\n");
                //存在Body
                Object buffer = getBufferObject();

                invokeWriteTo(requestBodyObject, buffer, BufferedSinkClass);

                //默认是 U8解码
                Charset charset = UTF8;
                //MediaType 类型
                Object contentType = getContentTypeMethodAndInvokeFromBody(requestBodyObject);
                if (contentType != null) {
                    charset = getMediaTypeCharSetMethodAndInvoke(contentType, UTF8);
                }
                if (isPlaintext(buffer)) {
                    //logger.log(buffer.readString(charset));
                    RequestBodyStr.append(getReadStringAndInvoke(buffer, charset)).append("\n");
//                    logger.log("--> END " + request.method()
//                            + " (" + requestBody.contentLength() + "-byte body)");
                    RequestBodyStr.append("--> END ").append(getMethodInRequest(RequestObject)).append(" (")
                            .append(getContentLengthMethodAndInvokeFromRequestBody(requestBodyObject)).append("-byte body)").append("\n");
                } else {
//                    logger.log("--> END " + request.method() + " (binary "
//                            + requestBody.contentLength() + "-byte body omitted)");
                    RequestBodyStr.append("--> END ").append(getMethodInRequest(RequestObject)).append(" (binary ")
                            .append(getContentLengthMethodAndInvokeFromRequestBody(requestBodyObject)).append("-byte body omitted)").append("\n");
                }
            }
            //打印 Body
            CLogUtils.NetLogger(RequestBodyStr.toString());

            StringBuilder ResponseUrlsStr = new StringBuilder();

            long startNs = System.nanoTime();
            Object response;
            try {
                //response = chain.proceed(request);
                response = getResponseObject(ChainObject, RequestObject);
            } catch (Throwable e) {
                ResponseUrlsStr.append("<-- HTTP FAILED: ").append(e.toString());
                throw e;
            }

            long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

            Object responseBody = getResponseBodyObject(response, ResponseBodyClass);


            long contentLength = getResponseBodyContentLengthMethodAndInvoke(responseBody);

            String bodySize = contentLength != -1 ? contentLength + "-byte" : "unknown-length";

//            logger.log("<-- "
//                    + response.code()
//                    + (response.message().isEmpty() ? "" : ' ' + response.message())
//                    + ' ' + response.request().url()
//                    + " (" + tookMs + "ms" + (!logHeaders ? ", " + bodySize + " body" : "") + ')');
            ResponseUrlsStr.append("响应Url:<--  \n")
                    .append(getResponseCode(response))
                    .append(getResponseMessage(response).isEmpty() ? "" : ' ' + getResponseMessage(response))
                    .append(' ').append(getUrlInRequest(RequestObject))
                    .append("  ( ").append(tookMs).append(" ms ").append(bodySize).append(" body ").append(" ) ").append("\n");

            CLogUtils.NetLogger(ResponseUrlsStr.toString());

            Object responseHeaders = getHeadersInRe(response);

            //开始打印响应的头
            String responseHeaderStr = getHeadersToStringMethodAndInvoke(HeaderClass, response);

            CLogUtils.NetLogger("响应头部信息: \n"+responseHeaderStr);

            StringBuilder ResponseBodyStr = new StringBuilder();
            ResponseBodyStr.append("响应体: ");
            //判断响应Body是否存在或者可解码
            if (!ResponseHasBody(response)) {
                //不存在Body
                ResponseBodyStr.append("\n 不存在响应体 <-- END HTTP");
            } else if (bodyHasUnknownEncoding(responseHeaders)) {
                ResponseBodyStr.append("\n<--响应体不支持解码 END HTTP (encoded body omitted)");
            } else {
//                BufferedSource source = responseBody.source();
//                source.request(Long.MAX_VALUE); // Buffer the entire body.
//                Buffer buffer = source.getBuffer();
//
//                Long gzippedLength = null;
//                if ("gzip".equalsIgnoreCase(headers.get("Content-Encoding"))) {
//                    gzippedLength = buffer.size();
//                    try (GzipSource gzippedResponseBody = new GzipSource(buffer.clone())) {
//                        buffer = new Buffer();
//                        buffer.writeAll(gzippedResponseBody);
//                    }
//                }
                Object source = getSourceAndInvoke(responseBody);
                getBufferedSourceRequestMethodAndInvoke(source, Long.MAX_VALUE);
                Object buffer = getBufferMethodInBufferedSourceAndInvoke(source, BufferClass);

//                    try (GzipSource gzippedResponseBody = new GzipSource(buffer.clone())) {
//                        buffer = new Buffer();
//                        buffer.writeAll(gzippedResponseBody);
//                    }
                Long gzippedLength = null;
                if ("gzip".equalsIgnoreCase(getHeadersGetMethodAndInvoke(responseHeaders, "Content-Encoding"))) {
                    gzippedLength = getBufferSize(buffer);
                    Object gzipSourceObject = null;
                    try {
                        gzipSourceObject = getGzipSourceObject(getBuffCloneAndInvoke(buffer));

                        buffer = getBufferObject();
                        getBuffWirteAllAndInvoke(buffer, gzipSourceObject);
                    } catch (Throwable throwable) {
                        CLogUtils.e("getGzipSourceObject error " + throwable.toString());
                    } finally {
                        if (gzipSourceObject != null) {
                            //回收
                            getGzipSourceCloseAndInvoke(gzipSourceObject);
                        }
                    }

                }

                Charset charset = UTF8;
                Object contentType = getContentTypeMethodAndInvokeFromBody(responseBody);
                if (contentType != null) {
                    charset = getMediaTypeCharSetMethodAndInvoke(contentType, UTF8);
                }
                if (!isPlaintext(buffer)) {
                    ResponseBodyStr.append("\n").append("<-- END HTTP (binary ").append(getBufferSize(buffer)).append(" -byte body omitted)");
                    return response;
                }
                if (contentLength != 0) {
                    ResponseBodyStr.append("\n").append(getBuffReadStringAndInvoke(getBuffCloneAndInvoke(buffer), charset));
                }
                if (gzippedLength != null) {
                    ResponseBodyStr.append("\n").append("<-- END HTTP (").append(getBufferSize(buffer)).append("-byte, ")
                            .append(gzippedLength).append("-gzipped-byte body)");
                } else {
                    ResponseBodyStr.append("\n").append("<-- END HTTP (").append(getBufferSize(buffer)).append("-byte body)");
                }
            }

            CLogUtils.NetLogger("\n" + ResponseBodyStr.toString());
            CLogUtils.NetLogger(" ");
            CLogUtils.NetLogger(" ");

            return response;

        } catch (Throwable e) {
            //出现异常 统一 打印
            CLogUtils.e("发现异常  " + e.toString());

            CLogUtils.e(Log.getStackTraceString(e));

//            for (StackTraceElement stackTraceElement :  e.getStackTrace()) {
//                CLogUtils.e("方法名字    " + stackTraceElement.getMethodName() + "------  行数    " + stackTraceElement.getLineNumber());
//            }
            System.exit(0);

        }
        return null;
    }

    private String getProtocol(Object connection) throws Exception {
        Method[] declaredMethods = connection.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (method.getReturnType().getName().equals(ProtocolClass.getName())
                    && method.getParameterTypes().length == 0
            ) {
                method.setAccessible(true);
                return method.invoke(connection).toString();
            }
        }
        throw new Exception("getProtocol  失败 ");
    }

    private Object InvokeConnection(Object chainObject) throws Exception {
        Method[] declaredMethods = chainObject.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (method.getReturnType().getName().equals(ConnectionClass.getName())
                    && method.getParameterTypes().length == 0
            ) {
                method.setAccessible(true);
                return method.invoke(chainObject);
            }
        }
        throw new Exception("InvokeConnection  失败 ");
    }

    private String getUrlInRequest(Object requestObject) throws Exception {
        Method[] declaredMethod = requestObject.getClass().getDeclaredMethods();
        for (Method method : declaredMethod) {
            if (method.getReturnType().getName().equals(HttpUrlClass.getName())
                    && method.getParameterTypes().length == 0
            ) {
                method.setAccessible(true);
                try {
                    return method.invoke(requestObject).toString();
                } catch (Throwable e) {
                    e.printStackTrace();
                    CLogUtils.e("getUrlInRequest error " + e.toString());
                }
            }
        }
        throw new Exception("getUrlInRequest  失败 ");
    }


    private String getBuffReadStringAndInvoke(Object buff, Object args1) throws Exception {
        Method[] declaredMethods = buff.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (method.getReturnType().getName().equals(String.class.getName())
                    && method.getParameterTypes().length == 1
                    && method.getParameterTypes()[0].getName().equals(Charset.class.getName())
            ) {
                method.setAccessible(true);
                return (String) method.invoke(buff, args1);
            }
        }
        throw new Exception("getBuffReadStringAndInvoke  失败 ");
    }

    private void getBuffWirteAllAndInvoke(Object buffer, Object arg1) throws Exception {
        if (arg1 == null) {
            throw new Exception("getBuffWirteAllAndInvoke  arg1 ==null  ");
        }
        Method[] declaredMethods = buffer.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (method.getReturnType().getName().equals(long.class.getName())
                    && method.getParameterTypes().length == 1
                    && method.getParameterTypes()[0].getName().equals(SourceClass.getName())
            ) {
                method.setAccessible(true);
                method.invoke(buffer, arg1);
                return;
            }
        }
        throw new Exception("getBuffWirteAllAndInvoke  失败 ");
    }

    private void getGzipSourceCloseAndInvoke(Object gzipSourceObject) throws Exception {
//        public void close() throws IOException
        Method[] declaredMethods = gzipSourceObject.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (method.getReturnType().getName().equals(void.class.getName())
                    && Modifier.isPublic(method.getModifiers())
                    && method.getParameterTypes().length == 0
            ) {
                method.setAccessible(true);
                method.invoke(gzipSourceObject);
                return;
            }
        }
        throw new Exception("getGzipSourceCloseAndInvoke  失败 ");
    }

    private Object getGzipSourceObject(Object buffClone) throws Exception {
        return GzipSourceClass.getConstructor(SourceClass).newInstance(buffClone);
    }

    /**
     * 调用buff里面Clone 方法
     *
     * @param buffer 当前Buff对象
     */
    private Object getBuffCloneAndInvoke(Object buffer) throws Exception {

//        public Buffer clone() ;
//        public Buffer buffer()
//        public Buffer getBuffer()

//        三个方法可能会冲突,后面两个方法返回的都是this可以用 对象的hashcode进行判断
        Method[] declaredMethods = buffer.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (method.getReturnType().getName().equals(buffer.getClass().getName())
                    && method.getParameterTypes().length == 0
            ) {
//                CLogUtils.e("当前方法名字 "+method.getName());
                method.setAccessible(true);
                try {
//                    2020-09-11 19:40:52.215 26522-26703/com.sup.android.superb E/XposedInto: 当前HashCode 2051150842  原始hashcode 2051150842
//                    2020-09-11 19:40:52.216 26522-26703/com.sup.android.superb E/XposedInto: 当前方法名字 clone
//                    2020-09-11 19:40:52.216 26522-26703/com.sup.android.superb E/XposedInto: 当前HashCode 2051150842  原始hashcode 2051150842
//                    2020-09-11 19:40:52.216 26522-26703/com.sup.android.superb E/XposedInto: 当前方法名字 emitCompleteSegments
//                    2020-09-11 19:40:52.216 26522-26703/com.sup.android.superb E/XposedInto: 当前HashCode 2051150842  原始hashcode 2051150842

                    Object invoke = method.invoke(buffer);

//                    CLogUtils.e("当前HashCode "+invoke.hashCode()
//                            +"  原始hashcode "
//                            +buffer.hashCode()+"  "+(invoke==buffer));
                    if (invoke != buffer) {
                        return invoke;
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
        throw new Exception("getBuffCloneAndInvoke  失败 ");
    }

    private String getResponseMessage(Object response) throws Exception {
        Field[] declaredFields = response.getClass().getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.getType().getName().equals(String.class.getName())) {
                field.setAccessible(true);
                try {
                    return (String) field.get(response);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
        throw new Exception("getResponseMessage  失败 ");
    }

    /**
     * @return public long contentLength() throws IOException {
     * return -1;
     * }
     */
    private Long getContentLengthMethodAndInvokeFromRequestBody(Object requestBodyObject) throws Exception {
        Method[] declaredMethods = requestBodyObject.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (method.getReturnType().getName().equals(long.class.getName()) && method.getParameterTypes().length == 0) {
                method.setAccessible(true);
                try {
                    return (long) method.invoke(requestBodyObject);
                } catch (Throwable e) {
                    CLogUtils.e("getContentLengthMethodAndInvokeFromRequestBody error " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        throw new Exception("getContentLengthMethodAndInvokeFromRequestBody  没有 找到 ");
    }

    private Object getResponseBodyObject(Object responseObject, Class responseBodyClass) throws Exception {
        Field[] declaredFields = responseObject.getClass().getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.getType().getName().equals(responseBodyClass.getName())) {
                field.setAccessible(true);
                try {
                    return field.get(responseObject);
                } catch (Throwable e) {
                    CLogUtils.e("getResponseBodyObject  IllegalAccessException " + e.toString());
                    e.printStackTrace();
                }
            }
        }
        throw new Exception("getResponseBodyObject  没有 找到 ");
    }

    /**
     * 获取  responseObject 里面的 string方法
     */
    private String getStringMethodAndInvoke(Object ResponseBodyObject, Class responseBodyClass) throws Exception {

        try {
            try {
                Method string = responseBodyClass.getDeclaredMethod("string");
                if (string != null) {
                    string.setAccessible(true);
                    return (String) string.invoke(ResponseBodyObject);
                }
            } catch (NoSuchMethodException e) {
                CLogUtils.e("没找到  string 方法 开始 遍历 ");
                Method[] declaredMethods = responseBodyClass.getDeclaredMethods();
                for (Method method : declaredMethods) {
                    CLogUtils.e("当前方法 名字  " + method.getName() + "  返回值   " + method.getReturnType().getName() + "  参数 长度  " + method.getParameterTypes().length);
                    if (method.getReturnType().getName().equals(String.class.getName()) &&
                            method.getParameterTypes().length == 0
                    ) {
                        method.setAccessible(true);
                        return (String) method.invoke(ResponseBodyObject);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            CLogUtils.e("getStringMethodAndInvoke  IllegalAccessException " + e.toString());
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            CLogUtils.e("getStringMethodAndInvoke  InvocationTargetException " + e.toString());
            e.printStackTrace();
        }
        throw new Exception("getStringMethodAndInvoke ==Null ");
    }

    /**
     * 主要是 调用MediaType 里面的 charset方法 实现
     *
     * @param contentType contentType
     */
    private Charset getMediaTypeCharSetMethodAndInvoke(Object contentType, Charset arg1) throws Exception {

        Method[] declaredMethods = contentType.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (method.getReturnType().getName().equals(Charset.class.getName()) &&
                    method.getParameterTypes().length == 1 &&
                    method.getParameterTypes()[0].getName().equals(Charset.class.getName())
            ) {
                method.setAccessible(true);
                //用 默认 U8为参数
                return (Charset) method.invoke(contentType, arg1);
            }
        }
        throw new Exception("getMediaTypeCharSetMethodAndInvoke charsetMethod ==Null ");
    }

    /**
     * result.head 参数类型是 Segment
     */
    private Object getHeadInBuffer(Object bufferObject) throws Exception {
        Class segmentClass = getSegmentClass();
        Field[] declaredFields = bufferObject.getClass().getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.getType().getName().equals(segmentClass.getName())) {
                try {
                    return field.get(bufferObject);
                } catch (IllegalAccessException e) {
                    CLogUtils.e("getHeadInBuffer     " + e.toString());
                    e.printStackTrace();
                }
            }
        }
        throw new Exception("getHeadInBuffer  没有 找到 ");
    }

    private Class getSegmentClass() throws Exception {
//        final byte[] data;
//        int limit;
//        boolean owner;
//        int pos;
//        boolean shared;
//        m wka;
//        m wkb;
        //本身类型 2个   boolean两个 int两个 final byte 1个

        for (Class Mclass : Hook.mClassList) {
            int booleanCount = 0;
            int intCount = 0;
            int byteCount = 0;
            int selfCount = 0;
            Field[] declaredFields = Mclass.getDeclaredFields();
            for (Field field : declaredFields) {
                if (field.getType().getName().equals(boolean.class.getName())) {
                    booleanCount++;
                }
                if (field.getType().getName().equals(int.class.getName())) {
                    intCount++;
                }
                if (field.getType().getName().equals(byte[].class.getName()) && Modifier.isFinal(field.getModifiers())) {
                    byteCount++;
                }
                if (field.getType().getName().equals(Mclass.getName())) {
                    selfCount++;
                }
                if (selfCount == 2 && byteCount == 1 && intCount == 2 && booleanCount == 2) {
                    return Mclass;
                }
            }
        }
        throw new Exception("getSegmentClass   没有 找到 ");
    }

    private Long getBufferSizeMethodAndInvoke(Object buffer) throws Exception {
        Field[] declaredFields = buffer.getClass().getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.getType().getName().equals(long.class.getName())) {
                field.setAccessible(true);
                return field.getLong(buffer);
            }
        }
        throw new Exception("getBufferSizeMethodAndInvoke  没有 找到 ");
    }

    /**
     * Buffer buffer();
     * Buffer getBuffer();
     * 这两个方法的 其中一个
     *
     * @param BufferedSourceObject BufferedSourceObject
     * @param mBufferClass         mBuffer字节码
     * @return BufferObject
     */
    private Object getBufferMethodInBufferedSourceAndInvoke(Object BufferedSourceObject, Class mBufferClass) throws Exception {
        Method[] declaredMethods = BufferedSourceObject.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (method.getReturnType().getName().equals(mBufferClass.getName())
                    && method.getParameterTypes().length == 0) {
                method.setAccessible(true);
                try {
                    return method.invoke(BufferedSourceObject);
                } catch (Throwable e) {
                    CLogUtils.e("getBufferMethodInBufferedSourceAndInvoke   IllegalAccessException " + e.toString());
                    e.printStackTrace();
                }
            }
        }
        throw new Exception("getBufferMethodInBufferedSourceAndInvoke  没有 找到 ");
    }

    /**
     * boolean request(long byteCount) throws IOException;
     * 返回类型是 boolean 参数 1  个 并且是 long类型
     */
    private void getBufferedSourceRequestMethodAndInvoke(Object bufferedSourceObject, long byteCount) throws Exception {
        Method[] declaredMethods = bufferedSourceObject.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
//            if(method.getName().equals("request")){
//                CLogUtils.e((method.getParameterTypes().length == 1) +" "+
//                        method.getReturnType().getName().equals(boolean.class.getName()) +" "+
//                        method.getParameterTypes()[0].getName().equals(long.class.getName()+""));
//            }
            if (method.getParameterTypes().length == 1 &&
                    method.getReturnType().getName().equals(boolean.class.getName()) &&
                    method.getParameterTypes()[0].getName().equals(long.class.getName())
            ) {
                method.setAccessible(true);
                try {
                    method.invoke(bufferedSourceObject, byteCount);
                    return;
                } catch (Throwable e) {
                    CLogUtils.e("getBufferedSourceRequestMethodAndInvoke    " + e.toString());
                    e.printStackTrace();
                }
            }
        }
        throw new Exception("没有 找到  Request 方法    getBufferedSourceRequestMethodAndInvoke");
    }


//    private static boolean bodyHasUnknownEncoding(Headers headers) {
//        String contentEncoding = headers.get("Content-Encoding");
//        return contentEncoding != null
//                && !contentEncoding.equalsIgnoreCase("identity")
//                && !contentEncoding.equalsIgnoreCase("gzip");
//    }

    /**
     * 这个 类型 特征 很难写 需要 根据 这三个 抽象方法的 返回类型 拿到 具体的内容
     * <p>
     * 主要为了复现 这个方法 BufferedSource source = responseBody.source();
     * <p>
     * <p>
     * public abstract @Nullable MediaType contentType();
     * public abstract long contentLength();
     * public abstract BufferedSource source();
     * <p>
     * 在 responseBody 返回值是  BufferedSource只有一个方法
     */
    private Object getSourceAndInvoke(Object responseBodyObject) throws Exception {
        Class BufferedSourceType = getResponseBodySourceMethodReturnType(responseBodyObject);
        Method[] declaredMethods = responseBodyObject.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (method.getReturnType().getName().equals(BufferedSourceType.getName())) {
                method.setAccessible(true);
                try {
                    return method.invoke(responseBodyObject);
                } catch (IllegalAccessException e) {
                    CLogUtils.e("  getSourceAndInvoke   IllegalAccessException   " + e.toString());
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    CLogUtils.e("  getSourceAndInvoke   InvocationTargetException   " + e.toString());
                    e.printStackTrace();
                }
            }
        }
        throw new Exception("getSourceAndInvoke  没有 找到 ");
    }

    /**
     * 主要 返回 public abstract BufferedSource source();
     * BufferedSource 的类型    防混淆
     * 把其他 有可能 出现的类型全部过滤掉
     *
     * @param responseBodyObject responseBodyObject
     */
    private Class getResponseBodySourceMethodReturnType(Object responseBodyObject) throws Exception {
        Class mediaTypeClass = getMediaTypeClass();
        if (mediaTypeClass != null) {
            Method[] declaredMethods = responseBodyObject.getClass().getDeclaredMethods();
            for (Method method : declaredMethods) {
                //这块 逻辑 比较复杂 我目前的想法是 返回类型 只有  BufferedSource 类型 才可以 把其他方法返回值类型 干掉
                if (!method.getReturnType().getName().equals(long.class.getName()) &&
                        !method.getReturnType().getName().equals(mediaTypeClass.getName()) &&
                        !method.getReturnType().getName().equals(void.class.getName()) &&
                        !method.getReturnType().getName().equals(int.class.getName()) &&
                        !method.getReturnType().getName().equals(responseBodyObject.getClass().getName()) &&
                        !method.getReturnType().getName().equals(Charset.class.getName()) &&
                        !method.getReturnType().getName().equals(String.class.getName()) &&
                        !method.getReturnType().getName().equals(Reader.class.getName()) &&
                        !method.getReturnType().getName().equals(byte[].class.getName()) &&
                        !method.getReturnType().getName().equals(InputStream.class.getName())
                ) {
                    method.setAccessible(true);
                    return method.getReturnType();
                }
            }
        }
        throw new Exception("getResponseBodySourceMethodReturnType  没有 找到 ");
    }

    /**
     * 根据头部信息判断是否支持解码
     *
     * @param HeadersObject HeadersObject
     */
    private boolean bodyHasUnknownEncoding(Object HeadersObject) throws Exception {
        try {
            if (HeadersObject != null) {
                //String contentEncoding = getHeadersGetMethodAndInvoke(HeadersObject, "Content-Encoding");
                String contentEncoding =getHeadersGetMethodAndInvoke(HeadersObject, "Content-Encoding");
                return contentEncoding != null
                        && !contentEncoding.equalsIgnoreCase("identity")
                        && !contentEncoding.equalsIgnoreCase("gzip");
            }
        } catch (Throwable e) {
            CLogUtils.e("bodyHasUnknownEncoding error " + e.getMessage());
            e.printStackTrace();
        }
        throw new Exception("bodyHasUnknownEncoding  没有 找到 ");
    }

    /**
     * Returns true if the response must have a (possibly 0-length) body. See RFC 7231.
     * 如果响应长度不为0则返回true
     */
    private boolean ResponseHasBody(Object ResponseObject) throws Exception {

//        /** Returns true if the response must have a (possibly 0-length) body. See RFC 7231. */
//        public static boolean hasBody(Response response) {
//            // HEAD requests never yield a body regardless of the response headers.
//            if (response.request().method().equals("HEAD")) {
//                return false;
//            }
//
//            int responseCode = response.code();
//            if ((responseCode < HTTP_CONTINUE || responseCode >= 200)
//                    && responseCode != HTTP_NO_CONTENT
//                    && responseCode != HTTP_NOT_MODIFIED) {
//                return true;
//            }
//
//            // If the Content-Length or Transfer-Encoding headers disagree with the response code, the
//            // response is malformed. For best compatibility, we honor the headers.
//            if (contentLength(response) != -1
//                    || "chunked".equalsIgnoreCase(response.header("Transfer-Encoding"))) {
//                return true;
//            }
//
//            return false;
//        }

        //从响应中 拿到请求
        Object Request = getResponseInRequestMethodAndInvoke(ResponseObject);


        if (Request != null) {
            //CLogUtils.e("Request 的名字是   " + Request.getClass().getName());


            String methodFieldInRequest = getMethodFieldInRequest(Request);
            if (methodFieldInRequest.equals("HEAD")) {
                return false;
            }
            int responseCode = getResponseCode(ResponseObject);

            if ((responseCode < HTTP_CONTINUE || responseCode >= 200)
                    && responseCode != HTTP_NO_CONTENT
                    && responseCode != HTTP_NOT_MODIFIED) {
                return true;
            }

            return contentLength(ResponseObject) != -1
                    || "chunked".equalsIgnoreCase(ResponseInHeaderMethodAndInvoke(ResponseObject,"Transfer-Encoding"));
        } else {
            CLogUtils.e("  没拿到   ResponseInRequest");
        }
        throw new Exception("ResponseHasBody  没有 找到 ");
    }

    private String ResponseInHeaderMethodAndInvoke(Object ResponseObject, String args) throws Exception {
        Object headersInResponse = getHeadersInRe(ResponseObject);
        return getHeadersGetMethodAndInvoke(headersInResponse, args);
    }


    private String HeaderGet(Object responseObject, String args) throws Exception {
        Object headersInResponse = getHeadersInRe(responseObject);
        if (headersInResponse != null) {
            return getHeadersGetMethodAndInvoke(headersInResponse, args);
        }
        throw new Exception("HeaderGet headersInResponse == Null ");
    }

    private long contentLength(Object responseObject) throws Exception {
        Object headers = getHeadersInRe(responseObject);
        if (headers != null) {
            String headersGetMethodAndInvoke = getHeadersGetMethodAndInvoke(headers, "Content-Length");
            return stringToLong(headersGetMethodAndInvoke);
        }
        throw new Exception("ContentLength == Null ");
    }

    /**
     * 从头部里面获取
     * headers.get("Content-Encoding")
     *
     * @param headersInResponse
     * @param args
     * @return
     * @throws Exception
     */
    private String getHeadersGetMethodAndInvoke(Object headersInResponse, String args) throws Exception {
        Method[] declaredMethods = headersInResponse.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (method.getReturnType().getName().equals(String.class.getName()) &&
                    method.getParameterTypes().length == 1 &&
                    method.getParameterTypes()[0].getName().equals(String.class.getName())
            ) {
                method.setAccessible(true);
                try {
                    return (String) method.invoke(headersInResponse, args);

                } catch (Throwable e) {
                    CLogUtils.e("getHeadersGetMethodAndInvoke   IllegalAccessException " + e.toString());
                    e.printStackTrace();
                }
            }
        }
        throw new Exception("getHeadersGetMethodAndInvoke == Null ");
    }

    private int getResponseCode(Object responseObject) throws Exception {
        Field[] declaredFields = responseObject.getClass().getDeclaredFields();
        for (Field field : declaredFields) {
            //final 并且是 String
            if (field.getType().getName().equals(int.class.getName()) && Modifier.isFinal(field.getModifiers())) {

                field.setAccessible(true);
                try {
                    return field.getInt(responseObject);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    CLogUtils.e("getMethodFieldInRequest   IllegalAccessException 没拿到   method");
                }
            }
        }
        throw new Exception("getResponseCode == Null ");
    }

    /**
     * 获取 Request里面的  method 字段 对象 等同于调用  method方法
     *
     * @param responseInRequestMethodAndInvoke Request
     */
    private String getMethodFieldInRequest(Object responseInRequestMethodAndInvoke) throws Exception {
        Field[] declaredFields = responseInRequestMethodAndInvoke.getClass().getDeclaredFields();
        for (Field field : declaredFields) {
            //final 并且是 String
            if (field.getType().getName().equals(String.class.getName()) && Modifier.isFinal(field.getModifiers())) {
                field.setAccessible(true);
                try {
                    return (String) field.get(responseInRequestMethodAndInvoke);
                } catch (Throwable e) {
                    e.printStackTrace();
                    CLogUtils.e("getMethodFieldInRequest   IllegalAccessException 没拿到   method");
                }
            }
        }
        throw new Exception("getMethodFieldInRequest == Null ");
    }

    /**
     * 获取 Response 里面的 Request的 方法
     */
    private Object getResponseInRequestMethodAndInvoke(Object ResponseObject) throws Exception {
        Field[] declaredFields = ResponseObject.getClass().getDeclaredFields();
        for (Field field : declaredFields) {
            //比较类型是 Request
            if (field.getType().getName().equals(RequestClass.getName())) {
                try {
                    field.setAccessible(true);
                    return field.get(ResponseObject);
                } catch (Throwable e) {
                    CLogUtils.e("getResponseInRequestMethodAndInvoke    没拿到   ResponseInRequest");
                    e.printStackTrace();
                }
            }
        }
        throw new Exception("getResponseInRequestMethodAndInvoke == Null ");
    }

    private int getResponseCodeMethodAndInvoke(Object responseObject) throws Exception {
        Method[] declaredMethods = responseObject.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (method.getReturnType().getName().equals(int.class.getName()) && method.getParameterTypes().length == 0) {
                method.setAccessible(true);
                try {
                    return (int) method.invoke(responseObject);
                } catch (IllegalAccessException e) {
                    CLogUtils.e("getResponseBodyContentLengthMethodAndInvoke   IllegalAccessException   " + e.toString());
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    CLogUtils.e("getResponseBodyContentLengthMethodAndInvoke   InvocationTargetException   " + e.toString());

                    e.printStackTrace();
                }
            }
        }
        throw new Exception("getResponseCodeMethodAndInvoke == Null ");

    }

    /**
     * 获取 ResponseBody contentLength
     * public abstract long contentLength();
     */
    private long getResponseBodyContentLengthMethodAndInvoke(Object responseBodyObject) throws Exception {
        Method[] declaredMethods = responseBodyObject.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (method.getParameterTypes().length == 0
                    && method.getReturnType().getName().equals(long.class.getName())) {
                method.setAccessible(true);
                try {
                    return (long) method.invoke(responseBodyObject);
                } catch (Throwable e) {
                    CLogUtils.e("getResponseBodyContentLengthMethodAndInvoke   IllegalAccessException   " + e.toString());
                    e.printStackTrace();
                }
            }
        }
        throw new Exception("getResponseBodyContentLengthMethodAndInvoke == Null ");
    }

    private Class getResponseBodyClass() throws Exception {
        if (ResponseBodyClass != null) {
            return ResponseBodyClass;
        }
        //本身 是抽象类  抽象方法 大于2 个
        //其中有一个 抽象方法 返回的 类型是 MediaType 参数 为 0个
        Class mediaTypeClass = getMediaTypeClass();
        if (mediaTypeClass != null) {
            for (Class Mclass : Hook.mClassList) {
                Field[] declaredFields = Mclass.getDeclaredFields();
                if (declaredFields.length == 1 && declaredFields[0].getType().getName().equals(Reader.class.getName())) {
                    int MediaTypeCount = 0;
                    Method[] declaredMethods = Mclass.getDeclaredMethods();
                    for (Method method : declaredMethods) {
                        if (method.getReturnType().getName().equals(mediaTypeClass.getName())) {
                            MediaTypeCount++;
                        }
                    }
                    if (MediaTypeCount >= 1) {
                        return Mclass;
                    }
                }
            }
        } else {
            CLogUtils.e("getResponseBodyClass   getMediaTypeClass    == null ");
        }
        throw new Exception("getResponseBodyClass == Null ");
    }

    private Object getResponseBodyMethodAndInvoke(Class bodyClass, Object responseBodyClass) throws Exception {
        Method[] declaredMethods = responseBodyClass.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (method.getParameterTypes().length == 0 && method.getReturnType().getName().equals(bodyClass.getName())) {
                try {
                    method.setAccessible(true);
                    return method.invoke(responseBodyClass);
                } catch (IllegalAccessException e) {
                    CLogUtils.e("getResponseBodyMethodAndInvoke   IllegalAccessException   " + e.toString());
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    CLogUtils.e("getResponseBodyMethodAndInvoke   InvocationTargetException   " + e.toString());
                    e.printStackTrace();
                }
            }
        }
        throw new Exception("getResponseBodyMethodAndInvoke == Null ");
    }

    /**
     * 获取  request.method()
     *
     * @param requestObject requestObject
     */
    private String getMethodInRequest(Object requestObject) throws Exception {
//        Method[] declaredMethods = requestObject.getClass().getDeclaredMethods();
//        for (Method method : declaredMethods) {
//            if (method.getParameterTypes().length == 0 &&
//                    method.getReturnType().getName().equals(String.class.getName())
//            ) {
//                method.setAccessible(true);
//                try {
//                    return (String) method.invoke(requestObject);
//                } catch (Throwable e) {
//                    CLogUtils.e("getMethodInRequest   IllegalAccessException   " + e.toString());
//                    e.printStackTrace();
//                }
//            }
//        }
        Field[] declaredFields = requestObject.getClass().getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.getType().getName().equals(String.class.getName())
                    && Modifier.isFinal(field.getModifiers())
            ) {
                field.setAccessible(true);
                return (String) field.get(requestObject);
            }
        }
        throw new Exception("getMethodInRequest == Null ");

    }

    private String getReadStringAndInvoke(Object bufferObject, Charset charset) throws Exception {
        Method[] declaredMethods = bufferObject.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (method.getReturnType().getName().equals(String.class.getName())
                    && method.getParameterTypes().length == 1 &&
                    method.getParameterTypes()[0].getName().equals(Charset.class.getName())) {
                method.setAccessible(true);
                try {
                    return (String) method.invoke(bufferObject, charset);
                } catch (Throwable e) {
                    CLogUtils.e("getReadStringAndInvoke   IllegalAccessException   " + e.toString());
                    e.printStackTrace();
                }
            }
        }
        throw new Exception("getReadStringAndInvoke == Null ");
    }


    private boolean exhausted(Object prefix) throws Exception {
        Field[] declaredFields = prefix.getClass().getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.getType().getName().equals(long.class.getName())) {
                field.setAccessible(true);
                try {
                    long aLong = field.getLong(prefix);
                    return aLong == 0L;
                } catch (IllegalAccessException e) {
                    CLogUtils.e("exhausted   " + e.toString());
                    e.printStackTrace();
                }
            }
        }
        throw new Exception("exhausted   没找到 this.size");
    }

    private boolean isPlaintext(Object bufferObject) throws Exception {
        try {


//            Buffer prefix = new Buffer();
//            long byteCount = buffer.size() < 64L ? buffer.size() : 64L;
//            buffer.copyTo(prefix, 0L, byteCount);
//
//            for(int i = 0; i < 16 && !prefix.exhausted(); ++i) {
//                int codePoint = prefix.readUtf8CodePoint();
//                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
//                    return false;
//                }
//            }


            Object prefix = bufferObject.getClass().newInstance();

            long bufferObjectSize = getBufferSize(bufferObject);

            if (bufferObjectSize != -1L) {
                long byteCount = bufferObjectSize < 64L ? bufferObjectSize : 64L;
                //            buffer.copyTo(prefix, 0L, byteCount);
                getCopyToMethodAndInvoke(bufferObject, prefix, byteCount);

//                for(int i = 0; i < 16 && !prefix.exhausted(); ++i) {
//                    int codePoint = prefix.readUtf8CodePoint();
//                    if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
//                        return false;
//                    }
//                }

                Method readUtf8CodePointMethod = getReadUtf8CodePointMethod(prefix);
                if (readUtf8CodePointMethod != null) {
                    for (int i = 0; i < 16 && !exhausted(prefix); ++i) {
                        int codePoint = (int) readUtf8CodePointMethod.invoke(prefix);
                        if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                            return false;
                        }
                    }
                    return true;
                } else {
                    CLogUtils.e("没拿到    readUtf8CodePointMethod ");
                    throw new Exception("没拿到    readUtf8CodePointMethod");
                }
            } else {
                CLogUtils.e("没拿到    bufferObjectSize ");
                throw new Exception("没拿到    bufferObjectSize");
            }
        } catch (InstantiationException e) {
            CLogUtils.e("isPlaintext   InstantiationException   " + e.toString());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            CLogUtils.e("isPlaintext   IllegalAccessException   " + e.toString());
            e.printStackTrace();
        } catch (Throwable e) {
            CLogUtils.e("isPlaintext   Exception   " + e.toString());
            e.printStackTrace();
        }
        throw new Exception("isPlaintext == Null ");
    }

    private long getBufferSize(Object bufferObject) throws Exception {
        Field[] fields = bufferObject.getClass().getDeclaredFields();
        long bufferObjectSize = -1L;
        for (Field field : fields) {
            if (field.getType().getName().equals(long.class.getName())) {
                field.setAccessible(true);
                return field.getLong(bufferObject);
            }
        }
        throw new Exception("getBufferSize == Null ");
    }

    private Method getReadUtf8CodePointMethod(Object prefix) throws Exception {
        Method[] declaredMethods = prefix.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (method.getParameterTypes().length == 0 && method.getReturnType().getName().equals(int.class.getName())) {
                //public int readUtf8CodePoint() throws EOFException   这块 是 异常类型 是1 个 类型是  EOFException
                if (method.getExceptionTypes().length == 1 && method.getExceptionTypes()[0].getName().equals(EOFException.class.getName())) {
                    method.setAccessible(true);
                    return method;
                }
            }
        }
        throw new Exception("getReadUtf8CodePointMethod == Null ");
    }

    private void getCopyToMethodAndInvoke(Object bufferObject, Object prefix, long byteCount) throws Exception {
        //buffer.copyTo(prefix, 0L, byteCount);
        Method[] declaredMethods = bufferObject.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {

//            if(method.getName().equals("copyTo")&&method.getParameterTypes().length == 3){
//                CLogUtils.e("方法 信息     "
//                        +(method.getParameterTypes()[0].getName().equals(bufferObject.getClass().getName()))+
//                                "   "+bufferObject.getClass().getName()+"     "+method.getParameterTypes()[0].getName()+"    "+
//                                ( method.getParameterTypes()[1].getName().equals(long.class.getName()))+"   "+
//                                (method.getParameterTypes()[2].getName().equals(long.class.getName()))
//                        );
//            }

            //返回值是 Buff
            if (method.getReturnType().getName().equals(bufferObject.getClass().getName())) {
                //参数 长度是 3 参数 类型 Buff long longg
                if (method.getParameterTypes().length == 3
                        //这个 参数 可能是 buff也可能 是 OutputStream 版本有关系
                        && (method.getParameterTypes()[0].getName().equals(bufferObject.getClass().getName()))
                        && (method.getParameterTypes()[1].getName().equals(long.class.getName()))
                        && (method.getParameterTypes()[2].getName().equals(long.class.getName()))
                ) {
                    method.setAccessible(true);
                    try {
                        method.invoke(bufferObject, prefix, 0L, byteCount);
                        return;
                    } catch (IllegalAccessException e) {
                        CLogUtils.e("isPlaintext   IllegalAccessException   " + e.toString());
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        CLogUtils.e("getcopyToMethodAndInvoke   InvocationTargetException   " + e.toString());
                        e.printStackTrace();
                    }
                }
            }
        }
        throw new Exception("getCopyToMethodAndInvoke == Null ");
    }

    /**
     * @param RBodyObject 响应 或者 请求的 Body
     * @return MediaType contentType = responseBody.contentType()
     */
    private Object getContentTypeMethodAndInvokeFromBody(Object RBodyObject) throws Exception {
        Class mediaTypeClass = getMediaTypeClass();
        Method[] declaredMethods = RBodyObject.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            //返回  类型是 mediaTypeClass 参数 无
            if (method.getParameterTypes().length == 0 && method.getReturnType().getName().equals(mediaTypeClass.getName())) {
                method.setAccessible(true);
                try {
                    return method.invoke(RBodyObject);
                } catch (Throwable e) {
                    CLogUtils.e("getContentTypeMethodAndInvoke   IllegalAccessException   " + e.toString());
                    e.printStackTrace();
                }
            }
        }
        throw new Exception("getContentTypeMethodAndInvoke == Null ");
    }

    /**
     * okhttp3.MediaType
     */
    private Class getMediaTypeClass() throws Exception {
        if (MediaTypeClass != null) {
            return MediaTypeClass;
        }
        throw new Exception("getMediaTypeClass == Null ");
    }

    private void invokeWriteTo(Object requestBodyObject, Object BufferedSinkObject, Class bufferedSinkClass) throws Exception {
        Method[] declaredMethods = requestBodyObject.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {

//            if(method.getName().equals("writeTo")){
//                CLogUtils.e((method.getParameterTypes().length == 1) + " "+
//                        method.getParameterTypes()[0].getName().equals(bufferedSinkClass.getName())+" "+
//                        method.getReturnType().getName().equals(void.class.getName())
//                        +bufferedSinkClass.getName()+"  "+ method.getParameterTypes()[0].getName()
//                );
//            }

            //参数类型 是 Buff类型
            if (method.getParameterTypes().length == 1 &&
                    method.getParameterTypes()[0].getName().equals(bufferedSinkClass.getName()) &&
                    method.getReturnType().getName().equals(void.class.getName())
            ) {
                try {
                    //CLogUtils.e("RequestBody writeTo  当前方法的名字是  " + method.getName());
                    method.setAccessible(true);
                    method.invoke(requestBodyObject, BufferedSinkObject);
                    return;
                } catch (Throwable e) {
                    CLogUtils.e(" invokeW1riteTo  Throwable" + e.toString());
                    e.printStackTrace();
                    return;
                }
            }
        }
        throw new Exception("invokeW1riteTo == Null ");
    }


    private Object getBufferObject() throws Exception {
        return BufferClass.newInstance();
    }

    /**
     * @param requestObject    requestObject
     * @param requestBodyClass requestBodyClass
     * @return RequestBodyObject
     */
    private Object getRequestBodyObject(Object requestObject, Class requestBodyClass) throws Exception {

        if (requestObject == null) {
            throw new Exception("getRequestBodyObject requestObject == Null ");
        }
        if (requestBodyClass == null) {
            throw new Exception("getRequestBodyObject requestBodyClass == Null ");
        }
        //返回类型 是 requestBodyClass 类型
        try {
            Field[] declaredFields = requestObject.getClass().getDeclaredFields();
            for (Field field : declaredFields) {

                if (field.getType().getName().equals(requestBodyClass.getName())) {


                    field.setAccessible(true);
                    return field.get(requestObject);
                }
            }
        } catch (Throwable e) {
            CLogUtils.e("getRequestBodyObject error " + e.toString());
            e.printStackTrace();
        }
        return null;
    }

    private Class getRequestBodyClass() throws Exception {
        if (RequestBodyClass != null) {
            return RequestBodyClass;
        }

        throw new Exception("getRequestBodyClass == Null ");
    }

    private Object getResponseObject(Object chainObject, Object RequestObject) throws Exception {
        Method[] declaredMethods = chainObject.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            // Response proceed(Request var1) throws IOException;  参数 个数 1个
            // 返回的类型是 Class 而不是 接口 并且类型是 final类型  并且第一个
            // 参数类型是 Request
            if (parameterTypes.length == 1) {
                if (!method.getReturnType().isInterface() &&
                        Modifier.isFinal(method.getReturnType().getModifiers()) &&
                        parameterTypes[0].getName().equals(RequestObject.getClass().getName())) {
                    try {
                        method.setAccessible(true);
                        return method.invoke(chainObject, RequestObject);
                    } catch (Throwable e) {
                        CLogUtils.e("getRequestObject  IllegalAccessException    " + e.toString());
                        e.printStackTrace();
                    }
                }
            }
        }
        throw new Exception("getResponseObject == Null ");
    }

    /**
     * 从请求或者响应里面拿到Headers
     */
    private Object getHeadersInRe(Object reObject) throws Exception {

        Field[] declaredFields = reObject.getClass().getDeclaredFields();
        for (Field field : declaredFields) {
            //CLogUtils.e("当前字段名字 "+field.getName()+"  类型  "+field.getType()+" HeaderClass.getName() "+HeaderClass.getName());
            if (field.getType().getName().equals(HeaderClass.getName())) {
                try {
                    field.setAccessible(true);
                    return field.get(reObject);
                } catch (Throwable e) {
                    CLogUtils.e("getHeadersInResponse   " + e.toString());
                    e.printStackTrace();
                }
            }
        }
        throw new Exception("getHeadersInRe == Null "+reObject.getClass().getName());
    }


    private String getHeadersToStringMethodAndInvoke(Class headerClass, Object Object) throws Exception {
        Method[] declaredMethods = Object.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            //返回 类型 是 headerClass
            if (method.getReturnType().getName().equals(headerClass.getName())
                    && method.getParameterTypes().length == 0
            ) {
                try {
                    //CLogUtils.e("getHeadersToStringMethodAndInvoke当前方法名字 "+method.getName());
                    method.setAccessible(true);
                    Object Headers = method.invoke(Object);

                    try {
                        return Headers.toString();
                    } catch (Throwable e) {
                        e.printStackTrace();
                        CLogUtils.e("getHeadersMethodAndInvoke   " + e.toString());
                    }
                } catch (Throwable e) {
                    CLogUtils.e("getHeadersMethodAndInvoke  IllegalAccessException " + e.toString());
                    e.printStackTrace();
                }
            }
        }
        throw new Exception("getHeadersMethodAndInvoke == Null ");
    }


    private String InvokeToString(Object Object) throws Exception {
        try {
            Method method = Object.getClass().getDeclaredMethod("toString");
            if (method == null) {
                Method[] declaredMethods = Object.getClass().getDeclaredMethods();
                for (Method method1 : declaredMethods) {
                    if (method1.getReturnType().getName().equals(String.class.getName()) &&
                            method1.getParameterTypes().length == 0
                    ) {
                        method = method1;
                    }
                }
            }
            Objects.requireNonNull(method).setAccessible(true);
            return (String) method.invoke(Object);
        } catch (Throwable e) {
            CLogUtils.e("InvokeToString   没有 找到 toString " + e.toString());
            e.printStackTrace();
        }
        throw new Exception("InvokeToString == Null ");
    }

    private Object getRequestObject(Object chainObject, Class RequestClass) throws Exception {
        if (RequestClass == null) {
            throw new Exception("getRequestObject  RequestClass == Null ");
        }
        Method[] declaredMethods = chainObject.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            //Request request();  参数 个数 0个 返回的类型是 Class 而不是 接口 并且类型是 final类型
            if (parameterTypes == null || parameterTypes.length == 0) {
                if (method.getReturnType().getName().equals(RequestClass.getName())) {
                    try {
                        method.setAccessible(true);
                        return method.invoke(chainObject);
                    } catch (Throwable e) {
                        CLogUtils.e("getRequestObject  IllegalAccessException    " + e.toString());
                        e.printStackTrace();
                    }
                }
            }
        }
        throw new Exception("getRequestObject == Null ");
    }
}
