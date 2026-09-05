package com.tianma.xsmscode.common.utils;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Read-only SharedPreferences backed by an Android Bundle (from ContentResolver.call)
 */
public class RemotePreferences implements SharedPreferences {

    private final Bundle mBundle;

    public RemotePreferences(Bundle bundle) {
        mBundle = (bundle != null) ? bundle : new Bundle();
    }

    @Override
    public Map<String, ?> getAll() {
        Map<String, Object> map = new HashMap<>();
        for (String key : mBundle.keySet()) {
            map.put(key, mBundle.get(key));
        }
        return map;
    }

    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        if (mBundle.containsKey(key)) {
            Object obj = mBundle.get(key);
            return (obj != null) ? String.valueOf(obj) : defValue;
        }
        return defValue;
    }

    @Nullable
    @Override
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        return defValues;
    }

    @Override
    public int getInt(String key, int defValue) {
        if (mBundle.containsKey(key)) {
            Object obj = mBundle.get(key);
            if (obj instanceof Number) {
                return ((Number) obj).intValue();
            }
            if (obj instanceof String) {
                try {
                    return Integer.parseInt((String) obj);
                } catch (Exception ignored) {}
            }
        }
        return defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
        if (mBundle.containsKey(key)) {
            Object obj = mBundle.get(key);
            if (obj instanceof Number) {
                return ((Number) obj).longValue();
            }
            if (obj instanceof String) {
                try {
                    return Long.parseLong((String) obj);
                } catch (Exception ignored) {}
            }
        }
        return defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
        if (mBundle.containsKey(key)) {
            Object obj = mBundle.get(key);
            if (obj instanceof Number) {
                return ((Number) obj).floatValue();
            }
            if (obj instanceof String) {
                try {
                    return Float.parseFloat((String) obj);
                } catch (Exception ignored) {}
            }
        }
        return defValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        if (mBundle.containsKey(key)) {
            Object obj = mBundle.get(key);
            if (obj instanceof Boolean) {
                return (Boolean) obj;
            }
            if (obj instanceof String) {
                return Boolean.parseBoolean((String) obj);
            }
        }
        return defValue;
    }

    @Override
    public boolean contains(String key) {
        return mBundle.containsKey(key);
    }

    @Override
    public Editor edit() {
        throw new UnsupportedOperationException("RemotePreferences is read-only");
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
    }
}
