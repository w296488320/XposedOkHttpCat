package q296488320.xposedinto.LogImp;

import java.io.EOFException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.regex.Pattern;

import q296488320.xposedinto.XpHook.Hook;
import q296488320.xposedinto.utils.CLogUtils;

import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;

/**
 * Created by ZhenXi on
 * 2019/8/22
 */
public class LogInterceptorImp implements InvocationHandler {
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private Object RequestObject;
    private Object ResponseObject;

    private Object mBufferObject;
    private StringBuilder mStringBuffer;


    private Class MediaTypeClass;
    private Class mResponseBodyClass;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        //CLogUtils.e("LogInterceptorImp   方法名字  "+method.getName()  +"    参数个数   "+args.length);
        try {
            mStringBuffer = new StringBuilder();
            //里面只有 一个方法
            Object ChainObject = args[0];


            mStringBuffer.append("\n" + "--------------------------------------->>>" + "\n");

            RequestObject = getRequestObject(ChainObject);
            //请求的 Url信息
            mStringBuffer.append(InvokeToString(RequestObject)).append("\n");


            ResponseObject = getResponseObject(ChainObject, RequestObject);

            if (RequestObject == null) {
                CLogUtils.e("getRequestObject   getRequestObject   返回 Null");
                return ResponseObject;
            } else {
                CLogUtils.e("getRequestObject   拿到 RequestObject  名字是   " + RequestObject.getClass().getName());
            }
            if (ResponseObject == null) {
                CLogUtils.e("getRequestObject   拿到 ResponseObject  名字是   ");
                return ResponseObject;
            } else {
                CLogUtils.e("   拿到 ResponseObject  名字是   " + ResponseObject.getClass().getName());
            }

            Class headerClass = getHeaderClass();
            if (headerClass != null) {
                CLogUtils.e("getRequestObject  拿到 headerClass  名字是  " + headerClass.getName());
                //拿到请求头的信息 直接 调用的 toString
                String RequestHeaders = getHeadersMethodAndInvoke(headerClass, RequestObject);
                mStringBuffer.append(RequestHeaders).append("\n");
            } else {
                CLogUtils.e("没有找到    拿到 RequestHeaders    名字是   ");
                CLogUtils.NetLogger("===========================  " + mStringBuffer.toString());
                return ResponseObject;
            }
            //需要先 拿到 RequestBody 类型
            Class RequestBodyClass = getRequestBodyClass();

            Object RequestBodyObject = getRequestBodyObject(RequestObject, RequestBodyClass);
            //Get请求 没有 请求体  可能 存在 为Nulll的 情况
            if (RequestBodyObject != null) {

                //在 不等于 Null的 时候 开始遍历  请求 Body
                CLogUtils.e("拿到  RequestBodyObject  名字是  " + RequestBodyObject.getClass().getName());

                mBufferObject = getBufferObject();
                Class bufferedSinkClass = getBufferedSinkClass();

                if (mBufferObject != null) {
                    CLogUtils.e("拿到  bufferObject  " + mBufferObject.getClass().getName());

                    //这块 参数类型 是 BufferedSink  但是传入的是 Buffer
                    if (invokeW1riteTo(RequestBodyObject, mBufferObject, bufferedSinkClass)) {
                        //默认是 U8解码
                        Charset charset = UTF8;
                        //MediaType 类型
                        Object contentType = getContentTypeMethodAndInvoke(RequestBodyObject);
                        if (contentType != null) {
                            charset = getMediaTypeCharSetMethodAndInvoke(contentType);
                        } else {
                            CLogUtils.e("contentTypeMethodAndInvoke  == null ");
                        }
                        if (isPlaintext(mBufferObject)) {
                            //logger.log(buffer.readString(charset));
                            mStringBuffer.append(getReadStringAndInvoke(mBufferObject, charset)).append("\n");
                        }
                    } else {
                        CLogUtils.e("没有成功 设置  requestBody.writeTo(buffer) ");
                    }
                }
            }


            //添加 响应体 的 url之类的
            mStringBuffer.append("\n" + "<<<---------------------------------------").append("\n");
            mStringBuffer.append(InvokeToString(ResponseObject)).append("\n");

            mResponseBodyClass = getResponseBodyClass();

            if (mResponseBodyClass != null) {
                //获取 响应头
                String ResponseHeaders = getHeadersMethodAndInvoke(headerClass, ResponseObject);
                if (ResponseHeaders == null) {
                    CLogUtils.e("ResponseHeaders   getHeadersMethodAndInvoke 返回 Null");
                    return ResponseObject;
                } else {
                    //打印响应头
                    mStringBuffer.append(ResponseHeaders).append("\n");
                    CLogUtils.e("ResponseHeaders   拿到响应 头部信息 ");
                }
                //判断是否存在响应体
                if (!ResponseHasBody(ResponseObject)) {
                    //不存在 Body的情况  直接打印结束
                    mStringBuffer.append("================     " + "<<<--------------- END HTTP").append("\n");
                    CLogUtils.NetLogger(mStringBuffer.toString());
                    return ResponseObject;
                    //判断 是否支持编码
                } else if (bodyHasUnknownEncoding(ResponseObject)) {
                    mStringBuffer.append("================     " + "<<<--------------- 解码 不支持 省略了 编码 正文 ").append("\n");
                    CLogUtils.NetLogger(mStringBuffer.toString());
                    return ResponseObject;

                } else {
                    Object ResponseBodyObject = getResponseBodyObject(ResponseObject, mResponseBodyClass);
                    if (ResponseBodyObject != null) {
                        //调用 string()  方法  ResponseBody 里面的方法
                        String ResponseBodyString = getStringMethodAndInvoke(ResponseBodyObject);

                        CLogUtils.e("响应体 内容是  " + ResponseBodyString);
                        if (ResponseBodyString != null) {
                            mStringBuffer.append(ResponseBodyString).append("\n").append("<<< 响应体   --------------- END HTTP ").append("\n");
                        }
                        CLogUtils.NetLogger("================     " + mStringBuffer.toString());
                        return ResponseObject;
                    }
                }


            } else {
                CLogUtils.e("没有 拿到 响应头  Class ");
            }

        } catch (Exception e) {
            //出现异常 统一 打印
            CLogUtils.e("发现异常  " + e.getMessage());

            StackTraceElement[] stackTrace = e.getStackTrace();
            for (StackTraceElement stackTraceElement : stackTrace) {
                CLogUtils.e("方法名字    " + stackTraceElement.getMethodName() + "------  行数    " + stackTraceElement.getLineNumber());
            }
            CLogUtils.NetLogger("================     出现异常 可能打印 不全面 " + "\n" + mStringBuffer.toString());
            e.printStackTrace();
        }
        return ResponseObject;
    }

    private Object getResponseBodyObject(Object responseObject, Class responseBodyClass) throws Exception {
        Field[] declaredFields = responseObject.getClass().getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.getType().getName().equals(responseBodyClass.getName())) {
                field.setAccessible(true);
                try {
                    return field.get(responseObject);
                } catch (IllegalAccessException e) {
                    CLogUtils.e("getResponseBodyObject  IllegalAccessException " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        throw new Exception("getResponseBodyObject  没有 找到 ");
    }

    private Class getBufferedSinkClass() throws Exception {
        //public interface BufferedSink extends Sink, WritableByteChannel
        for (Class MClass : Hook.mClassList) {
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
                Field[] declaredFields = MClass.getDeclaredFields();
                if (isHaveWritableByteChannel && declaredFields.length == 0) {
                    return MClass;
                }
            }
        }
        throw new Exception("getBufferedSinkClass  没有 找到 ");
    }


    /**
     * 获取  responseObject 里面的 string方法
     *
     * @param responseObject
     * @return
     */
    private String getStringMethodAndInvoke(Object responseObject) throws Exception {
        try {
            try {
                Method string = responseObject.getClass().getDeclaredMethod("string");
                if (string != null) {
                    string.setAccessible(true);
                    return (String) string.invoke(responseObject);
                }
            } catch (NoSuchMethodException e) {
                Method[] declaredMethods = responseObject.getClass().getDeclaredMethods();
                for (Method method : declaredMethods) {
                    if (method.getReturnType().getName().equals(String.class.getName()) &&
                            method.getParameterTypes().length == 0
                    ) {
                        method.setAccessible(true);
                        return (String) method.invoke(responseObject);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            CLogUtils.e("getStringMethodAndInvoke  IllegalAccessException " + e.getMessage());
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            CLogUtils.e("getStringMethodAndInvoke  InvocationTargetException " + e.getMessage());
            e.printStackTrace();
        }
        throw new Exception("getStringMethodAndInvoke ==Null ");
    }

    /**
     * 主要是 调用MediaType 里面的 charset方法 实现
     *
     * @param contentType contentType
     * @return
     * @throws Exception
     */
    private Charset getMediaTypeCharSetMethodAndInvoke(Object contentType) throws Exception {

        Method[] declaredMethods = contentType.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (method.getReturnType().getName().equals(Charset.class.getName()) &&
                    method.getParameterTypes().length == 1 &&
                    method.getParameterTypes()[0].getName().equals(Charset.class.getName())
            ) {
                method.setAccessible(true);
                //用 默认 U8为参数
                return (Charset) method.invoke(contentType, UTF8);
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
                    CLogUtils.e("getHeadInBuffer     " + e.getMessage());
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
     * @param mBufferObject        mBuffer字节码
     * @return BufferObject
     */
    private Object getBufferMethodInBufferedSourceAndInvoke(Object BufferedSourceObject, Class mBufferObject) throws Exception {
        Method[] declaredMethods = BufferedSourceObject.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (method.getReturnType().getName().equals(mBufferObject.getName())
                    && method.getParameterTypes().length == 0) {
                method.setAccessible(true);
                try {
                    return method.invoke(BufferedSourceObject);
                } catch (IllegalAccessException e) {
                    CLogUtils.e("getBufferMethodInBufferedSourceAndInvoke   IllegalAccessException " + e.getMessage());
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    CLogUtils.e("getBufferMethodInBufferedSourceAndInvoke   InvocationTargetException " + e.getMessage());
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
    private void getBufferedSourceRequestMethodAndInvoke(Object bufferedSourceObject, long body) throws Exception {
        Method[] declaredMethods = bufferedSourceObject.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (method.getParameterTypes().length == 1 &&
                    method.getReturnType().getName().equals(boolean.class.getName()) &&
                    method.getParameterTypes()[0].getName().equals(long.class.getName())
            ) {
                method.setAccessible(true);
                try {
                    method.invoke(bufferedSourceObject, body);
                } catch (IllegalAccessException e) {
                    CLogUtils.e("getBufferedSourceRequestMethodAndInvoke   IllegalAccessException " + e.getMessage());
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    CLogUtils.e("getBufferedSourceRequestMethodAndInvoke   InvocationTargetException " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        throw new Exception("没有 找到  Request 方法    getBufferedSourceRequestMethodAndInvoke");
    }

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
                    CLogUtils.e("  getSourceAndInvoke   IllegalAccessException   " + e.getMessage());
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    CLogUtils.e("  getSourceAndInvoke   InvocationTargetException   " + e.getMessage());
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


//    private static boolean bodyHasUnknownEncoding(Headers headers) {
//        String contentEncoding = headers.get("Content-Encoding");
//        return contentEncoding != null
//                && !contentEncoding.equalsIgnoreCase("identity")
//                && !contentEncoding.equalsIgnoreCase("gzip");
//    }

    /**
     * 主要 判断 响应体是否 支持解码
     *
     * @param ResponseObject ResponseObject
     */
    private boolean bodyHasUnknownEncoding(Object ResponseObject) throws Exception {
        try {
            return HeaderGetContent_Encoding(ResponseObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new Exception("bodyHasUnknownEncoding  没有 找到 ");
    }


    private static final int HTTP_CONTINUE = 100;

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
            CLogUtils.e("Request 的名字是   " + Request.getClass().getName());


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
                    || "chunked".equalsIgnoreCase(ResponseInHeadersMethod("Transfer-Encoding"));
        } else {
            CLogUtils.e("  没拿到   ResponseInRequest");
        }
        throw new Exception("ResponseHasBody  没有 找到 ");
    }


    private String ResponseInHeadersMethod(Object responseObject) throws Exception {
        Object headersInResponse = getHeadersInResponse(responseObject);
        if (headersInResponse != null) {
            return getHeadersGetMethodAndInvoke(headersInResponse, "Transfer-Encoding");
        }
        return "";
    }


    private boolean HeaderGetContent_Encoding(Object Object) throws Exception {
        //bodyHasUnknownEncoding(response.headers())  先拿到
        Object headersInObject = getHeadersInResponse(Object);

        if (headersInObject != null) {
            String contentEncoding = getHeadersGetMethodAndInvoke(headersInObject, "Content-Encoding");

            return contentEncoding != null
                    && !contentEncoding.equalsIgnoreCase("identity")
                    && !contentEncoding.equalsIgnoreCase("gzip");
        }
        throw new Exception("HeaderGetContent_Encoding  getHeadersInResponse  == Null ");
    }


    private String HeaderGet(Object responseObject) throws Exception {
        Object headersInResponse = getHeadersInResponse(responseObject);
        if (headersInResponse != null) {
            return getHeadersGetMethodAndInvoke(headersInResponse, "Content-Encoding");
        }
        throw new Exception("HeaderGet headersInResponse == Null ");
    }


    private long contentLength(Object responseObject) throws Exception {
        Object headersInResponse = getHeadersInResponse(responseObject);
        if (headersInResponse != null) {
            String headersGetMethodAndInvoke = getHeadersGetMethodAndInvoke(headersInResponse, "Content-Length");
            return stringToLong(headersGetMethodAndInvoke);
        }
        throw new Exception("ContentLength == Null ");
    }


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

                } catch (IllegalAccessException e) {
                    CLogUtils.e("getHeadersGetMethodAndInvoke   IllegalAccessException " + e.getMessage());
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    CLogUtils.e("getHeadersGetMethodAndInvoke   InvocationTargetException " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        throw new Exception("getHeadersGetMethodAndInvoke == Null ");
    }

    private static long stringToLong(String s) {
        if (s == null) return -1;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return -1;
        }
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
                } catch (IllegalAccessException e) {
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
            if (field.getType().getName().equals(RequestObject.getClass().getName())) {
                try {
                    field.setAccessible(true);
                    return field.get(ResponseObject);
                } catch (IllegalAccessException e) {
                    CLogUtils.e("getResponseInRequestMethodAndInvoke   IllegalAccessException 没拿到   ResponseInRequest");
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
                    CLogUtils.e("getResponseBodyContentLengthMethodAndInvoke   IllegalAccessException   " + e.getMessage());
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    CLogUtils.e("getResponseBodyContentLengthMethodAndInvoke   InvocationTargetException   " + e.getMessage());

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
    private int getResponseBodyContentLengthMethodAndInvoke(Class responseBodyClass, Object responseBodyObject) throws Exception {
        Method[] declaredMethods = responseBodyClass.getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (method.getParameterTypes().length == 0 && method.getReturnType().getName().equals(long.class.getName())) {
                method.setAccessible(true);
                try {
                    return (int) method.invoke(responseBodyObject);
                } catch (IllegalAccessException e) {
                    CLogUtils.e("getResponseBodyContentLengthMethodAndInvoke   IllegalAccessException   " + e.getMessage());
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    CLogUtils.e("getResponseBodyContentLengthMethodAndInvoke   InvocationTargetException   " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        throw new Exception("getResponseBodyContentLengthMethodAndInvoke == Null ");
    }

    private Class getResponseBodyClass() throws Exception {
        if (mResponseBodyClass != null) {
            return mResponseBodyClass;
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
                    CLogUtils.e("getResponseBodyMethodAndInvoke   IllegalAccessException   " + e.getMessage());
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    CLogUtils.e("getResponseBodyMethodAndInvoke   InvocationTargetException   " + e.getMessage());
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
        Method[] declaredMethods = requestObject.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (method.getParameterTypes().length == 0 &&
                    method.getReturnType().getName().equals(String.class.getName())
            ) {
                method.setAccessible(true);
                try {
                    return (String) method.invoke(requestObject);
                } catch (IllegalAccessException e) {
                    CLogUtils.e("getMethodInRequest   IllegalAccessException   " + e.getMessage());
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    CLogUtils.e("getMethodInRequest   InvocationTargetException   " + e.getMessage());
                    e.printStackTrace();
                }
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
                } catch (IllegalAccessException e) {
                    CLogUtils.e("getReadStringAndInvoke   IllegalAccessException   " + e.getMessage());

                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    CLogUtils.e("InvocationTargetException   InvocationTargetException   " + e.getMessage());
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
                    CLogUtils.e("exhausted   " + e.getMessage());
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
            CLogUtils.e("isPlaintext   InstantiationException   " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            CLogUtils.e("isPlaintext   IllegalAccessException   " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            CLogUtils.e("isPlaintext   Exception   " + e.getMessage());
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
                        CLogUtils.e("isPlaintext   IllegalAccessException   " + e.getMessage());
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        CLogUtils.e("getcopyToMethodAndInvoke   InvocationTargetException   " + e.getMessage());
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
    private Object getContentTypeMethodAndInvoke(Object RBodyObject) throws Exception {
        Class mediaTypeClass = getMediaTypeClass();
        Method[] declaredMethods = RBodyObject.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            //返回  类型是 mediaTypeClass 参数 无
            if (method.getParameterTypes().length == 0 && method.getReturnType().getName().equals(mediaTypeClass.getName())) {
                method.setAccessible(true);
                try {
                    return method.invoke(RBodyObject);
                } catch (IllegalAccessException e) {
                    CLogUtils.e("getContentTypeMethodAndInvoke   IllegalAccessException   " + e.getMessage());
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    CLogUtils.e("getContentTypeMethodAndInvoke   InvocationTargetException   " + e.getMessage());
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
//        private static final Pattern PARAMETER = Pattern.compile(";\\s*(?:([a-zA-Z0-9-!#$%&'*+.^_`{|}~]+)=(?:([a-zA-Z0-9-!#$%&'*+.^_`{|}~]+)|\"([^\"]*)\"))?");
//        private static final Pattern TYPE_SUBTYPE = Pattern.compile("([a-zA-Z0-9-!#$%&'*+.^_`{|}~]+)/([a-zA-Z0-9-!#$%&'*+.^_`{|}~]+)");
//        private final String charset;
//        private final String mediaType;
//        private final String subtype;
//        private final String type;
        // 还有两个方法 是 charSet返回类型

        Class<Pattern> patternClass = Pattern.class;

        for (Class mClass : Hook.mClassList) {
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
                        if (charSetReturn >= 2) {
                            MediaTypeClass = mClass;
                            return mClass;
                        }
                    }
                }
            }
        }
        throw new Exception("getMediaTypeClass == Null ");
    }

    private boolean invokeW1riteTo(Object requestBodyObject, Object BufferedSinkObject, Class bufferedSinkClass) throws Exception {
        Method[] declaredMethods = requestBodyObject.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            //参数类型 是 Buff类型
            if (method.getParameterTypes().length == 1 && method.getParameterTypes()[0].getName().equals(bufferedSinkClass.getName())) {
                try {
                    CLogUtils.e("RequestBody writeTo  当前方法的名字是  " + method.getName());
                    method.setAccessible(true);
                    method.invoke(requestBodyObject, BufferedSinkObject);
                    return true;
                } catch (IllegalAccessException e) {
                    CLogUtils.e(" invokeW1riteTo  IllegalAccessException" + e.getMessage());
                    e.printStackTrace();
                    return false;
                } catch (InvocationTargetException e) {
                    CLogUtils.e(" invokeW1riteTo  InvocationTargetException" + e.getMessage());
                    e.printStackTrace();
                    return false;
                }
            }
        }
        throw new Exception("invokeW1riteTo == Null ");
    }


    private Object getBufferObject() throws Exception {
        //父类为null 接口数大于三个
        //本身是final类型  四个字段 两个是 final类型 并且是 static
        for (Class Mclass : Hook.mClassList) {

            if (Modifier.isFinal(Mclass.getModifiers()) && Mclass.getSuperclass().getName().equals(Object.class.getName()) && Mclass.getInterfaces().length >= 3) {
                int StaticCount = 0;
                int LongConut = 0;
                Field[] declaredFields = Mclass.getDeclaredFields();
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
//                    if(Mclass.getName().equals("okio.Buffer")){
//                        CLogUtils.e("StaticCount  "+StaticCount+"LongConut   "+LongConut +" declaredFields 个数   "+declaredFields.length);
//                    }
                    if (StaticCount == 2 && LongConut == 1) {
                        try {
                            return Mclass.newInstance();
                        } catch (InstantiationException e) {
                            CLogUtils.e("getBufferObject  InstantiationException   " + e.getMessage());
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            CLogUtils.e("getBufferObject  IllegalAccessException  " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        throw new Exception("getBufferObject == Null ");
    }

    /**
     * @param requestObject    requestObject
     * @param requestBodyClass requestBodyClass
     * @return RequestBodyObject
     */
    private Object getRequestBodyObject(Object requestObject, Class requestBodyClass) throws Exception {
        //返回类型 是 requestBodyClass 类型
        Method[] declaredMethods = requestObject.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (method.getReturnType().getName().equals(requestBodyClass.getName())) {
                try {
                    method.setAccessible(true);
                    return method.invoke(requestObject);
                } catch (IllegalAccessException e) {
                    CLogUtils.e("getRequestBodyObject  IllegalAccessException    " + e.getMessage());
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    CLogUtils.e("getRequestBodyObject  InvocationTargetException    " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        throw new Exception("getRequestBodyObject == Null ");
    }

    private Class getRequestBodyClass() throws Exception {
        //本身是 abstract 类 里面有2个抽象方法
        for (Class Mclass : Hook.mClassList) {
            if (Modifier.isAbstract(Mclass.getModifiers())) {
                int conut = 0;
                Method[] declaredMethods = Mclass.getDeclaredMethods();
                for (Method method : declaredMethods) {
                    if (Modifier.isAbstract(method.getModifiers())) {
                        conut++;
                    }
                }
                if (conut == 2) {
                    return Mclass;
                }
            }
        }
        throw new Exception("getRequestBodyClass == Null ");
    }

    private Object getResponseObject(Object chainObject, Object RequestObject) throws Exception {
        Method[] declaredMethods = chainObject.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            //Response proceed(Request var1) throws IOException;  参数 个数 1个
            // 返回的类型是 Class 而不是 接口 并且类型是 final类型  并且第一个
            // 参数类型是 Request
            if (parameterTypes.length == 1) {
                if (!method.getReturnType().isInterface() &&
                        Modifier.isFinal(method.getReturnType().getModifiers()) &&
                        parameterTypes[0].getName().equals(RequestObject.getClass().getName())) {
                    try {
                        method.setAccessible(true);
                        return method.invoke(chainObject, RequestObject);
                    } catch (IllegalAccessException e) {
                        CLogUtils.e("getRequestObject  IllegalAccessException    " + e.getMessage());
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        CLogUtils.e("getRequestObject  InvocationTargetException    " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
        throw new Exception("getResponseObject == Null ");
    }

    /**
     * @param responseObject responseObject
     */
    private Object getHeadersInResponse(Object responseObject) throws Exception {
        Class headerClass = getHeaderClass();
        if (headerClass != null) {
            Field[] declaredFields = responseObject.getClass().getDeclaredFields();
            for (Field field : declaredFields) {
                if (field.getType().getName().equals(headerClass.getName())) {
                    try {
                        field.setAccessible(true);
                        return field.get(responseObject);
                    } catch (IllegalArgumentException e) {
                        CLogUtils.e("getHeadersInResponse  IllegalArgumentException " + e.getMessage());
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        CLogUtils.e("getHeadersInResponse  IllegalAccessException " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        } else {
            throw new Exception("getHeadersInResponse 里面 headerClass  ==  Null");
        }
        throw new Exception("getHeadersInResponse == Null ");
    }


    private String getHeadersMethodAndInvoke(Class headerClass, Object Object) throws Exception {
        Method[] declaredMethods = Object.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            //返回 类型 是 headerClass
            if (method.getReturnType().getName().equals(headerClass.getName())) {
                try {
                    method.setAccessible(true);
                    Object Headers = method.invoke(Object);
                    try {
                        Method toString = Headers.getClass().getMethod("toString");
                        toString.setAccessible(true);
                        return (String) toString.invoke(Headers);
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                        CLogUtils.e("getHeadersMethodAndInvoke  NoSuchMethodException " + e.getMessage());
                    }
                } catch (IllegalAccessException e) {
                    CLogUtils.e("getHeadersMethodAndInvoke  IllegalAccessException " + e.getMessage());
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    CLogUtils.e("getHeadersMethodAndInvoke  InvocationTargetException " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        throw new Exception("getHeadersMethodAndInvoke == Null ");
    }


    private Class getHeaderClass() throws Exception {
        for (Class Mclass : Hook.mClassList) {

            if (Modifier.isFinal(Mclass.getModifiers())) {
                //构造方法又2种    Headers(Builder builder)   Headers(String[] namesAndValues)
                if (Mclass.getDeclaredFields().length == 1 && Mclass.getDeclaredConstructors().length == 2) {
                    Field declaredField = Mclass.getDeclaredFields()[0];
                    if (declaredField.getType().getName().equals(String[].class.getName())
                            && Modifier.isFinal(declaredField.getModifiers())
                            && Modifier.isPrivate(declaredField.getModifiers())
                    ) {
                        return Mclass;
                    }
                }
            }
        }
        throw new Exception("getHeaderClass == Null ");
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
        } catch (NoSuchMethodException e) {
            CLogUtils.e("InvokeToString   没有 找到 toString " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            CLogUtils.e("InvokeToString  IllegalAccessException    " + e.getMessage());
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            CLogUtils.e("InvokeToString  InvocationTargetException    " + e.getMessage());
            e.printStackTrace();
        }
        throw new Exception("InvokeToString == Null ");
    }

    private Object getRequestObject(Object chainObject) throws Exception {
        Method[] declaredMethods = chainObject.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            //Request request();  参数 个数 0个 返回的类型是 Class 而不是 接口 并且类型是 final类型
            if (parameterTypes == null || parameterTypes.length == 0) {
                if (!method.getReturnType().isInterface() && Modifier.isFinal(method.getReturnType().getModifiers())) {
                    try {
                        method.setAccessible(true);
                        return method.invoke(chainObject);
                    } catch (IllegalAccessException e) {
                        CLogUtils.e("getRequestObject  IllegalAccessException    " + e.getMessage());
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        CLogUtils.e("getRequestObject  InvocationTargetException    " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
        throw new Exception("getRequestObject == Null ");
    }
}
