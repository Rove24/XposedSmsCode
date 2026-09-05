package com.tianma.xsmscode.common.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

import com.github.rove24.xposed.smscode.BuildConfig;
import com.github.rove24.xposed.smscode.R;
import com.tianma.xsmscode.common.constant.Const;
import com.tianma.xsmscode.xp.hook.permission.PermissionGranterHook;
import com.tianma.xsmscode.xp.hook.code.SmsHandlerHook;

import androidx.annotation.IntDef;

/**
 * 包相关工具类
 */
public class PackageUtils {

    /**
     * not installed
     */
    private final static int PACKAGE_NOT_INSTALLED = 0;
    /**
     * installed & disabled
     */
    private final static int PACKAGE_DISABLED = 1;
    /**
     * installed & enabled
     */
    private final static int PACKAGE_ENABLED = 2;

    @IntDef({PACKAGE_NOT_INSTALLED, PACKAGE_DISABLED, PACKAGE_ENABLED})
    @interface PackageState {
    }

    private PackageUtils() {
    }

    private static @PackageState int checkPackageState(Context context, String packageName) {
        if (isPackageEnabled(context, packageName)) {
            // installed & enabled
            return PACKAGE_ENABLED;
        } else {
            if (isPackageInstalled(context, packageName)) {
                // installed & disabled
                return PACKAGE_DISABLED;
            } else {
                // not installed
                return PACKAGE_NOT_INSTALLED;
            }
        }
    }

    /**
     * 指定的包名对应的App是否已安装
     */
    public static boolean isPackageInstalled(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(packageName, 0);
            return packageInfo != null;
        } catch (PackageManager.NameNotFoundException e) {
            // ignore
        }
        return false;
    }

    /**
     * 对应包名的应用是否已启用
     */
    public static boolean isPackageEnabled(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            return appInfo != null && appInfo.enabled;
        } catch (PackageManager.NameNotFoundException e) {
            // ignore
        }
        return false;
    }

    public enum Section {
        INSTALL("install", 0),
        MODULES("modules", 1);

        private final String mSection;
        private final int mFragment;

        Section(String section, int fragment) {
            mSection = section;
            mFragment = fragment;
        }
    }

    private static boolean startOldXposedActivity(Context context, String section) {
        Intent intent = new Intent(Const.XPOSED_OPEN_SECTION_ACTION);
        intent.putExtra(Const.XPOSED_EXTRA_SECTION, section);
        try {
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean startNewXposedActivity(Context context, int fragment) {
        Intent intent = new Intent();
        intent.setClassName(Const.XPOSED_PACKAGE, Const.XPOSED_ACTIVITY);
        intent.putExtra(Const.XPOSED_EXTRA_FRAGMENT, fragment);
        try {
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean startXposedActivity(Context context, Section section) {
        return startNewXposedActivity(context, section.mFragment)
                || startOldXposedActivity(context, section.mSection);
    }

    private static boolean checkTaiChiExists(Context context) {
        int taichiPkgState = checkPackageState(context, Const.TAICHI_PACKAGE_NAME);
        if (taichiPkgState == PACKAGE_ENABLED) {
            // installed & enabled
            return true;
        } else if (taichiPkgState == PACKAGE_NOT_INSTALLED) {
            Toast.makeText(context, R.string.taichi_install_prompt, Toast.LENGTH_SHORT).show();
        } else if (taichiPkgState == PACKAGE_DISABLED) {
            Toast.makeText(context, R.string.taichi_enable_prompt, Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    public static void startTaiChiActivity(Context context) {
        if (checkTaiChiExists(context)) {
            // installed & enabled
            Intent intent = new Intent();
            intent.setClassName(Const.TAICHI_PACKAGE_NAME, Const.TAICHI_MAIN_PAGE);
            context.startActivity(intent);
        }
    }

    public static void startCheckModuleInTaiChi(Context context) {
        if (checkTaiChiExists(context)) {
            Intent intent = new Intent("me.weishu.exp.ACTION_MODULE_MANAGE");
            intent.setData(Uri.parse("package:" + BuildConfig.APPLICATION_ID));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    /**
     * 在太极中勾选本模块相关的应用
     */
    public static void startAddAppsInTaiChi(Context context) {
        if (checkTaiChiExists(context)) {
            Intent intent = new Intent("me.weishu.exp.ACTION_ADD_APP");
            String uriStr = "package:" +
                    SmsHandlerHook.ANDROID_PHONE_PACKAGE + "|" +
                    PermissionGranterHook.ANDROID_PACKAGE;
            intent.setData(Uri.parse(uriStr));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    public static void showAppDetailsInCoolApk(Context context) {
        int packageState = checkPackageState(context, Const.COOL_MARKET_PACKAGE_NAME);
        if (packageState == PACKAGE_ENABLED) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse("market://details?id=" + BuildConfig.APPLICATION_ID));
            intent.setPackage(Const.COOL_MARKET_PACKAGE_NAME);
            context.startActivity(intent);
        } else if (packageState == PACKAGE_DISABLED) {
            Toast.makeText(context, R.string.coolapk_enable_prompt, Toast.LENGTH_SHORT).show();
        } else if (packageState == PACKAGE_NOT_INSTALLED) {
            Toast.makeText(context, R.string.coolapk_install_prompt, Toast.LENGTH_SHORT).show();
        }
    }

}
