package com.tianma.xsmscode.common.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.tianma.xsmscode.common.constant.PrefConst;

import java.util.Map;

/**
 * Utility for real-time cross-process preference synchronization between the
 * module UI and the hooked processes (phone, system_server).
 */
public class PrefSyncUtils {

    public static final String ACTION_PREF_CHANGED = "com.github.rove24.xposed.smscode.ACTION_PREF_CHANGED";
    public static final String ACTION_REQUEST_PREF_SYNC = "com.github.rove24.xposed.smscode.ACTION_REQUEST_PREF_SYNC";

    private PrefSyncUtils() {
    }

    public static void broadcastPreferences(Context context) {
        if (context == null) return;
        try {
            SharedPreferences sp = context.getSharedPreferences(PrefConst.PREF_NAME, Context.MODE_PRIVATE);
            Bundle bundle = toBundle(sp);
            Intent intent = new Intent(ACTION_PREF_CHANGED);
            intent.putExtras(bundle);
            context.sendBroadcast(intent);

            try {
                Intent phoneIntent = new Intent(ACTION_PREF_CHANGED);
                phoneIntent.putExtras(bundle);
                phoneIntent.setPackage("com.android.phone");
                context.sendBroadcast(phoneIntent);
            } catch (Throwable ignored) {}

            XLog.d("Broadcasted updated preferences to hooked processes");
        } catch (Throwable t) {
            XLog.w("Failed to broadcast preferences: " + t.getMessage());
        }
    }

    public static Bundle toBundle(SharedPreferences sp) {
        Bundle bundle = new Bundle();
        if (sp == null) return bundle;

        // Baseline defaults matching settings.xml
        bundle.putBoolean(PrefConst.KEY_ENABLE, true);
        bundle.putBoolean(PrefConst.KEY_COPY_TO_CLIPBOARD, false);
        bundle.putBoolean(PrefConst.KEY_SHOW_TOAST, true);
        bundle.putBoolean(PrefConst.KEY_BLOCK_SMS, false);
        bundle.putBoolean(PrefConst.KEY_DEDUPLICATE_SMS, false);
        bundle.putBoolean(PrefConst.KEY_ENABLE_AUTO_INPUT_CODE, true);
        bundle.putString(PrefConst.KEY_AUTO_INPUT_CODE_DELAY, PrefConst.KEY_AUTO_INPUT_CODE_DELAY_DEFAULT);
        bundle.putString(PrefConst.KEY_SMSCODE_KEYWORDS, PrefConst.SMSCODE_KEYWORDS_DEFAULT);
        bundle.putBoolean(PrefConst.KEY_SHOW_CODE_NOTIFICATION, true);
        bundle.putBoolean(PrefConst.KEY_AUTO_CANCEL_CODE_NOTIFICATION, false);
        bundle.putBoolean(PrefConst.KEY_ENABLE_CODE_RECORDS, true);
        bundle.putBoolean(PrefConst.KEY_MARK_AS_READ, false);
        bundle.putBoolean(PrefConst.KEY_DELETE_SMS, false);
        bundle.putBoolean(PrefConst.KEY_KILL_ME, false);

        // Overlay actual user values
        Map<String, ?> all = sp.getAll();
        if (all != null) {
            for (Map.Entry<String, ?> entry : all.entrySet()) {
                String k = entry.getKey();
                Object v = entry.getValue();
                if (v instanceof Boolean) bundle.putBoolean(k, (Boolean) v);
                else if (v instanceof String) bundle.putString(k, (String) v);
                else if (v instanceof Integer) bundle.putInt(k, (Integer) v);
                else if (v instanceof Long) bundle.putLong(k, (Long) v);
                else if (v instanceof Float) bundle.putFloat(k, (Float) v);
            }
        }
        return bundle;
    }
}
