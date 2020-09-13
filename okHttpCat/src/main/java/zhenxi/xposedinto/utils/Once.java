package zhenxi.xposedinto.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Run code by only once in life circle of host class.
 * Created by Lymons on 16/7/6.
 */
public class Once {
    private static Map<Integer, Once> runMap = new HashMap<>();
    private static Map<String, Set<Integer>> hostMap = new HashMap<>();

    private Set<String> runSet = new HashSet<>();
    private WeakReference<Object> host;

    private Once(Object host) {
        this.host = new WeakReference<>(host);
    }

    private Once() {
    }

    public static class OnceNg {
        private Once once;
        private boolean ok;

        public OnceNg(Once once, boolean ok) {
            this.once = once;
            this.ok = ok;
        }

        public Once ng(Runnable runnable) {
            if (ok == false) {
                runnable.run();
            }
            return once;
        }
    }

    private static class AppOnce extends Once {
        private static AppOnce _instance;
        private static String PREF_NAME = "ONCE";

        SharedPreferences pref;

        private AppOnce(Context context) {
            pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }

        public static Once instance(Context context) {
            if (_instance == null) {
                _instance = new AppOnce(context);
            }
            return _instance;
        }

        public OnceNg run(@NonNull String tag, @NonNull Runnable runnable) {
            boolean ok = false;
            if (!pref.getBoolean(tag, false)) {
                pref.edit().putBoolean(tag, Boolean.TRUE).commit();
                runnable.run();
                ok = true;
            }
            return new OnceNg(this, ok);
        }
    }


    /**
     * Set host class.
     * @param host
     * @return
     */
    public static Once host(@NonNull Object host) {
        addHost(host);

        Integer hashCode = host.hashCode();
        Once once = runMap.get(hashCode);
        if (once == null) {
            once = new Once(host);
            runMap.put(hashCode, once);
        }
        return once;
    }

    /**
     * get App LifeCircle's once class.
     * @param context
     * @return
     */
    public static Once app(@NonNull Context context) {

        return AppOnce.instance(context.getApplicationContext());

    }

    /**
     * Purge all instance of host class except yours.
     * @return
     */
    public Once self() {
        purgeOthers(host);
        return this;
    }

    /**
     * Run code by tag.
     * @param tag every runnable must specify a tag.
     * @param runnable code you want run only once.
     * @return
     */
    public OnceNg run(@NonNull String tag, @NonNull Runnable runnable) {
        boolean ok = runIfExists(runSet, tag, runnable);
        return new OnceNg(this, ok);
    }

    /**
     * Private methods
     */

    private static void addHost(Object host) {
        String name = host.getClass().getName();
        Integer hashCode = host.hashCode();
        Set<Integer> hosts = hostMap.get(name);
        if (hosts == null) {
            hosts = new HashSet<>();
            hostMap.put(name, hosts);
        }

        hosts.add(hashCode);
    }

    private boolean runIfExists(Set<String> runSet, String tag, Runnable runnable) {
        if (runSet.contains(tag) == false) {
            runSet.add(tag);
            runnable.run();
            return true;
        }
        return false;
    }

    private static void purgeOthers(Object host) {
        if (host == null) {
            return;
        }

        String name = host.getClass().getName();
        Integer hashCode = host.hashCode();
        Set<Integer> hosts = hostMap.get(name);
        if (hosts == null) {
            return;
        }
        for (Integer hash : hosts) {
            if (!Objects.equals(hash, hashCode)) {
                runMap.remove(hash);
            }
        }
    }
}