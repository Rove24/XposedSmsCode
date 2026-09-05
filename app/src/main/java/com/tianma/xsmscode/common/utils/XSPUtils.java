package com.tianma.xsmscode.common.utils;

import com.tianma.xsmscode.common.constant.PrefConst;

import android.content.SharedPreferences;

public class XSPUtils {

    private XSPUtils() {

    }

    /**
     * 总开关是否打开
     */
    public static boolean isEnabled(SharedPreferences preferences) {
        return preferences != null ? preferences.getBoolean(PrefConst.KEY_ENABLE, true) : true;
    }

    /**
     * 日志模式是否是verbose log模式
     */
    public static boolean isVerboseLogMode(SharedPreferences preferences) {
        return preferences != null && preferences.getBoolean(PrefConst.KEY_VERBOSE_LOG_MODE, false);
    }

    /**
     * 自动输入总开关是否打开
     */
    public static boolean autoInputCodeEnabled(SharedPreferences preferences) {
        return preferences != null ? preferences.getBoolean(PrefConst.KEY_ENABLE_AUTO_INPUT_CODE, true) : true;
    }

    /**
     * 自动输入延迟(单位s)
     * @param preferences
     * @return
     */
    public static long getAutoInputCodeDelay(SharedPreferences preferences) {
        if (preferences == null) {
            return 0L;
        }
        String value = preferences.getString(PrefConst.KEY_AUTO_INPUT_CODE_DELAY, PrefConst.KEY_AUTO_INPUT_CODE_DELAY_DEFAULT);
        long delay;
        try {
            delay = Long.parseLong(value);
        } catch (Exception e) {
            delay = Long.parseLong(PrefConst.KEY_AUTO_INPUT_CODE_DELAY_DEFAULT);
        }
        return delay;
    }

    /**
     * 是否应该在复制验证码到系统剪切板之后显示Toast
     */
    public static boolean shouldShowToast(SharedPreferences preferences) {
        return preferences != null ? preferences.getBoolean(PrefConst.KEY_SHOW_TOAST, true) : true;
    }

    /**
     * 获取短信验证码关键字
     */
    public static String getSMSCodeKeywords(SharedPreferences preferences) {
        return preferences != null ? preferences.getString(PrefConst.KEY_SMSCODE_KEYWORDS,
                PrefConst.SMSCODE_KEYWORDS_DEFAULT) : PrefConst.SMSCODE_KEYWORDS_DEFAULT;
    }

    /**
     * 标记为已读是否打开
     */
    public static boolean markAsReadEnabled(SharedPreferences preferences) {
        return preferences != null && preferences.getBoolean(PrefConst.KEY_MARK_AS_READ, false);
    }

    /**
     * 是否删除验证码短信
     */
    public static boolean deleteSmsEnabled(SharedPreferences preferences) {
        return preferences != null && preferences.getBoolean(PrefConst.KEY_DELETE_SMS, false);
    }

    /**
     * 是否复制到剪切板
     */
    public static boolean copyToClipboardEnabled(SharedPreferences preferences) {
        return preferences != null ? preferences.getBoolean(PrefConst.KEY_COPY_TO_CLIPBOARD, false) : false;
    }

    /**
     * 是否记录短信验证码
     */
    public static boolean recordSmsCodeEnabled(SharedPreferences preferences) {
        return preferences != null ? preferences.getBoolean(PrefConst.KEY_ENABLE_CODE_RECORDS, true) : true;
    }

    /**
     * 是否拦截短信通知
     */
    public static boolean blockSmsEnabled(SharedPreferences preferences) {
        return preferences != null && preferences.getBoolean(PrefConst.KEY_BLOCK_SMS, false);
    }

    /**
     * 验证码提取成功后是否杀掉模块进程
     */
    public static boolean killMeEnabled(SharedPreferences preferences) {
        return preferences != null && preferences.getBoolean(PrefConst.KEY_KILL_ME, false);
    }

    /**
     * 是否显示验证码通知
     */
    public static boolean showCodeNotification(SharedPreferences preferences) {
        return preferences != null ? preferences.getBoolean(PrefConst.KEY_SHOW_CODE_NOTIFICATION, true) : true;
    }

    /**
     * 是否自动清除验证码通知
     */
    public static boolean autoCancelCodeNotification(SharedPreferences preferences) {
        return preferences != null && preferences.getBoolean(PrefConst.KEY_AUTO_CANCEL_CODE_NOTIFICATION, false);
    }

    /**
     * 获取验证码通知保留时间
     */
    public static int getNotificationRetentionTime(SharedPreferences preferences) {
        if (preferences == null) {
            return 0;
        }
        String value = preferences.getString(PrefConst.KEY_NOTIFICATION_RETENTION_TIME,
                PrefConst.NOTIFICATION_RETENTION_TIME_DEFAULT);
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 是否过滤掉重复短信
     */
    public static boolean deduplicateSms(SharedPreferences preferences) {
        return preferences != null && preferences.getBoolean(PrefConst.KEY_DEDUPLICATE_SMS, false);
    }
}