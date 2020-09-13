package zhenxi.xposedinto.classloader;

import dalvik.system.DexClassLoader;

/**
 * Created by Lyh on
 * 2019/11/14
 */
public class MyClassLoader extends DexClassLoader {
    public MyClassLoader(String dexPath, String optimizedDirectory, String librarySearchPath, ClassLoader parent) {
        super(dexPath, optimizedDirectory, librarySearchPath, parent);
    }
}
