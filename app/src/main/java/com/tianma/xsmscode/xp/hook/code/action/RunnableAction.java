package com.tianma.xsmscode.xp.hook.code.action;

import android.content.Context;

import com.tianma.xsmscode.data.db.entity.SmsMsg;

import android.content.SharedPreferences;

/**
 * Runnable + Action + Callable
 */
public abstract class RunnableAction extends CallableAction implements Runnable {

    public RunnableAction(Context pluginContext, Context phoneContext, SmsMsg smsMsg, SharedPreferences xsp) {
        super(pluginContext, phoneContext, smsMsg, xsp);
    }

    @Override
    public void run() {
        call();
    }
}
