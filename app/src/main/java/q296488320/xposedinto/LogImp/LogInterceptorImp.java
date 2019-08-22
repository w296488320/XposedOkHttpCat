package q296488320.xposedinto.LogImp;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;

import q296488320.xposedinto.XpHook.Hook;
import q296488320.xposedinto.utils.CLogUtils;

/**
 * Created by Lyh on
 * 2019/8/22
 */
public class LogInterceptorImp implements InvocationHandler {
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private Object RequestObject;
    private Object ResponseObject ;
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        StringBuilder stringBuffer=new StringBuilder();
        //里面只有 一个方法
        Object ChainObject = args[0];


        RequestObject = getRequestObject(ChainObject);


        if(RequestObject==null){
            CLogUtils.e("getRequestObject   getRequestObject   返回 Null");
            return null;
        }else {
            CLogUtils.e("getRequestObject   拿到 RequestObject  名字是   "+RequestObject.getClass().getName());
        }
        ResponseObject =   getResponseObject(ChainObject,RequestObject);

        if(ResponseObject!=null){
            CLogUtils.e("getRequestObject   拿到 ResponseObject  名字是   "+ResponseObject.getClass().getName());
        }

        stringBuffer.append("\n"+"--------------->>>"+"\n"+InvokeToString(RequestObject)+"\n\n");

        Class headerClass = getHeaderClass();
        if(headerClass!=null){
            CLogUtils.e("getRequestObject  拿到 headerClass  名字是  "+headerClass.getName());
        }
        CLogUtils.e("开始查找 headers 函数 ");
        String headersMethodAndInvoke = getHeadersMethodAndInvoke(headerClass, RequestObject);

        if(headersMethodAndInvoke==null){
            CLogUtils.e("getRequestObject   getHeadersMethodAndInvoke 返回 Null");
            return ResponseObject;
        }else {
            stringBuffer.append(headersMethodAndInvoke);
            CLogUtils.e("getRequestObject   拿到请求 头部信息 ");
        }
         //判断 是否存在 body
//        RequestBody requestBody = request.body();
//        boolean hasRequestBody = requestBody != null;

        //需要先 拿到 RequestBody 类型
      Class RequestBodyClass =  getRequestBodyClass();
      Object RequestBodyObject= getRequestBodyObject(RequestObject,RequestBodyClass);

      if(RequestBodyObject!=null){
          CLogUtils.e("拿到  RequestBodyObject");

//          Buffer buffer = new Buffer();
//          requestBody.writeTo(buffer);
//          Charset charset = UTF8;
//          MediaType contentType = requestBody.contentType();
//          if (contentType != null) {
//              charset = contentType.charset(UTF8);
//          }

          Object bufferObject = getBufferObject();
          if(bufferObject!=null){
              CLogUtils.e("拿到  bufferObject");
              if(invokeW1riteTo(RequestBodyObject,bufferObject)){
                  Charset charset=UTF8;


              }else {
                  CLogUtils.e("没有成功 设置  requestBody.writeTo(buffer) ");
              }
          }
      }



