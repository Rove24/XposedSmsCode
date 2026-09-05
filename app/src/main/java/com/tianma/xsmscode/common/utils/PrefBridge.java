package com.tianma.xsmscode.common.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.github.rove24.xposed.smscode.BuildConfig;
import com.tianma.xsmscode.common.constant.PrefConst;
import com.tianma.xsmscode.data.db.DBProvider;

public class PrefBridge {

    private static volatile SharedPreferences sCachedPreferences = null;

    private PrefBridge() {
    }

    public static synchronized void updateCachedPreferences(Bundle bundle, Context context) {
        if (bundle != null && !bundle.isEmpty()) {
            sCachedPreferences = new RemotePreferences(bundle);
            XLog.d("Updated in-memory cached preferences");
            if (context != null) {
                try {
                    SharedPreferences localSp = context.getSharedPreferences("xsmscode_phone_prefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = localSp.edit();
                    for (String key : bundle.keySet()) {
                        Object val = bundle.get(key);
                        if (val instanceof Boolean) editor.putBoolean(key, (Boolean) val);
                        else if (val instanceof String) editor.putString(key, (String) val);
                        else if (val instanceof Integer) editor.putInt(key, (Integer) val);
                        else if (val instanceof Long) editor.putLong(key, (Long) val);
                        else if (val instanceof Float) editor.putFloat(key, (Float) val);
                    }
                    editor.apply();
                    XLog.d("Persisted preferences to local phone storage");
                } catch (Throwable ignored) {}
            }
        }
    }

    public static SharedPreferences loadPreferences(Context context) {
        if (sCachedPreferences != null) {
            return sCachedPreferences;
        }

        if (context == null) {
            try {
                Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
                java.lang.reflect.Method currentAppMethod = activityThreadClass.getDeclaredMethod("currentApplication");
                Object app = currentAppMethod.invoke(null);
                if (app instanceof Context) {
                    context = (Context) app;
                }
            } catch (Throwable ignored) {}
        }

        // 1. Check local phone storage (saved from previous broadcasts)
        if (context != null) {
            try {
                SharedPreferences localSp = context.getSharedPreferences("xsmscode_phone_prefs", Context.MODE_PRIVATE);
                if (!localSp.getAll().isEmpty()) {
                    sCachedPreferences = localSp;
                    XLog.d("Loaded preferences from local phone storage");
                    return sCachedPreferences;
                }
            } catch (Throwable ignored) {}
        }

        // 2. Check DBProvider IPC
        if (context != null) {
            try {
                Bundle b = context.getContentResolver().call(DBProvider.AUTHORITY_URI, "get_preferences", null, null);
                if (b != null && !b.isEmpty()) {
                    XLog.d("Loaded preferences from DBProvider IPC successfully");
                    sCachedPreferences = new RemotePreferences(b);
                    return sCachedPreferences;
                }
            } catch (Throwable t) {
                XLog.w("Failed to load preferences from DBProvider: " + t.getMessage());
            }
        }
        try {
            de.robv.android.xposed.XSharedPreferences xsp =
                    new de.robv.android.xposed.XSharedPreferences(BuildConfig.APPLICATION_ID, PrefConst.PREF_NAME);
            xsp.reload();
            if (!xsp.getAll().isEmpty()) {
                XLog.d("Loaded preferences from XSharedPreferences");
                return xsp;
            }
        } catch (Throwable ignored) {}

        // Fallback defaults — must match settings.xml android:defaultValue exactly
        Bundle fallback = new Bundle();
        fallback.putBoolean(PrefConst.KEY_ENABLE, true);                        // xml: true
        fallback.putBoolean(PrefConst.KEY_COPY_TO_CLIPBOARD, false);            // xml: false
        fallback.putBoolean(PrefConst.KEY_SHOW_TOAST, true);                    // xml: true
        fallback.putBoolean(PrefConst.KEY_BLOCK_SMS, false);                    // xml: false
        fallback.putBoolean(PrefConst.KEY_DEDUPLICATE_SMS, false);              // xml: false
        fallback.putBoolean(PrefConst.KEY_ENABLE_AUTO_INPUT_CODE, true);        // xml: true
        fallback.putString(PrefConst.KEY_AUTO_INPUT_CODE_DELAY, PrefConst.KEY_AUTO_INPUT_CODE_DELAY_DEFAULT);
        fallback.putString(PrefConst.KEY_SMSCODE_KEYWORDS, PrefConst.SMSCODE_KEYWORDS_DEFAULT);
        fallback.putBoolean(PrefConst.KEY_SHOW_CODE_NOTIFICATION, true);        // xml: true
        fallback.putBoolean(PrefConst.KEY_AUTO_CANCEL_CODE_NOTIFICATION, false);// xml: false
        fallback.putBoolean(PrefConst.KEY_ENABLE_CODE_RECORDS, true);           // xml: true
        fallback.putBoolean(PrefConst.KEY_MARK_AS_READ, false);                 // xml: false
        fallback.putBoolean(PrefConst.KEY_DELETE_SMS, false);                   // xml: false (implicit)
        fallback.putBoolean(PrefConst.KEY_KILL_ME, false);                      // xml: false
        XLog.d("Loaded preferences using safe defaults (last resort)");
        return new RemotePreferences(fallback);
    }
}
