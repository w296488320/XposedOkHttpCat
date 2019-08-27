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

/**
 * Created by ZhenXi on
 * 2019/8/22
 */
public class LogInterceptorImp implements InvocationHandler {
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private Object RequestObject;
    private Object ResponseObject;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

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

        stringBuffer.append("\n" + "--------------->>>" + "\n" + InvokeToString(RequestObject) + "\n\n");

        Class headerClass = getHeaderClass();
        if (headerClass != null) {
            CLogUtils.e("getRequestObject  拿到 headerClass  名字是  " + headerClass.getName());
        } else {
            CLogUtils.e("没有找到    拿到 headerClass    名字是   ");
        }
        CLogUtils.e("开始查找 headers 函数 ");
        String headersMethodAndInvoke = getHeadersMethodAndInvoke(headerClass, RequestObject);

        if (headersMethodAndInvoke == null) {
            CLogUtils.e("getRequestObject   getHeadersMethodAndInvoke 返回 Null");
            return ResponseObject;
        } else {
            stringBuffer.append(headersMethodAndInvoke);
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

            Object bufferObject = getBufferObject();
            if (bufferObject != null) {
                CLogUtils.e("拿到  bufferObject");
                if (invokeW1riteTo(RequestBodyObject, bufferObject)) {
                    //默认是 U8解码
                    Charset charset = UTF8;
                    Class mediaTypeClass = getMediaTypeClass();
                    Charset contentTypeMethodAndInvoke = getContentTypeMethodAndInvoke(mediaTypeClass, RequestBodyObject);
                    if (contentTypeMethodAndInvoke != null) {
                        charset = contentTypeMethodAndInvoke;
                        if (isPlaintext(bufferObject)) {
                            //logger.log(buffer.readString(charset));
                            stringBuffer.append(getReadStringAndInvoke(bufferObject, charset) + "\n");
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
        
        Class responseBodyClass = getResponseBodyClass();

        getResponseBodyMethodAndInvoke(ResponseObject);


        return ResponseObject;
    }

    private Class getResponseBodyClass() {
        //本身 是抽象类  抽象方法 大于2 个
        //其中有一个 抽象方法 返回的 类型是 MediaType 参数 为 0个
        Class mediaTypeClass = getMediaTypeClass();
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
        return null;

    }

    private void getResponseBodyMethodAndInvoke(Object responseObject) {
        Class mediaTypeClass = getMediaTypeClass();


    }

    /**
     * 获取  request.method()
     *
     * @param requestObject
     */
    private String getMethodInRequest(Object requestObject) {
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
        return "";
    }

    private String getReadStringAndInvoke(Object bufferObject, Charset charset) {
        Method[] declaredMethods = bufferObject.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (method.getReturnType().equals(String.class.getName())
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
        return "";
    }


    public boolean exhausted(Object prefix) throws Exception {
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
        throw new Exception("没找到 this.size");
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
        return false;
    }

    private Method getReadUtf8CodePointMethod(Object prefix) {
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
        return null;
    }

    private void getCopyToMethodAndInvoke(Object bufferObject, Object prefix, long byteCount) {
        Method[] declaredMethods = bufferObject.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            //返回值是 Buff
            if (method.getReturnType().equals(bufferObject.getClass().getName())) {
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
    }

    private Charset getContentTypeMethodAndInvoke(Class mediaTypeClass, Object requestBodyObject) {
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
        return null;
    }

    /**
     * okhttp3.MediaType
     */
    private Class getMediaTypeClass() {

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
                        if (field.getType().getName().equals(String.class) && Modifier.isFinal(field.getModifiers()) && Modifier.isPrivate(field.getModifiers())) {
                            StringFinalType++;
                        }
                        if (field.getType().equals(patternClass.getName())) {
                            PatternTypeCount++;
                        }
                        if (StringFinalType >= 4 && PatternTypeCount == 2) {
                            return mClass;
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean invokeW1riteTo(Object requestBodyObject, Object bufferObject) {
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
        return false;
    }


    private Object getBufferObject() {
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
        return null;
    }

    /**
     * @param requestObject
     * @param requestBodyClass
     * @return
     */
    private Object getRequestBodyObject(Object requestObject, Class requestBodyClass) {
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
        CLogUtils.e("调用 request.body() 失败 ");
        return null;
    }

    private Class getRequestBodyClass() {
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
        return null;
    }

    private Object getResponseObject(Object chainObject, Object RequestObject) {
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
        return null;
    }

    private String getHeadersMethodAndInvoke(Class headerClass, Object RequestObject) {
        Method[] declaredMethods = RequestObject.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (method.getReturnType().getName().equals(headerClass.getName())) {
                try {
                    Object Headers = method.invoke(RequestObject);
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
        return null;
    }

    private Class getHeaderClass() {
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
        return null;
    }

    private String InvokeToString(Object requestObject) {
        try {
            Method method = requestObject.getClass().getMethod("toString");
            method.setAccessible(true);
            return (String) method.invoke(requestObject);
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
        return null;
    }

    private Object getRequestObject(Object chainObject) {
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
        return null;
    }
}
