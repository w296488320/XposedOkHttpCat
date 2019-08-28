package q296488320.xposedinto.LogImp;

import java.io.EOFException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
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
    private Class mResponseBodyClass;
    private Object mBufferObject;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        try {
            StringBuilder stringBuffer = new StringBuilder();
            //里面只有 一个方法
            Object ChainObject = args[0];


            RequestObject = getRequestObject(ChainObject);


            if (RequestObject == null) {
                CLogUtils.e("getRequestObject   getRequestObject   返回 Null");
                return null;
            } else {
                CLogUtils.e("getRequestObject   拿到 RequestObject  名字是   " + RequestObject.getClass().getName());

            }
            ResponseObject = getResponseObject(ChainObject, RequestObject);

            if (ResponseObject != null) {
                CLogUtils.e("getRequestObject   拿到 ResponseObject  名字是   " + ResponseObject.getClass().getName());
            } else {
                CLogUtils.e("没有找到    拿到 ResponseObject  名字是   ");
            }

            stringBuffer.append("\n" + "--------------->>>" + "\n").append(InvokeToString(RequestObject)).append("\n\n");

            Class headerClass = getHeaderClass();
            if (headerClass != null) {
                CLogUtils.e("getRequestObject  拿到 headerClass  名字是  " + headerClass.getName());
            } else {
                CLogUtils.e("没有找到    拿到 headerClass    名字是   ");
            }
            CLogUtils.e("开始查找 headers 函数 ");
            String RequestHeaders = getHeadersMethodAndInvoke(headerClass, RequestObject);

            if (RequestHeaders == null) {
                CLogUtils.e("getRequestObject   getHeadersMethodAndInvoke 返回 Null");
                return ResponseObject;
            } else {
                stringBuffer.append(RequestHeaders).append("\n");
                CLogUtils.e("getRequestObject   拿到请求 头部信息 ");
            }

            //判断 是否存在 body
//        RequestBody requestBody = request.body();
//        boolean hasRequestBody = requestBody != null;

            //需要先 拿到 RequestBody 类型
            Class RequestBodyClass = getRequestBodyClass();

            Object RequestBodyObject = getRequestBodyObject(RequestObject, RequestBodyClass);
            //Get请求 没有 请求体  可能 存在 为Nulll的 情况
            if (RequestBodyObject != null) {
                //在 不等于 Null的 时候 开始遍历  请求 Body
                CLogUtils.e("拿到  RequestBodyObject");

                //          Buffer buffer = new Buffer();
                //          requestBody.writeTo(buffer);
                //          Charset charset = UTF8;

                //          MediaType contentType = requestBody.contentType();
                //          if (contentType != null) {
                //              charset = contentType.charset(UTF8);
                //          }

                mBufferObject = getBufferObject();

                if (mBufferObject != null) {
                    CLogUtils.e("拿到  bufferObject");
                    if (invokeW1riteTo(RequestBodyObject, mBufferObject)) {
                        //默认是 U8解码
                        Charset charset = UTF8;
                        Class mediaTypeClass = getMediaTypeClass();
                        Charset contentTypeMethodAndInvoke = getContentTypeMethodAndInvoke(mediaTypeClass, RequestBodyObject);
                        if (contentTypeMethodAndInvoke != null) {
                            charset = contentTypeMethodAndInvoke;
                            if (isPlaintext(mBufferObject)) {
                                //logger.log(buffer.readString(charset));
                                stringBuffer.append(getReadStringAndInvoke(mBufferObject, charset)).append("\n");
                            }
                        } else {
                            CLogUtils.e("contentTypeMethodAndInvoke  == null ");
                        }
                    } else {
                        CLogUtils.e("没有成功 设置  requestBody.writeTo(buffer) ");
                    }
                }
            }

            long startNs = System.nanoTime();//返回的是纳秒

            long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

            mResponseBodyClass = getResponseBodyClass();

//        ResponseBody responseBody = response.body();
//        long contentLength = responseBody.contentLength();
//        String bodySize = contentLength != -1 ? contentLength + "-byte" : "unknown-length";
            if (mResponseBodyClass != null) {
                Object responseBodyObject = getResponseBodyMethodAndInvoke(mResponseBodyClass, ResponseObject);

                int contentLength = getResponseBodyContentLengthMethodAndInvoke(mResponseBodyClass, responseBodyObject);

                String bodySize = contentLength != -1 ? contentLength + "-byte" : "unknown-length";

                //            logger.log("<-- "
                //                    + response.code()
                //                    + (response.message().isEmpty() ? "" : ' ' + response.message())
                //                    + ' ' + response.request().url()
                //                    + " (" + tookMs + "ms" + (!logHeaders ? ", " + bodySize + " body" : "") + ')');


                stringBuffer.append("<<<---------------" + "\n").append(InvokeToString(ResponseObject)).append(bodySize).append("    ").append(" bodySize").append("\n");
                //            Headers headers = response.headers();
                //            for (int i = 0, count = headers.size(); i < count; i++) {
                //                logHeader(headers, i);
                //            }


                String ResponseHeaders = getHeadersMethodAndInvoke(headerClass, ResponseObject);

                if (ResponseHeaders == null) {
                    CLogUtils.e("ResponseHeaders   getHeadersMethodAndInvoke 返回 Null");
                    return ResponseObject;
                } else {
                    stringBuffer.append(ResponseHeaders).append("\n");
                    CLogUtils.e("ResponseHeaders   拿到请求 头部信息 ");
                }
                // HttpHeaders.hasBody(response)
                if (!ResponseHasBody(ResponseObject)) {
                    stringBuffer.append("<<<--------------- END HTTP").append("\n");
                } else if (bodyHasUnknownEncoding(ResponseObject)) {
                    stringBuffer.append("<<<--------------- 解码 不支持 省略了 编码 正文 ").append("\n");
                } else {
                    // 存在 返回体的 时候 并且支持解码

                    //                BufferedSource source = responseBody.source();
                    //                source.request(Long.MAX_VALUE); // Buffer the entire body.

                    //                Buffer buffer = source.getBuffer();

                    Object BufferedSourceObject = getSourceAndInvoke(responseBodyObject);
                    if (BufferedSourceObject != null) {
                        getBufferedSourceRequestMethodAndInvoke(BufferedSourceObject);
                        Object buffer = getBufferMethodInBufferedSourceAndInvoke(BufferedSourceObject, mBufferObject.getClass());
                        if (buffer != null) {
                            if (!isPlaintext(buffer)) {
                                stringBuffer.append("<<<---------------   -byte body omittedppend(").append("\n");
                            }
                            if (contentLength != 0) {
                                //logger.log(buffer.clone().readString(charset));
                                getBufferCloneMethodAndInvoke(buffer);
                            }


                        } else {
                            CLogUtils.e("getBufferMethodInBufferedSourceAndInvoke  获取 buffer  == null   ");
                        }
                    } else {
                        CLogUtils.e("BufferedSourceObject  == null   ");
                    }
                }
            } else {
                CLogUtils.e("getResponseBodyClass  返回值   responseBodyClass   == null   ");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseObject;
    }

    /**
     * 重新实现 一次  public Buffer 里面 clone()
     *
     * @param buffer
     */
    private Object getBufferCloneMethodAndInvoke(Object buffer) throws Exception {
        Object result = buffer.getClass().newInstance();
        if (result == null) {
            throw new Exception("getBufferCloneMethodAndInvoke result==Null ");
        }
        Class segmentClass = getSegmentClass();
        Field[] declaredFields = buffer.getClass().getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.getType().getName().equals(long.class.getName())) {
                field.setAccessible(true);
                long size = field.getLong(buffer);
                if (size == 0) {
                    return result;
                }
            }
        }
        throw new Exception("getBufferCloneMethodAndInvoke  没有 找到 ");
    }

    private Class getSegmentClass() {

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
     * @param BufferedSourceObject BufferedSourceObject
     * @param mBufferObject        mBuffer字节码
     * @return BufferObject
     */
    private Object getBufferMethodInBufferedSourceAndInvoke(Object BufferedSourceObject, Class mBufferObject) throws Exception {
        Method[] declaredMethods = BufferedSourceObject.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (method.getReturnType().getName().equals(mBufferObject.getName()) && method.getParameterTypes().length == 0) {
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
     *
     * @param bufferedSourceObject
     */
    private void getBufferedSourceRequestMethodAndInvoke(Object bufferedSourceObject) throws Exception {
        Method[] declaredMethods = bufferedSourceObject.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (method.getParameterTypes().length == 1 &&
                    method.getReturnType().getName().equals(boolean.class.getName()) &&
                    method.getParameterTypes()[0].getName().equals(long.class.getName())
            ) {
                method.setAccessible(true);
                try {
                    method.invoke(bufferedSourceObject, Long.MAX_VALUE);
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
     * public abstract @Nullable MediaType contentType();
     * public abstract long contentLength();
     * public abstract BufferedSource source();
     *
     * @param responseBodyObject
     */
    private Object getSourceAndInvoke(Object responseBodyObject) throws Exception {
        Class mediaTypeClass = getMediaTypeClass();
        if (mediaTypeClass != null) {
            Method[] declaredMethods = mResponseBodyClass.getDeclaredMethods();
            for (Method method : declaredMethods) {
                if (Modifier.isAbstract(method.getModifiers())) {
                    if (!method.getReturnType().getName().equals(long.class.getName()) &&
                            !method.getReturnType().getName().equals(mediaTypeClass.getName())) {
                        try {
                            method.setAccessible(true);
                            return method.invoke(responseBodyObject);
                        } catch (IllegalAccessException e) {
                            CLogUtils.e("getSourceAndInvoke   IllegalAccessException " + e.getMessage());
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            CLogUtils.e("getSourceAndInvoke   IllegalAccessException " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        throw new Exception("getSourceAndInvoke  没有 找到 ");
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
     * @return
     */
    private boolean bodyHasUnknownEncoding(Object ResponseObject) throws Exception {
        try {
            return HeaderGetContent_Encoding(ResponseObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new Exception("bodyHasUnknownEncoding  没有 找到 ");
    }


    public static final int HTTP_CONTINUE = 100;

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

        Object responseInRequestMethodAndInvoke = getResponseInRequestMethodAndInvoke(ResponseObject);
        if (responseInRequestMethodAndInvoke != null) {
            String methodFieldInRequest = getMethodFieldInRequest(responseInRequestMethodAndInvoke);
            if (methodFieldInRequest.equals("HEAD")) {
                return false;
            }
            int responseCode = getResponseCode(responseInRequestMethodAndInvoke);
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


    private boolean HeaderGetContent_Encoding(Object responseObject) throws Exception {
        Object headersInResponse = getHeadersInResponse(responseObject);
        if (headersInResponse != null) {
            String contentEncoding = getHeadersGetMethodAndInvoke(headersInResponse, "Content-Encoding");
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
                    method.getParameterTypes().length == 1 && method.getParameterTypes()[0].getName().equals(String.class.getName())
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

    private int getResponseCode(Object responseInRequestMethodAndInvoke) throws Exception {
        Field[] declaredFields = responseInRequestMethodAndInvoke.getClass().getDeclaredFields();
        for (Field field : declaredFields) {
            //final 并且是 String
            if (field.getType().getName().equals(int.class.getName()) && Modifier.isFinal(field.getModifiers())) {
                field.setAccessible(true);
                try {
                    return field.getInt(responseInRequestMethodAndInvoke);
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
    private String getMethodFieldInRequest(Object responseInRequestMethodAndInvoke) {
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
        return "";
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
        //本身 是抽象类  抽象方法 大于2 个
        //其中有一个 抽象方法 返回的 类型是 MediaType 参数 为 0个
        Class mediaTypeClass = getMediaTypeClass();
        if (mediaTypeClass != null) {
            Method[] declaredMethods = mediaTypeClass.getDeclaredMethods();
            for (Class Mclass : Hook.mClassList) {
                if (Modifier.isAbstract(Mclass.getModifiers())
                        && declaredMethods.length >= 2
                ) {
                    for (Method method : declaredMethods) {
                        if (method.getReturnType().getName().equals(mediaTypeClass.getName()) && method.getParameterTypes().length == 0) {
                            return Mclass;
                        }
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
            Field[] fields = bufferObject.getClass().getFields();
            long bufferObjectSize = -1L;
            for (Field field : fields) {
                if (field.getType().getName().equals(long.class.getName())) {
                    field.setAccessible(true);
                    bufferObjectSize = field.getLong(bufferObject);
                }
            }


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
        Method[] declaredMethods = bufferObject.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            //返回值是 Buff
            if (method.getReturnType().getName().equals(bufferObject.getClass().getName())) {
                //参数 长度是 3 参数 类型 Buff long longg
                if (method.getParameterTypes().length == 3
                        && method.getParameterTypes()[0].getName().equals(bufferObject.getClass().getName())
                        && method.getParameterTypes()[1].getName().equals(long.class.getName())
                        && method.getParameterTypes()[2].getName().equals(long.class.getName())
                ) {
                    method.setAccessible(true);
                    try {
                        method.invoke(bufferObject, prefix, 0L, byteCount);
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

    private Charset getContentTypeMethodAndInvoke(Class mediaTypeClass, Object requestBodyObject) throws Exception {
        Method[] declaredMethods = requestBodyObject.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            //返回  类型是 mediaTypeClass 参数 无
            if (method.getParameterTypes().length == 0 && method.getReturnType().getName().equals(mediaTypeClass.getName())) {
                method.setAccessible(true);
                try {
                    return (Charset) method.invoke(requestBodyObject);
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

//        private static final Pattern PARAMETER = Pattern.compile(";\\s*(?:([a-zA-Z0-9-!#$%&'*+.^_`{|}~]+)=(?:([a-zA-Z0-9-!#$%&'*+.^_`{|}~]+)|\"([^\"]*)\"))?");
//        private static final Pattern TYPE_SUBTYPE = Pattern.compile("([a-zA-Z0-9-!#$%&'*+.^_`{|}~]+)/([a-zA-Z0-9-!#$%&'*+.^_`{|}~]+)");
//        private final String charset;
//        private final String mediaType;
//        private final String subtype;
//        private final String type;

        Class<Pattern> patternClass = Pattern.class;

        for (Class mClass : Hook.mClassList) {
            if (Modifier.isFinal(mClass.getModifiers())) {
                Field[] declaredFields = mClass.getDeclaredFields();
                if (declaredFields.length != 0) {
                    int StringFinalType = 0;
                    int PatternTypeCount = 0;
                    for (Field field : declaredFields) {
                        if (field.getType().getName().equals(String.class.getName()) && Modifier.isFinal(field.getModifiers()) && Modifier.isPrivate(field.getModifiers())) {
                            StringFinalType++;
                        }
                        if (field.getType().getName().equals(patternClass.getName())) {
                            PatternTypeCount++;
                        }
                        if (StringFinalType >= 4 && PatternTypeCount == 2) {
                            return mClass;
                        }
                    }
                }
            }
        }
        throw new Exception("getMediaTypeClass == Null ");
    }

    private boolean invokeW1riteTo(Object requestBodyObject, Object bufferObject) throws Exception {
        Method[] declaredMethods = requestBodyObject.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            //参数类型 是 Buff类型
            if (method.getParameterTypes().length == 1 && method.getParameterTypes()[0].getName().equals(bufferObject.getClass().getName())) {
                try {
                    CLogUtils.e("RequestBody writeTo  当前方法的名字是  " + method.getName());
                    method.setAccessible(true);
                    method.invoke(requestBodyObject, bufferObject);
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
        //本身是final类型  四个字段 两个是 final类型 并且是 static
        for (Class Mclass : Hook.mClassList) {
            int count = 0;
            if (Modifier.isFinal(Mclass.getModifiers())) {
                Field[] declaredFields = Mclass.getDeclaredFields();
                if (declaredFields.length == 4) {
                    for (Field field : declaredFields) {
                        if (Modifier.isFinal(field.getModifiers()) && Modifier.isStatic(field.getModifiers())) {
                            count++;
                        }
                    }
                    if (count == 2) {
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
            //Response proceed(Request var1) throws IOException;  参数 个数 1个 返回的类型是 Class 而不是 接口 并且类型是 final类型  并且第一个 参数类型是 Request
            if (parameterTypes.length == 1) {
                if (!method.getReturnType().isInterface() && Modifier.isFinal(method.getReturnType().getModifiers()) && parameterTypes[0].getName().equals(RequestObject.getClass().getName())) {
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
        throw new Exception("getResponseObject == Null ");
    }

    /**
     * @param responseObject responseObject
     * @return
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
                if (Mclass.getDeclaredFields() != null && Mclass.getDeclaredFields().length == 1) {
                    Field declaredField = Mclass.getDeclaredFields()[0];
                    if (declaredField.getType().getName().equals(String.class.getName()) && Modifier.isFinal(declaredField.getModifiers())) {
                        return Mclass;
                    }
                }
            }
        }
        throw new Exception("getHeaderClass == Null ");
    }

    private String InvokeToString(Object Object) throws Exception {
        try {
            Method method = Object.getClass().getMethod("toString");
            method.setAccessible(true);
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
