package com.tianma.xsmscode.xp.hook.code.action.impl;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.github.rove24.xposed.smscode.R;
import com.tianma.xsmscode.common.constant.NotificationConst;
import com.tianma.xsmscode.common.utils.XLog;
import com.tianma.xsmscode.common.utils.XSPUtils;
import com.tianma.xsmscode.data.db.entity.SmsMsg;
import com.tianma.xsmscode.xp.hook.code.CopyCodeReceiver;
import com.tianma.xsmscode.xp.hook.code.action.CallableAction;

import android.content.SharedPreferences;

/**
 * 显示验证码通知
 */
public class NotifyAction extends CallableAction {

    public static final String NOTIFY_RETENTION_TIME = "notify_retention_time";
    public static final String NOTIFY_ID = "notify_id";

    public NotifyAction(Context pluginContext, Context phoneContext, SmsMsg smsMsg, SharedPreferences xsp) {
        super(pluginContext, phoneContext, smsMsg, xsp);
    }

    @Override
    public Bundle action() {
        if (XSPUtils.showCodeNotification(xsp)) {
            return showCodeNotification(mSmsMsg);
        }
        return null;
    }

    @SuppressLint({"UnspecifiedImmutableFlag", "NotificationPermission"})
    private Bundle showCodeNotification(SmsMsg smsMsg) {
        NotificationManager manager = (NotificationManager) mPhoneContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return null;
        }

        String company = smsMsg.getCompany();
        String smsCode = smsMsg.getSmsCode();
        String title = TextUtils.isEmpty(company) ? smsMsg.getSender() : company;
        String content;
        if (mPluginContext != null) {
            try {
                content = mPluginContext.getString(R.string.code_notification_content, smsCode);
            } catch (Throwable t) {
                content = "验证码：" + smsCode;
            }
        } else {
            content = "验证码：" + smsCode;
        }

        int notificationId = smsMsg.hashCode();

        Intent copyCodeIntent = CopyCodeReceiver.createIntent(smsCode);
        final PendingIntent contentIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            contentIntent = PendingIntent.getBroadcast(mPhoneContext, 0,
                    copyCodeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            contentIntent = PendingIntent.getBroadcast(mPhoneContext, 0,
                    copyCodeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        } else {
            contentIntent = PendingIntent.getBroadcast(mPhoneContext,
                    0, copyCodeIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        try {
            Context notifContext = (mPluginContext != null) ? mPluginContext : mPhoneContext;
            NotificationCompat.Builder builder = new NotificationCompat.Builder(notifContext, NotificationConst.CHANNEL_ID_SMSCODE_NOTIFICATION)
                    .setWhen(System.currentTimeMillis())
                    .setContentTitle(title)
                    .setContentText(content)
                    .setContentIntent(contentIntent)
                    .setAutoCancel(true)
                    .setGroup(NotificationConst.GROUP_KEY_SMSCODE_NOTIFICATION);

            if (mPluginContext != null) {
                try {
                    builder.setSmallIcon(R.drawable.ic_app_icon)
                            .setLargeIcon(BitmapFactory.decodeResource(mPluginContext.getResources(), R.drawable.ic_app_icon))
                            .setColor(ContextCompat.getColor(mPluginContext, R.color.ic_launcher_background));
                } catch (Throwable ignored) {}
            } else {
                builder.setSmallIcon(android.R.drawable.sym_action_chat);
            }

            manager.notify(notificationId, builder.build());
            XLog.d("Show notification succeed");
        } catch (Throwable t) {
            XLog.e("Failed to show notification", t);
        }

        if (XSPUtils.autoCancelCodeNotification(xsp)) {
            long retentionTime = XSPUtils.getNotificationRetentionTime(xsp) * 1000L;
            Bundle bundle = new Bundle();
            bundle.putLong(NOTIFY_RETENTION_TIME, retentionTime);
            bundle.putInt(NOTIFY_ID, notificationId);
            return bundle;
        }
        return null;
    }
}
