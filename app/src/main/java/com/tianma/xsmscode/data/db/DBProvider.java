package com.tianma.xsmscode.data.db;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;

import com.github.rove24.xposed.smscode.BuildConfig;
import com.tianma.xsmscode.data.db.entity.AppInfoDao;
import com.tianma.xsmscode.data.db.entity.SmsCodeRuleDao;
import com.tianma.xsmscode.data.db.entity.SmsMsgDao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DBProvider extends ContentProvider {

    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".db.provider";
    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    private static final String PATH_SMS_MSG = "sms_msg";
    private static final String PATH_SMS_CODE_RULE = "sms_code_rule";
    private static final String PATH_APP_INFO = "app_info";

    public static final Uri SMS_MSG_CONTENT_URI =
            Uri.parse("content://" + AUTHORITY + "/" + PATH_SMS_MSG);
    public static final Uri SMS_CODE_RULE_URI =
            Uri.parse("content://" + AUTHORITY + "/" + PATH_SMS_CODE_RULE);
    public static final Uri APP_INFO_URI =
            Uri.parse("content://" + AUTHORITY + "/" + PATH_APP_INFO);

    private static final int SMS_MSG_DIR = 0;
    private static final int SMS_MSG_ID = 1;
    private static final int SMS_CODE_RULE_DIR = 2;
    private static final int SMS_CODE_RULE_ID = 3;
    private static final int APP_INFO_DIR = 4;
    private static final int APP_INFO_ID = 5;


    private static final String TABLE_SMS_MSG = SmsMsgDao.TABLENAME;
    private static final String TABLE_SMS_CODE_RULE = SmsCodeRuleDao.TABLENAME;
    private static final String TABLE_APP_INFO = AppInfoDao.TABLENAME;

    private static final UriMatcher sUriMatcher;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, PATH_SMS_MSG, SMS_MSG_DIR);
        sUriMatcher.addURI(AUTHORITY, PATH_SMS_MSG + "/#", SMS_MSG_ID);

        sUriMatcher.addURI(AUTHORITY, PATH_SMS_CODE_RULE, SMS_CODE_RULE_DIR);
        sUriMatcher.addURI(AUTHORITY, PATH_SMS_CODE_RULE + "/#", SMS_CODE_RULE_ID);

        sUriMatcher.addURI(AUTHORITY, PATH_APP_INFO, APP_INFO_DIR);
        sUriMatcher.addURI(AUTHORITY, PATH_APP_INFO + "/#", APP_INFO_ID);
    }

    private SQLiteDatabase mDatabase;
    private Context mContext;

    @Override
    public boolean onCreate() {
        mContext = getContext();
        mDatabase = DBManager.get(mContext).getSQLiteDatabase();
        return true;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        recordActivation();
        int uriType = sUriMatcher.match(uri);
        long id;
        String path;
        switch (uriType) {
            case SMS_MSG_DIR:
                id = mDatabase.insert(TABLE_SMS_MSG, null, values);
                path = PATH_SMS_MSG + "/" + id;
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
        if (mContext != null) {
            mContext.getContentResolver().notifyChange(uri, null);
        }
        return Uri.parse(path);
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        recordActivation();
        int uriType = sUriMatcher.match(uri);
        String tableName;
        switch (uriType) {
            case SMS_CODE_RULE_DIR:
                tableName = TABLE_SMS_CODE_RULE;
                break;
            case SMS_MSG_DIR:
                tableName = TABLE_SMS_MSG;
                break;
            case APP_INFO_DIR:
                tableName = TABLE_APP_INFO;
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
        return mDatabase.query(tableName, projection, selection, selectionArgs, null, null, sortOrder);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        int uriType = sUriMatcher.match(uri);
        int rowsDeleted;
        switch (uriType) {
            case SMS_MSG_DIR:
                rowsDeleted = mDatabase.delete(TABLE_SMS_MSG, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
        if (rowsDeleted > 0) {
            mContext.getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    private void recordActivation() {
        recordActivation(102, BuildConfig.MODULE_VERSION, "ipc");
    }

    private void recordActivation(int apiVersion, int moduleVersion, String fromPkg) {
        Context ctx = getContext() != null ? getContext() : mContext;
        if (ctx != null) {
            try {
                android.content.SharedPreferences sp =
                        ctx.getSharedPreferences("module_status", Context.MODE_PRIVATE);
                sp.edit()
                        .putBoolean("is_activated", true)
                        .putInt("api_version", apiVersion)
                        .putInt("module_version", moduleVersion)
                        .putString("from_package", fromPkg)
                        .putLong("last_heartbeat", System.currentTimeMillis())
                        .apply();
            } catch (Throwable ignored) {}
        }
    }

    @Nullable
    @Override
    public Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
        if ("get_preferences".equals(method)) {
            Context ctx = getContext();
            if (ctx != null) {
                recordActivation();
                android.content.SharedPreferences pref =
                        ctx.getSharedPreferences(com.tianma.xsmscode.common.constant.PrefConst.PREF_NAME, Context.MODE_PRIVATE);
                Bundle res = new Bundle();

                // 1. Baseline defaults from settings.xml
                res.putBoolean(com.tianma.xsmscode.common.constant.PrefConst.KEY_ENABLE, true);
                res.putBoolean(com.tianma.xsmscode.common.constant.PrefConst.KEY_COPY_TO_CLIPBOARD, false);
                res.putBoolean(com.tianma.xsmscode.common.constant.PrefConst.KEY_SHOW_TOAST, true);
                res.putBoolean(com.tianma.xsmscode.common.constant.PrefConst.KEY_BLOCK_SMS, false);
                res.putBoolean(com.tianma.xsmscode.common.constant.PrefConst.KEY_DEDUPLICATE_SMS, false);
                res.putBoolean(com.tianma.xsmscode.common.constant.PrefConst.KEY_ENABLE_AUTO_INPUT_CODE, true);
                res.putString(com.tianma.xsmscode.common.constant.PrefConst.KEY_AUTO_INPUT_CODE_DELAY, com.tianma.xsmscode.common.constant.PrefConst.KEY_AUTO_INPUT_CODE_DELAY_DEFAULT);
                res.putString(com.tianma.xsmscode.common.constant.PrefConst.KEY_SMSCODE_KEYWORDS, com.tianma.xsmscode.common.constant.PrefConst.SMSCODE_KEYWORDS_DEFAULT);
                res.putBoolean(com.tianma.xsmscode.common.constant.PrefConst.KEY_SHOW_CODE_NOTIFICATION, true);
                res.putBoolean(com.tianma.xsmscode.common.constant.PrefConst.KEY_AUTO_CANCEL_CODE_NOTIFICATION, false);
                res.putBoolean(com.tianma.xsmscode.common.constant.PrefConst.KEY_ENABLE_CODE_RECORDS, true);
                res.putBoolean(com.tianma.xsmscode.common.constant.PrefConst.KEY_MARK_AS_READ, false);
                res.putBoolean(com.tianma.xsmscode.common.constant.PrefConst.KEY_DELETE_SMS, false);
                res.putBoolean(com.tianma.xsmscode.common.constant.PrefConst.KEY_KILL_ME, false);

                // 2. Override with actual user preferences
                java.util.Map<String, ?> map = pref.getAll();
                if (map != null) {
                    for (java.util.Map.Entry<String, ?> entry : map.entrySet()) {
                        String k = entry.getKey();
                        Object v = entry.getValue();
                        if (v instanceof Boolean) res.putBoolean(k, (Boolean) v);
                        else if (v instanceof String) res.putString(k, (String) v);
                        else if (v instanceof Integer) res.putInt(k, (Integer) v);
                        else if (v instanceof Long) res.putLong(k, (Long) v);
                        else if (v instanceof Float) res.putFloat(k, (Float) v);
                    }
                }
                return res;
            }
        } else if ("heartbeat".equals(method) && extras != null) {
            int apiVersion = extras.getInt("apiVersion", 102);
            int moduleVersion = extras.getInt("moduleVersion", BuildConfig.MODULE_VERSION);
            String fromPkg = extras.getString("fromPackage", "");
            long ts = extras.getLong("timestamp", System.currentTimeMillis());

            Context ctx = getContext();
            if (ctx != null) {
                android.content.SharedPreferences sp =
                        ctx.getSharedPreferences("module_status", Context.MODE_PRIVATE);
                sp.edit()
                        .putBoolean("is_activated", true)
                        .putInt("api_version", apiVersion)
                        .putInt("module_version", moduleVersion)
                        .putString("from_package", fromPkg)
                        .putLong("last_heartbeat", ts)
                        .apply();
            }
            Bundle res = new Bundle();
            res.putBoolean("success", true);
            return res;
        }
        return super.call(method, arg, extras);
    }
}
