package com.tianma.xsmscode.xp;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.provider.Telephony;
import android.util.Log;

import androidx.annotation.NonNull;

import com.github.rove24.xposed.smscode.BuildConfig;
import com.github.rove24.xposed.smscode.R;
import com.tianma.xsmscode.common.constant.NotificationConst;
import com.tianma.xsmscode.common.constant.PrefConst;
import com.tianma.xsmscode.common.utils.ModuleUtils;
import com.tianma.xsmscode.common.utils.NotificationUtils;
import com.tianma.xsmscode.common.utils.XLog;
import com.tianma.xsmscode.common.utils.XSPUtils;
import com.tianma.xsmscode.xp.hook.code.CodeWorker;
import com.tianma.xsmscode.xp.hook.code.CopyCodeReceiver;
import com.tianma.xsmscode.xp.hook.code.ParseResult;
import com.tianma.xsmscode.xp.hook.permission.PermissionGranterHook;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

/**
 * Modern Xposed Module entry (API 102) for LibXposed / LSPosed Modern API.
 */
public class ModernHookEntry extends XposedModule {

    private static final String TAG = "XSmsCode-API102";
    private static final String ANDROID_PHONE_PACKAGE = "com.android.phone";
    private static final String SYSTEM_SERVER_PACKAGE = "android";
    private static final String SMSCODE_PACKAGE = BuildConfig.APPLICATION_ID;
    private static final String SMS_HANDLER_CLASS = "com.android.internal.telephony.InboundSmsHandler";
    private static final int EVENT_BROADCAST_COMPLETE = 3;

    private Context mPhoneContext;
    private Context mPluginContext;
    private final ThreadLocal<Boolean> mInDispatch = new ThreadLocal<>();

    @Override
    public void onModuleLoaded(@NonNull ModuleLoadedParam param) {
        log(Log.INFO, TAG, String.format(Locale.getDefault(),
                "Loaded in %s, framework: %s (%s) API %d",
                param.getProcessName(), getFrameworkName(), getFrameworkVersion(), getApiVersion()));
    }

    @Override
    public void onPackageReady(@NonNull PackageReadyParam param) {
        String packageName = param.getPackageName();
        ClassLoader classLoader = param.getClassLoader();

        if (SMSCODE_PACKAGE.equals(packageName)) {
            hookOwnModule(classLoader);
        } else if (ANDROID_PHONE_PACKAGE.equals(packageName)) {
            hookSmsHandler(classLoader);
        } else if (SYSTEM_SERVER_PACKAGE.equals(packageName) || "system".equals(packageName)) {
            hookSystemServer(classLoader);
        }
    }

    @Override
    public void onSystemServerStarting(@NonNull SystemServerStartingParam param) {
        hookSystemServer(param.getClassLoader());
    }