        return ResponseObject;
    }

    private boolean invokeW1riteTo(Object requestBodyObject, Object bufferObject) {
        Method[] declaredMethods = requestBodyObject.getClass().getDeclaredMethods();
        for(Method method:declaredMethods){
            if(Modifier.isAbstract(method.getModifiers())&&method.getParameterTypes().length==1){
                try {
                    CLogUtils.e("RequestBody writeTo  当前方法的名字是  "+method.getName());
                    method.invoke(requestBodyObject,bufferObject);
                    return true;
                } catch (IllegalAccessException e) {
                    CLogUtils.e(" invokeW1riteTo  IllegalAccessException"+e.getMessage());
                    e.printStackTrace();
                    return false;
                } catch (InvocationTargetException e) {
                    CLogUtils.e(" invokeW1riteTo  InvocationTargetException"+e.getMessage());
                    e.printStackTrace();
                    return false;
                }
            }
        }
        return false;
    }


    private Object getBufferObject() {
        //本身是final类型  四个字段 两个是 final类型 并且是 static
        for(Class Mclass: Hook.mClassList){
            int count=0;
            if(Modifier.isFinal(Mclass.getModifiers())) {
                Field[] declaredFields = Mclass.getDeclaredFields();
                if(declaredFields.length==4){
                    for(Field field:declaredFields){
                        if(Modifier.isFinal(field.getModifiers())&&Modifier.isStatic(field.getModifiers())){
                            count++;
                        }
                    }
                    if(count==2){
                        try {
                            return Mclass.newInstance();
                        } catch (InstantiationException e) {
                            CLogUtils.e("getBufferObject  InstantiationException   "+e.getMessage());
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            CLogUtils.e("getBufferObject  IllegalAccessException  "+e.getMessage());
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
        for(Method method:declaredMethods){
            if(method.getReturnType().getName().equals(requestBodyClass.getName())){
                try {
                   return  method.invoke(requestObject);
                } catch (IllegalAccessException e) {
                    CLogUtils.e("getRequestBodyObject  IllegalAccessException    " +e.getMessage());
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    CLogUtils.e("getRequestBodyObject  InvocationTargetException    " +e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        CLogUtils.e("调用 request.body() 失败 ");
        return null;
    }

    private Class getRequestBodyClass() {
        //本身是 abstract 类 里面有2个抽象方法
        for(Class Mclass: Hook.mClassList){
            if(Modifier.isAbstract(Mclass.getModifiers())){
                int conut=0;
                Method[] declaredMethods = Mclass.getDeclaredMethods();
                for(Method method:declaredMethods){
                    if(Modifier.isAbstract(method.getModifiers())){
                        conut++;
                    }
                }
                if(conut==2){
                    return Mclass;
                }
            }
        }
        return null;
    }

    private Object getResponseObject(Object chainObject, Object RequestObject) {
        Method[] declaredMethods = chainObject.getClass().getDeclaredMethods();
        for(Method method:declaredMethods){
            Class<?>[] parameterTypes = method.getParameterTypes();
            //Response proceed(Request var1) throws IOException;  参数 个数 1个 返回的类型是 Class 而不是 接口 并且类型是 final类型  并且第一个 参数类型是 Request
            if(parameterTypes.length==1){
                if(!method.getReturnType().isInterface()&&Modifier.isFinal(method.getReturnType().getModifiers())&&parameterTypes[0].getName().equals(RequestObject.getClass().getName())){
                    try {
                        return method.invoke(chainObject);
                    } catch (IllegalAccessException e) {
                        CLogUtils.e("getRequestObject  IllegalAccessException    " +e.getMessage());
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        CLogUtils.e("getRequestObject  InvocationTargetException    " +e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }

    private String getHeadersMethodAndInvoke(Class headerClass,Object RequestObject) {
        Method[] declaredMethods = RequestObject.getClass().getDeclaredMethods();
        for(Method method :declaredMethods){
            if(method.getReturnType().getName().equals(headerClass.getName())){
                try {
                    Object Headers =  method.invoke(RequestObject);
                    try {
                        return (String) Headers.getClass().getMethod("toString").invoke(Headers);
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                        CLogUtils.e("getHeadersMethodAndInvoke  NoSuchMethodException "+e.getMessage());
                    }
                } catch (IllegalAccessException e) {
                    CLogUtils.e("getHeadersMethodAndInvoke  IllegalAccessException "+e.getMessage());
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    CLogUtils.e("getHeadersMethodAndInvoke  InvocationTargetException "+e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private Class getHeaderClass() {
        for(Class Mclass: Hook.mClassList){
            if(Modifier.isFinal(Mclass.getModifiers())){
                if(Mclass.getDeclaredFields()!=null&&Mclass.getDeclaredFields().length==1){
                    Field declaredField = Mclass.getDeclaredFields()[0];
                    if(declaredField.getType().getName().equals(String.class.getName())&&Modifier.isFinal(declaredField.getModifiers())){
                        return Mclass;
                    }
                }
            }
        }
        return null;
    }

    private String InvokeToString(Object requestObject) {
        try {
            return (String)requestObject.getClass().getMethod("toString").invoke(requestObject);
        } catch (NoSuchMethodException e) {
            CLogUtils.e("InvokeToString   没有 找到 toString " +e.getMessage());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            CLogUtils.e("InvokeToString  IllegalAccessException    " +e.getMessage());
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            CLogUtils.e("InvokeToString  InvocationTargetException    " +e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private Object getRequestObject(Object chainObject) {
        Method[] declaredMethods = chainObject.getClass().getDeclaredMethods();
        for(Method method:declaredMethods){
            Class<?>[] parameterTypes = method.getParameterTypes();
            //Request request();  参数 个数 0个 返回的类型是 Class 而不是 接口 并且类型是 final类型
            if(parameterTypes==null||parameterTypes.length==0){
                if(!method.getReturnType().isInterface()&&Modifier.isFinal(method.getReturnType().getModifiers())){
                    try {
                        return method.invoke(chainObject);
                    } catch (IllegalAccessException e) {
                        CLogUtils.e("getRequestObject  IllegalAccessException    " +e.getMessage());
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        CLogUtils.e("getRequestObject  InvocationTargetException    " +e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }
}
