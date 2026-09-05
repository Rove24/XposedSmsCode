package com.tianma.xsmscode.common.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 当前Xposed模块相关工具类
 */
public class ModuleUtils {

    private ModuleUtils() {
    }

    /**
     * 返回模块版本 <br/>
     * 注意：该方法被本模块Hook住，返回的值是 BuildConfig.MODULE_VERSION，如果没被Hook则返回-1
     */
    public static int getModuleVersion() {
        XLog.d("getModuleVersion()");
        return -1;
    }

    public static int getModuleVersion(Context context) {
        int ver = getModuleVersion();
        if (ver > 0) {
            return ver;
        }
        if (context != null) {
            try {
                int globalVer = android.provider.Settings.Global.getInt(context.getContentResolver(), "xsmscode_active_version", -1);
                if (globalVer > 0) {
                    return globalVer;
                }
            } catch (Throwable ignored) {}

            try {
                SharedPreferences sp = context.getSharedPreferences("module_status", Context.MODE_PRIVATE);
                if (sp.getBoolean("is_activated", false)) {
                    return sp.getInt("module_version", 13);
                }
            } catch (Throwable ignored) {}
        }
        return -1;
    }

    /**
     * 当前模块是否在XposedInstaller中被启用
     */
    public static boolean isModuleEnabled() {
        return getModuleVersion() > 0;
    }

    public static boolean isModuleEnabled(Context context) {
        if (isModuleEnabled()) {
            return true;
        }
        if (context != null) {
            try {
                int api = android.provider.Settings.Global.getInt(context.getContentResolver(), "xsmscode_active_api", -1);
                if (api >= 100) {
                    return true;
                }
            } catch (Throwable ignored) {}

            try {
                SharedPreferences sp = context.getSharedPreferences("module_status", Context.MODE_PRIVATE);
                if (sp.getBoolean("is_activated", false)) {
                    long last = sp.getLong("last_heartbeat", 0);
                    if (System.currentTimeMillis() - last < 30L * 24 * 3600 * 1000L) {
                        return true;
                    }
                }
            } catch (Throwable ignored) {}
        }
        return false;
    }

    /**
     * 返回当前运行的 Xposed API 版本
     * -1: 未激活, 82: 传统 Xposed, 102: 现代 LibXposed API 102
     */
    public static int getApiVersion() {
        return -1;
    }

    public static int getApiVersion(Context context) {
        int api = getApiVersion();
        if (api > 0) {
            return api;
        }
        if (context != null) {
            try {
                int globalApi = android.provider.Settings.Global.getInt(context.getContentResolver(), "xsmscode_active_api", -1);
                if (globalApi > 0) {
                    return globalApi;
                }
            } catch (Throwable ignored) {}

            try {
                SharedPreferences sp = context.getSharedPreferences("module_status", Context.MODE_PRIVATE);
                if (sp.getBoolean("is_activated", false)) {
                    return sp.getInt("api_version", 102);
                }
            } catch (Throwable ignored) {}
        }
        return -1;
    }

    /**
     * 是否正在运行于 Modern Xposed Module API 102 架构下
     */
    public static boolean isModernApi102() {
        return getApiVersion() >= 100;
    }

    public static boolean isModernApi102(Context context) {
        return getApiVersion(context) >= 100;
    }
}
