package com.tianma.xsmscode.xp.hook.permission;

import android.os.Binder;

import com.tianma.xsmscode.common.utils.XLog;
import com.tianma.xsmscode.xp.hook.BaseSubHook;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Hook com.android.server.input.InputManagerService#injectInputEvent
 * to clear calling identity, allowing com.android.phone to inject input events without INJECT_EVENTS permission.
 */
public class InputManagerServiceHook extends BaseSubHook {

    private static final String CLASS_INPUT_MANAGER_SERVICE = "com.android.server.input.InputManagerService";

    public InputManagerServiceHook(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    public void startHook() {
        try {
            Class<?> imsClass = XposedHelpers.findClassIfExists(CLASS_INPUT_MANAGER_SERVICE, mClassLoader);
            if (imsClass == null) {
                XLog.w("InputManagerService class not found");
                return;
            }

            for (Method method : imsClass.getDeclaredMethods()) {
                String name = method.getName();
                if (name.startsWith("injectInput") || name.contains("injectInputEvent")) {
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            long token = Binder.clearCallingIdentity();
                            param.setObjectExtra("token", token);
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Long token = (Long) param.getObjectExtra("token");
                            if (token != null) {
                                Binder.restoreCallingIdentity(token);
                            }
                        }
                    });
                }
            }
            XLog.i("Hooked InputManagerService#injectInputEvent successfully");
        } catch (Throwable t) {
            XLog.e("Failed to hook InputManagerService#injectInputEvent", t);
        }
    }
}
