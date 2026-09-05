package com.tianma.xsmscode.xp.hook.code.action.impl;

import android.content.Context;
import android.os.Bundle;

import com.tianma.xsmscode.common.utils.ClipboardUtils;
import com.tianma.xsmscode.common.utils.XSPUtils;
import com.tianma.xsmscode.data.db.entity.SmsMsg;
import com.tianma.xsmscode.xp.hook.code.action.RunnableAction;

import android.content.SharedPreferences;

/**
 * 将验证码复制到剪切板
 */
public class CopyToClipboardAction extends RunnableAction {

    public CopyToClipboardAction(Context pluginContext, Context phoneContext, SmsMsg smsMsg, SharedPreferences xsp) {
        super(pluginContext, phoneContext, smsMsg, xsp);
    }

    @Override
    public Bundle action() {
        if (XSPUtils.copyToClipboardEnabled(xsp)) {
            copyToClipboard();
        }
        return null;
    }

    private void copyToClipboard() {
        Context targetContext = (mPhoneContext != null) ? mPhoneContext : mPluginContext;
        if (targetContext != null && mSmsMsg != null) {
            com.tianma.xsmscode.common.utils.XLog.i("Executing copyToClipboard for code: %s", mSmsMsg.getSmsCode());
            ClipboardUtils.copyToClipboard(targetContext, mSmsMsg.getSmsCode());
        } else {
            com.tianma.xsmscode.common.utils.XLog.e("copyToClipboard skipped: targetContext or mSmsMsg is null");
        }
    }
}