    private void hookOwnModule(ClassLoader classLoader) {
        try {
            Class<?> moduleUtilsClass = Class.forName("com.tianma.xsmscode.common.utils.ModuleUtils", true, classLoader);
            Method getModuleVersionMethod = moduleUtilsClass.getDeclaredMethod("getModuleVersion");
            hook(getModuleVersionMethod).intercept(chain -> BuildConfig.MODULE_VERSION);

            Method getApiVersionMethod = moduleUtilsClass.getDeclaredMethod("getApiVersion");
            hook(getApiVersionMethod).intercept(chain -> 102);

            log(Log.INFO, TAG, "Hooked ModuleUtils: reported module version " + BuildConfig.MODULE_VERSION + ", API 102");
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "Failed to hook ModuleUtils in own package", t);
        }
    }

    private void hookSmsHandler(ClassLoader classLoader) {
        try {
            // 1. Attempt immediate context acquisition if phone Application is already created
            try {
                Class<?> activityThreadClass = Class.forName("android.app.ActivityThread", true, classLoader);
                Method currentAppMethod = activityThreadClass.getDeclaredMethod("currentApplication");
                Object app = currentAppMethod.invoke(null);
                if (app instanceof Context) {
                    initPhoneContext((Context) app);
                }
            } catch (Throwable ignored) {}

            // 2. Hook Instrumentation#callApplicationOnCreate to capture Application context immediately
            try {
                Class<?> instrClass = Class.forName("android.app.Instrumentation", true, classLoader);
                for (Method m : instrClass.getDeclaredMethods()) {
                    if ("callApplicationOnCreate".equals(m.getName()) && m.getParameterTypes().length == 1) {
                        hook(m).intercept(chain -> {
                            Object result = chain.proceed();
                            List<Object> args = chain.getArgs();
                            if (args.size() > 0 && args.get(0) instanceof Context) {
                                initPhoneContext((Context) args.get(0));
                            }
                            return result;
                        });
                        log(Log.INFO, TAG, "Hooked Instrumentation#callApplicationOnCreate in com.android.phone");
                        break;
                    }
                }
            } catch (Throwable t) {
                log(Log.WARN, TAG, "Failed to hook Instrumentation#callApplicationOnCreate: " + t.getMessage());
            }

            // 3. Hook Application.onCreate in com.android.phone
            try {
                Class<?> appClass = Class.forName("android.app.Application", true, classLoader);
                Method onCreateMethod = appClass.getDeclaredMethod("onCreate");
                hook(onCreateMethod).intercept(chain -> {
                    Object result = chain.proceed();
                    if (chain.getThisObject() instanceof Context) {
                        initPhoneContext((Context) chain.getThisObject());
                    }
                    return result;
                });
                log(Log.INFO, TAG, "Hooked Application#onCreate in com.android.phone");
            } catch (Throwable t) {
                log(Log.WARN, TAG, "Failed to hook Application#onCreate in phone: " + t.getMessage());
            }

            // 4. Hook PhoneApp#onCreate if present
            try {
                Class<?> phoneAppClass = Class.forName("com.android.phone.PhoneApp", true, classLoader);
                for (Method m : phoneAppClass.getDeclaredMethods()) {
                    if ("onCreate".equals(m.getName()) && m.getParameterTypes().length == 0) {
                        hook(m).intercept(chain -> {
                            Object result = chain.proceed();
                            if (chain.getThisObject() instanceof Context) {
                                initPhoneContext((Context) chain.getThisObject());
                            }
                            return result;
                        });
                        log(Log.INFO, TAG, "Hooked PhoneApp#onCreate");
                        break;
                    }
                }
            } catch (Throwable ignored) {}

            Class<?> handlerClass = Class.forName(SMS_HANDLER_CLASS, true, classLoader);

            // 5. Hook constructors to capture Context
            Constructor<?>[] constructors = handlerClass.getDeclaredConstructors();
            for (Constructor<?> ctor : constructors) {
                hook(ctor).intercept(chain -> {
                    Object result = chain.proceed();
                    List<Object> args = chain.getArgs();
                    for (Object arg : args) {
                        if (arg instanceof Context) {
                            initPhoneContext((Context) arg);
                            break;
                        }
                    }
                    return result;
                });
            }
            log(Log.INFO, TAG, "Hooked InboundSmsHandler constructors");

            // 6. Hook ALL dispatchIntent overloads to ensure compatibility across all Android versions and OEM ROMs
            int hookedDispatchCount = 0;
            for (Method method : handlerClass.getDeclaredMethods()) {
                if ("dispatchIntent".equals(method.getName())) {
                    int receiverIdx = -1;
                    Class<?>[] paramTypes = method.getParameterTypes();
                    for (int i = 0; i < paramTypes.length; i++) {
                        if (BroadcastReceiver.class.isAssignableFrom(paramTypes[i])) {
                            receiverIdx = i;
                            break;
                        }
                    }
                    final int finalReceiverIdx = receiverIdx;
                    hook(method).intercept(chain -> {
                        if (Boolean.TRUE.equals(mInDispatch.get())) {
                            return chain.proceed();
                        }
                        try {
                            mInDispatch.set(Boolean.TRUE);
                            List<Object> args = chain.getArgs();
                            if (args.size() > 0 && args.get(0) instanceof Intent) {
                                Intent intent = (Intent) args.get(0);
                                if (Telephony.Sms.Intents.SMS_DELIVER_ACTION.equals(intent.getAction())) {
                                    Object receiver = (finalReceiverIdx >= 0 && finalReceiverIdx < args.size()) ? args.get(finalReceiverIdx) : null;
                                    boolean block = handleSmsDeliver(chain.getThisObject(), intent, receiver);
                                    if (block) {
                                        log(Log.INFO, TAG, "SMS message blocked by XposedSmsCode");
                                        return null;
                                    }
                                }
                            }
                            return chain.proceed();
                        } finally {
                            mInDispatch.remove();
                        }
                    });
                    hookedDispatchCount++;
                }
            }
            log(Log.INFO, TAG, "Hooked " + hookedDispatchCount + " InboundSmsHandler#dispatchIntent overloads");
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "Failed to hook InboundSmsHandler", t);
        }
    }

    private synchronized void initPhoneContext(Context context) {
        if (mPhoneContext == null && context != null) {
            mPhoneContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
            try {
                mPluginContext = mPhoneContext.createPackageContext(SMSCODE_PACKAGE, Context.CONTEXT_IGNORE_SECURITY);
            } catch (Throwable t) {
                log(Log.WARN, TAG, "createPackageContext failed, fallback to mPhoneContext: " + t.getMessage());
                mPluginContext = mPhoneContext;
            }
            try {
                initNotificationChannel();
                registerPrefChangeReceiver(mPhoneContext);
                registerCopyCodeReceiver();
                sendHeartbeat(mPhoneContext, ANDROID_PHONE_PACKAGE);
                registerActivationReceivers(mPhoneContext);
                requestPrefSync(mPhoneContext);
                log(Log.INFO, TAG, "Phone context initialized successfully");
            } catch (Throwable t) {
                log(Log.ERROR, TAG, "Failed to initialize phone context / plugin context", t);
            }
        }
    }

    private void registerPrefChangeReceiver(Context context) {
        try {
            BroadcastReceiver rx = new BroadcastReceiver() {
                @Override
                public void onReceive(Context ctx, Intent intent) {
                    if (intent == null) return;
                    android.os.Bundle extras = intent.getExtras();
                    if (extras != null) {
                        com.tianma.xsmscode.common.utils.PrefBridge.updateCachedPreferences(extras, ctx != null ? ctx : mPhoneContext);
                        registerCopyCodeReceiver();
                        log(Log.INFO, TAG, "Preferences synchronized in real-time in phone process");
                    }
                }
            };
            IntentFilter filter = new IntentFilter("com.github.rove24.xposed.smscode.ACTION_PREF_CHANGED");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(rx, filter, Context.RECEIVER_EXPORTED);
            } else {
                context.registerReceiver(rx, filter);
            }
            log(Log.INFO, TAG, "Registered pref change receiver in phone process");
        } catch (Throwable t) {
            log(Log.WARN, TAG, "Failed to register pref change receiver: " + t.getMessage());
        }
    }

    private void requestPrefSync(Context context) {
        try {
            Intent req = new Intent("com.github.rove24.xposed.smscode.ACTION_REQUEST_PREF_SYNC");
            req.setPackage(SMSCODE_PACKAGE);
            context.sendBroadcast(req);
            log(Log.INFO, TAG, "Requested pref sync from module app");
        } catch (Throwable ignored) {}
    }

    private void registerActivationReceivers(Context context) {
        try {
            BroadcastReceiver rx = new BroadcastReceiver() {
                @Override
                public void onReceive(Context ctx, Intent intent) {
                    sendHeartbeat(mPhoneContext != null ? mPhoneContext : ctx, ANDROID_PHONE_PACKAGE);
                }
            };
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.github.rove24.xposed.smscode.ACTION_PING_ACTIVATION");
            filter.addAction(Intent.ACTION_USER_UNLOCKED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(rx, filter, Context.RECEIVER_EXPORTED);
            } else {
                context.registerReceiver(rx, filter);
            }
            log(Log.INFO, TAG, "Registered activation ping/unlock receivers in phone process");
        } catch (Throwable t) {
            log(Log.WARN, TAG, "Failed to register activation receivers: " + t.getMessage());
        }
    }

    private void sendHeartbeat(Context context, String fromPkg) {
        if (context == null) return;
        new Thread(() -> {
            // 1. Write Settings.Global directly (if permissible)
            try {
                android.provider.Settings.Global.putInt(context.getContentResolver(), "xsmscode_active_api", 102);
                android.provider.Settings.Global.putInt(context.getContentResolver(), "xsmscode_active_version", BuildConfig.MODULE_VERSION);
                android.provider.Settings.Global.putLong(context.getContentResolver(), "xsmscode_active_time", System.currentTimeMillis());
                log(Log.INFO, TAG, "Updated Settings.Global activation status from " + fromPkg);
            } catch (Throwable t) {
                log(Log.WARN, TAG, "Failed to write Settings.Global from " + fromPkg + ": " + t.getMessage());
            }

            // 2. Call DBProvider IPC (if permissible)
            try {
                android.os.Bundle extras = new android.os.Bundle();
                extras.putInt("apiVersion", 102);
                extras.putInt("moduleVersion", BuildConfig.MODULE_VERSION);
                extras.putString("fromPackage", fromPkg);
                extras.putLong("timestamp", System.currentTimeMillis());
                context.getContentResolver().call(com.tianma.xsmscode.data.db.DBProvider.AUTHORITY_URI, "heartbeat", null, extras);
                log(Log.INFO, TAG, "Sent activation heartbeat from " + fromPkg);
            } catch (Throwable t) {
                log(Log.WARN, TAG, "Failed to send activation heartbeat: " + t.getMessage());
            }

            // 3. Send PONG broadcast back to the app
            try {
                Intent pong = new Intent("com.github.rove24.xposed.smscode.ACTION_PONG_ACTIVATION");
                pong.putExtra("apiVersion", 102);
                pong.putExtra("moduleVersion", BuildConfig.MODULE_VERSION);
                pong.setPackage(SMSCODE_PACKAGE);
                context.sendBroadcast(pong);
                log(Log.INFO, TAG, "Sent ACTION_PONG_ACTIVATION to " + SMSCODE_PACKAGE);
            } catch (Throwable ignored) {}
        }, "xsmscode-heartbeat").start();
    }

    private Context getContextFromHandler(Object handler) {
        if (handler == null) return null;
        Class<?> clazz = handler.getClass();
        while (clazz != null && clazz != Object.class) {
            try {
                Field f = clazz.getDeclaredField("mContext");
                f.setAccessible(true);
                Object obj = f.get(handler);
                if (obj instanceof Context) {
                    return (Context) obj;
                }
            } catch (Throwable ignored) {}
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    private SharedPreferences getModulePreferences() {
        Context ctx = mPhoneContext;
        if (ctx == null) {
            try {
                Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
                Method currentAppMethod = activityThreadClass.getDeclaredMethod("currentApplication");
                Object app = currentAppMethod.invoke(null);
                if (app instanceof Context) {
                    ctx = (Context) app;
                }
            } catch (Throwable ignored) {}
        }
        return com.tianma.xsmscode.common.utils.PrefBridge.loadPreferences(ctx);
    }

    private void initNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mPhoneContext != null && mPluginContext != null) {
            try {
                String channelId = NotificationConst.CHANNEL_ID_SMSCODE_NOTIFICATION;
                String channelName = mPluginContext.getString(R.string.channel_name_smscode_notification);
                NotificationUtils.createNotificationChannel(mPhoneContext, channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
                log(Log.INFO, TAG, "Initialized notification channel");
            } catch (Throwable t) {
                log(Log.ERROR, TAG, "Failed to init notification channel", t);
            }
        }
    }

    private void registerCopyCodeReceiver() {
        if (mPhoneContext != null) {
            try {
                SharedPreferences sp = getModulePreferences();
                if (XSPUtils.showCodeNotification(sp)) {
                    CopyCodeReceiver.registerMe(mPhoneContext);
                    log(Log.INFO, TAG, "Registered CopyCodeReceiver");
                } else {
                    CopyCodeReceiver.unregisterMe(mPhoneContext);
                    log(Log.INFO, TAG, "Unregistered CopyCodeReceiver (notification disabled)");
                }
            } catch (Throwable t) {
                log(Log.ERROR, TAG, "Failed to update CopyCodeReceiver", t);
            }
        }
    }

    private boolean handleSmsDeliver(Object inboundSmsHandler, Intent intent, Object smsReceiver) {
        try {
            if (mPhoneContext == null && inboundSmsHandler != null) {
                Context ctx = getContextFromHandler(inboundSmsHandler);
                if (ctx != null) {
                    initPhoneContext(ctx);
                }
            }

            Context pluginCtx = (mPluginContext != null) ? mPluginContext : mPhoneContext;
            SharedPreferences sp = getModulePreferences();
            CodeWorker worker = new CodeWorker(pluginCtx, mPhoneContext, intent, sp);
            ParseResult parseResult = worker.parse();
            if (mPhoneContext != null) {
                sendHeartbeat(mPhoneContext, ANDROID_PHONE_PACKAGE);
            }
            if (parseResult != null && parseResult.isBlockSms()) {
                deleteRawTableAndSendMessage(inboundSmsHandler, smsReceiver);
                return true;
            }
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "Error in handleSmsDeliver", t);
        }
        return false;
    }

    private void deleteRawTableAndSendMessage(Object inboundSmsHandler, Object smsReceiver) {
        long token = Binder.clearCallingIdentity();
        try {
            deleteFromRawTable(inboundSmsHandler, smsReceiver);
        } catch (Throwable e) {
            log(Log.ERROR, TAG, "Error when deleting SMS from raw table", e);
        } finally {
            Binder.restoreCallingIdentity(token);
        }

        try {
            Method sendMessageMethod = inboundSmsHandler.getClass().getMethod("sendMessage", int.class);
            sendMessageMethod.invoke(inboundSmsHandler, EVENT_BROADCAST_COMPLETE);
            log(Log.INFO, TAG, "Sent EVENT_BROADCAST_COMPLETE");
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "Failed to send EVENT_BROADCAST_COMPLETE", t);
        }
    }

    private void deleteFromRawTable(Object inboundSmsHandler, Object smsReceiver) throws Exception {
        if (smsReceiver == null) return;
        Field deleteWhereField = smsReceiver.getClass().getDeclaredField("mDeleteWhere");
        deleteWhereField.setAccessible(true);
        Object deleteWhere = deleteWhereField.get(smsReceiver);

        Field deleteWhereArgsField = smsReceiver.getClass().getDeclaredField("mDeleteWhereArgs");
        deleteWhereArgsField.setAccessible(true);
        Object deleteWhereArgs = deleteWhereArgsField.get(smsReceiver);

        Method deleteMethod = null;
        for (Method m : inboundSmsHandler.getClass().getDeclaredMethods()) {
            if ("deleteFromRawTable".equals(m.getName())) {
                deleteMethod = m;
                deleteMethod.setAccessible(true);
                break;
            }
        }

        if (deleteMethod != null) {
            if (deleteMethod.getParameterTypes().length == 3) {
                deleteMethod.invoke(inboundSmsHandler, deleteWhere, deleteWhereArgs, 2);
            } else if (deleteMethod.getParameterTypes().length == 2) {
                deleteMethod.invoke(inboundSmsHandler, deleteWhere, deleteWhereArgs);
            }
        }
    }

    private final java.util.concurrent.atomic.AtomicBoolean mSystemServerHooked = new java.util.concurrent.atomic.AtomicBoolean(false);

    private void hookSystemServer(ClassLoader classLoader) {
        if (!mSystemServerHooked.compareAndSet(false, true)) {
            log(Log.INFO, TAG, "System server already hooked, skipping duplicate hook");
            return;
        }

        try {
            final int sdkInt = Build.VERSION.SDK_INT;
            if (sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                new com.tianma.xsmscode.xp.hook.permission.PermissionManagerServiceHook34(classLoader).startHook();
            } else if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
                new com.tianma.xsmscode.xp.hook.permission.PermissionManagerServiceHook33(classLoader).startHook();
            } else if (sdkInt >= Build.VERSION_CODES.S) {
                new com.tianma.xsmscode.xp.hook.permission.PermissionManagerServiceHook31(classLoader).startHook();
            } else if (sdkInt >= Build.VERSION_CODES.R) {
                new com.tianma.xsmscode.xp.hook.permission.PermissionManagerServiceHook30(classLoader).startHook();
            } else {
                new com.tianma.xsmscode.xp.hook.permission.PermissionManagerServiceHook(classLoader).startHook();
            }
            log(Log.INFO, TAG, "System server permissions granted successfully");
        } catch (Throwable t) {
            log(Log.WARN, TAG, "Failed to hook system server permissions: " + t.getMessage());
        }

        try {
            Class<?> amsClass = Class.forName("com.android.server.am.ActivityManagerService", true, classLoader);
            for (Method m : amsClass.getDeclaredMethods()) {
                if ("systemReady".equals(m.getName())) {
                    hook(m).intercept(chain -> {
                        Object res = chain.proceed();
                        try {
                            Context sysCtx = getSystemContextFromAms(chain.getThisObject());
                            if (sysCtx != null) {
                                initSystemContext(sysCtx);
                            }
                        } catch (Throwable t) {
                            log(Log.WARN, TAG, "Error in systemReady hook: " + t.getMessage());
                        }
                        return res;
                    });
                    log(Log.INFO, TAG, "Hooked ActivityManagerService#systemReady");
                    break;
                }
            }
        } catch (Throwable t) {
            log(Log.WARN, TAG, "Failed to hook ActivityManagerService#systemReady: " + t.getMessage());
        }

        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread", true, classLoader);
            Method currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread");
            Object at = currentActivityThreadMethod.invoke(null);
            if (at != null) {
                Method getSystemContextMethod = at.getClass().getDeclaredMethod("getSystemContext");
                Object sysCtxObj = getSystemContextMethod.invoke(at);
                if (sysCtxObj instanceof Context) {
                    initSystemContext((Context) sysCtxObj);
                }
            }
        } catch (Throwable ignored) {}

        // Hook InputManagerService#injectInputEvent to bypass INJECT_EVENTS permission check
        try {
            Class<?> imsClass = Class.forName("com.android.server.input.InputManagerService", true, classLoader);
            java.util.Set<Method> targetMethods = new java.util.HashSet<>();
            for (Method m : imsClass.getDeclaredMethods()) {
                String name = m.getName();
                if (name.startsWith("injectInput") || name.contains("injectInputEvent")) {
                    targetMethods.add(m);
                }
            }
            for (Method m : imsClass.getMethods()) {
                String name = m.getName();
                if (name.startsWith("injectInput") || name.contains("injectInputEvent")) {
                    targetMethods.add(m);
                }
            }
            for (Method m : targetMethods) {
                try {
                    hook(m).intercept(chain -> {
                        long token = Binder.clearCallingIdentity();
                        try {
                            return chain.proceed();
                        } finally {
                            Binder.restoreCallingIdentity(token);
                        }
                    });
                } catch (Throwable ignored) {}
            }
            log(Log.INFO, TAG, "Hooked " + targetMethods.size() + " InputManagerService#injectInput* methods via LibXposed API");
        } catch (Throwable t) {
            log(Log.WARN, TAG, "Failed to hook InputManagerService via Modern API: " + t.getMessage());
        }

        // Grant INJECT_EVENTS in ActivityManagerService as fallback
        try {
            Class<?> amsClass = Class.forName("com.android.server.am.ActivityManagerService", true, classLoader);
            for (Method m : amsClass.getDeclaredMethods()) {
                if ("checkPermission".equals(m.getName())) {
                    hook(m).intercept(chain -> {
                        List<Object> args = chain.getArgs();
                        if (args.size() >= 2 && "android.permission.INJECT_EVENTS".equals(args.get(0))) {
                            Object uidObj = args.get(args.size() - 1);
                            if (uidObj instanceof Integer) {
                                int uid = (Integer) uidObj;
                                if ((uid % 100000) == 1001) {
                                    return android.content.pm.PackageManager.PERMISSION_GRANTED;
                                }
                            }
                        }
                        return chain.proceed();
                    });
                }
            }
        } catch (Throwable ignored) {}

        // Grant INJECT_EVENTS in PackageManagerService as fallback
        try {
            Class<?> pmsClass = Class.forName("com.android.server.pm.PackageManagerService", true, classLoader);
            for (Method m : pmsClass.getDeclaredMethods()) {
                if ("checkUidPermission".equals(m.getName())) {
                    hook(m).intercept(chain -> {
                        List<Object> args = chain.getArgs();
                        if (args.size() >= 2 && "android.permission.INJECT_EVENTS".equals(args.get(0))) {
                            Object uidObj = args.get(1);
                            if (uidObj instanceof Integer) {
                                int uid = (Integer) uidObj;
                                if ((uid % 100000) == 1001) {
                                    return android.content.pm.PackageManager.PERMISSION_GRANTED;
                                }
                            }
                        }
                        return chain.proceed();
                    });
                }
            }
        } catch (Throwable ignored) {}
    }

    private final java.util.concurrent.atomic.AtomicBoolean mSystemContextInitialized = new java.util.concurrent.atomic.AtomicBoolean(false);

    private Context getSystemContextFromAms(Object ams) {
        if (ams != null) {
            Class<?> cur = ams.getClass();
            while (cur != null && cur != Object.class) {
                try {
                    Field mContextField = cur.getDeclaredField("mContext");
                    mContextField.setAccessible(true);
                    Object val = mContextField.get(ams);
                    if (val instanceof Context) {
                        return (Context) val;
                    }
                } catch (Throwable ignored) {}
                cur = cur.getSuperclass();
            }
        }
        try {
            Class<?> atClass = Class.forName("android.app.ActivityThread");
            Method curAtMethod = atClass.getDeclaredMethod("currentActivityThread");
            Object at = curAtMethod.invoke(null);
            if (at != null) {
                Method getSysCtx = atClass.getDeclaredMethod("getSystemContext");
                Object sysCtx = getSysCtx.invoke(at);
                if (sysCtx instanceof Context) {
                    return (Context) sysCtx;
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private void initSystemContext(Context sysCtx) {
        if (sysCtx == null || !mSystemContextInitialized.compareAndSet(false, true)) {
            return;
        }
        try {
            android.provider.Settings.Global.putInt(sysCtx.getContentResolver(), "xsmscode_active_api", 102);
            android.provider.Settings.Global.putInt(sysCtx.getContentResolver(), "xsmscode_active_version", BuildConfig.MODULE_VERSION);
            android.provider.Settings.Global.putLong(sysCtx.getContentResolver(), "xsmscode_active_time", System.currentTimeMillis());
            log(Log.INFO, TAG, "System server wrote activation to Settings.Global successfully");
        } catch (Throwable t) {
            log(Log.WARN, TAG, "System server failed to write Settings.Global: " + t.getMessage());
        }

        try {
            BroadcastReceiver rx = new BroadcastReceiver() {
                @Override
                public void onReceive(Context ctx, Intent intent) {
                    try {
                        Context targetCtx = ctx != null ? ctx : sysCtx;
                        android.provider.Settings.Global.putInt(targetCtx.getContentResolver(), "xsmscode_active_api", 102);
                        android.provider.Settings.Global.putInt(targetCtx.getContentResolver(), "xsmscode_active_version", BuildConfig.MODULE_VERSION);
                        android.provider.Settings.Global.putLong(targetCtx.getContentResolver(), "xsmscode_active_time", System.currentTimeMillis());

                        Intent pong = new Intent("com.github.rove24.xposed.smscode.ACTION_PONG_ACTIVATION");
                        pong.putExtra("apiVersion", 102);
                        pong.putExtra("moduleVersion", BuildConfig.MODULE_VERSION);
                        pong.setPackage(SMSCODE_PACKAGE);
                        targetCtx.sendBroadcast(pong);
                        log(Log.INFO, TAG, "System server responded to activation ping");
                    } catch (Throwable ignored) {}
                }
            };
            IntentFilter f = new IntentFilter("com.github.rove24.xposed.smscode.ACTION_PING_ACTIVATION");
            f.addAction(Intent.ACTION_USER_UNLOCKED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                sysCtx.registerReceiver(rx, f, Context.RECEIVER_EXPORTED);
            } else {
                sysCtx.registerReceiver(rx, f);
            }
            log(Log.INFO, TAG, "Registered ACTION_PING_ACTIVATION receiver in system_server");
        } catch (Throwable t) {
            log(Log.WARN, TAG, "Failed to register ping receiver in system_server: " + t.getMessage());
        }

        try {
            Intent pong = new Intent("com.github.rove24.xposed.smscode.ACTION_PONG_ACTIVATION");
            pong.putExtra("apiVersion", 102);
            pong.putExtra("moduleVersion", BuildConfig.MODULE_VERSION);
            pong.setPackage(SMSCODE_PACKAGE);
            sysCtx.sendBroadcast(pong);
        } catch (Throwable ignored) {}
    }
}