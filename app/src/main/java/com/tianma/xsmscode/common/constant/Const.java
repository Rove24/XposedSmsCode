package com.tianma.xsmscode.common.constant;

import com.github.rove24.xposed.smscode.BuildConfig;

/**
 * Constant about 3rd app
 */
public interface Const {

    /* Xposed SmsCode begin */
    String HOME_ACTIVITY_ALIAS = BuildConfig.APPLICATION_ID + ".HomeActivityAlias";

    String PROJECT_SOURCE_CODE_URL = "https://github.com/Rove24/XposedSmsCode";
    String PROJECT_GITHUB_LATEST_RELEASE_URL = PROJECT_SOURCE_CODE_URL + "/releases/latest";
    String PROJECT_DOC_BASE_URL = "https://tianma8023.github.io/SmsCode";
    String DOC_SMS_CODE_RULE_HELP ="sms_code_rule_help";
    /* Xposed SmsCode end */

    /* Taichi begin */
    String TAICHI_PACKAGE_NAME = "me.weishu.exp";
    String TAICHI_MAIN_PAGE = "me.weishu.exp.ui.MainActivity";
    /* Taichi end */

    /* Xposed Installer begin */
    String XPOSED_PACKAGE = "de.robv.android.xposed.installer";
    // Old Xposed installer
    String XPOSED_OPEN_SECTION_ACTION = XPOSED_PACKAGE + ".OPEN_SECTION";
    String XPOSED_EXTRA_SECTION = "section";
    // New Xposed installer
    String XPOSED_ACTIVITY = XPOSED_PACKAGE + ".WelcomeActivity";
    String XPOSED_EXTRA_FRAGMENT = "fragment";
    /* Xposed Installer end */

    /* CoolApk */
    String COOL_MARKET_PACKAGE_NAME = "com.coolapk.market";
    /* CoolApk end */
}
