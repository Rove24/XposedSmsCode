package com.tianma.xsmscode.xp.hook.code.action.impl;

import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import com.github.rove24.xposed.smscode.R;
import com.tianma.xsmscode.common.utils.XSPUtils;
import com.tianma.xsmscode.data.db.entity.SmsMsg;
import com.tianma.xsmscode.xp.hook.code.action.RunnableAction;

import android.content.SharedPreferences;

/**
 * 显示验证码Toast
 */
public class ToastAction extends RunnableAction {

    public ToastAction(Context pluginContext, Context phoneContext, SmsMsg smsMsg, SharedPreferences xsp) {
        super(pluginContext, phoneContext, smsMsg, xsp);
    }

    @Override
    public Bundle action() {
        if (XSPUtils.shouldShowToast(xsp)) {
            showCodeToast();
        }
        return null;
    }

    private void showCodeToast() {
        String text;
        if (mPluginContext != null) {
            try {
                text = mPluginContext.getString(R.string.current_sms_code, mSmsMsg.getSmsCode());
            } catch (Throwable t) {
                text = "当前验证码：" + mSmsMsg.getSmsCode();
            }
        } else {
            text = "当前验证码：" + mSmsMsg.getSmsCode();
        }
        Context target = (mPhoneContext != null) ? mPhoneContext : mPluginContext;
        if (target != null) {
            Toast.makeText(target, text, Toast.LENGTH_LONG).show();
        }
    }
}
