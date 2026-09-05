package com.tianma.xsmscode.xp.hook.code.helper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.input.InputManager;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import com.tianma.xsmscode.common.utils.XLog;

import de.robv.android.xposed.XposedHelpers;

/**
 * Helper for InputMethod Input Characters.<br/>
 * Refer: com.android.commands.input.Input
 */
public class InputHelper {

    private InputHelper() {
    }

    public static void sendText(String text) throws Throwable {
        sendText(null, text);
    }

    /**
     * Send text to current focused input field.
     */
    public static void sendText(Context context, String text) throws Throwable {
        if (text == null || text.isEmpty()) return;

        int source = InputDevice.SOURCE_KEYBOARD;
        StringBuilder sb = new StringBuilder(text);

        boolean escapeFlag = false;
        for (int i = 0; i < sb.length(); i++) {
            if (escapeFlag) {
                escapeFlag = false;
                if (sb.charAt(i) == 's') {
                    sb.setCharAt(i, ' ');
                    sb.deleteCharAt(--i);
                }
            }
            if (sb.charAt(i) == '%') {
                escapeFlag = true;
            }
        }

        char[] chars = sb.toString().toCharArray();
        KeyCharacterMap kcm = null;
        try {
            kcm = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
        } catch (Throwable ignored) {}

        KeyEvent[] events = (kcm != null) ? kcm.getEvents(chars) : null;
        if (events != null && events.length > 0) {
            long currentDownTime = SystemClock.uptimeMillis();
            for (KeyEvent keyEvent : events) {
                if (source != keyEvent.getSource()) {
                    keyEvent.setSource(source);
                }
                long now = SystemClock.uptimeMillis();
                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    currentDownTime = now;
                }
                KeyEvent timedEvent = KeyEvent.changeTimeRepeat(keyEvent, now, 0);
                try {
                    XposedHelpers.setLongField(timedEvent, "mDownTime", currentDownTime);
                } catch (Throwable ignored) {}
                injectKeyEvent(context, timedEvent);
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ignored) {}
            }
        } else {
            // Fallback: manually generate KeyEvents for common verification code characters (digits and letters)
            for (char c : chars) {
                int keyCode = getKeyCodeForChar(c);
                if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                    long downTime = SystemClock.uptimeMillis();
                    KeyEvent down = new KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, keyCode, 0, 0,
                            KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 8, source); // 8 = FLAG_FROM_SYSTEM
                    injectKeyEvent(context, down);
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException ignored) {}
                    long upTime = SystemClock.uptimeMillis();
                    KeyEvent up = new KeyEvent(downTime, upTime, KeyEvent.ACTION_UP, keyCode, 0, 0,
                            KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 8, source);
                    injectKeyEvent(context, up);
                    try {
                        Thread.sleep(25);
                    } catch (InterruptedException ignored) {}
                }
            }
        }
    }

    private static int getKeyCodeForChar(char c) {
        if (c >= '0' && c <= '9') {
            return KeyEvent.KEYCODE_0 + (c - '0');
        } else if (c >= 'a' && c <= 'z') {
            return KeyEvent.KEYCODE_A + (c - 'a');
        } else if (c >= 'A' && c <= 'Z') {
            return KeyEvent.KEYCODE_A + (c - 'A');
        }
        return KeyEvent.KEYCODE_UNKNOWN;
    }

    public static void sendKeyEvent(int inputSource, int keyCode, boolean longpress) throws Throwable {
        sendKeyEvent(null, inputSource, keyCode, longpress);
    }

    public static void sendKeyEvent(Context context, int inputSource, int keyCode, boolean longpress) throws Throwable {
        long now = SystemClock.uptimeMillis();
        injectKeyEvent(context, new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, 0,
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 8, inputSource));
        if (longpress) {
            injectKeyEvent(context, new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 1, 0,
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0, KeyEvent.FLAG_LONG_PRESS | 8,
                inputSource));
        }
        injectKeyEvent(context, new KeyEvent(now, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, keyCode, 0, 0,
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 8, inputSource));
    }

    @SuppressLint("PrivateApi")
    private static void injectKeyEvent(Context context, KeyEvent keyEvent) throws Throwable {
        // Tag with FLAG_FROM_SYSTEM
        try {
            XposedHelpers.setIntField(keyEvent, "mFlags", keyEvent.getFlags() | 8);
        } catch (Throwable ignored) {}

        // Try WAIT_FOR_FINISH (2) first, then ASYNC (0)
        int[] modes = new int[]{2, 0};

        java.util.List<Object> candidates = new java.util.ArrayList<>();

        // 1. Context InputManager
        if (context != null) {
            try {
                Object im = context.getSystemService(Context.INPUT_SERVICE);
                if (im != null) candidates.add(im);
            } catch (Throwable ignored) {}
        }

        // 2. InputManagerGlobal (Android 14/15/16/17)
        try {
            Class<?> imgClass = Class.forName("android.hardware.input.InputManagerGlobal");
            Object img = XposedHelpers.callStaticMethod(imgClass, "getInstance");
            if (img != null) candidates.add(img);
        } catch (Throwable ignored) {}

        // 3. Static InputManager.getInstance()
        try {
            Object staticIm = XposedHelpers.callStaticMethod(InputManager.class, "getInstance");
            if (staticIm != null) candidates.add(staticIm);
        } catch (Throwable ignored) {}

        // 4. IInputManager via ServiceManager binder directly
        try {
            Class<?> smClass = Class.forName("android.os.ServiceManager");
            android.os.IBinder binder = (android.os.IBinder) XposedHelpers.callStaticMethod(smClass, "getService", "input");
            if (binder != null) {
                Class<?> stubClass = Class.forName("android.hardware.input.IInputManager$Stub");
                Object iInputManager = XposedHelpers.callStaticMethod(stubClass, "asInterface", binder);
                if (iInputManager != null) candidates.add(iInputManager);
            }
        } catch (Throwable ignored) {}

        // Try candidate objects across modes
        Throwable lastThrowable = null;
        for (int mode : modes) {
            for (Object target : candidates) {
                if (target == null) continue;
                Class<?> clazz = target.getClass();
                java.lang.reflect.Method[] methods = clazz.getMethods();
                for (java.lang.reflect.Method m : methods) {
                    String name = m.getName();
                    if (name.startsWith("injectInput")) {
                        m.setAccessible(true);
                        Class<?>[] pTypes = m.getParameterTypes();
                        try {
                            if (pTypes.length == 2 && android.view.InputEvent.class.isAssignableFrom(pTypes[0]) && pTypes[1] == int.class) {
                                Object res = m.invoke(target, keyEvent, mode);
                                if (res instanceof Boolean && (Boolean) res) return;
                                if (res == null) return;
                            } else if (pTypes.length == 3 && android.view.InputEvent.class.isAssignableFrom(pTypes[0])
                                    && pTypes[1] == int.class && pTypes[2] == int.class) {
                                // Android 15/16/17: injectInputEvent(event, mode, targetUid)
                                Object res = m.invoke(target, keyEvent, mode, -1);
                                if (res instanceof Boolean && (Boolean) res) return;
                                if (res == null) return;
                            } else if (pTypes.length == 4 && android.view.InputEvent.class.isAssignableFrom(pTypes[0])) {
                                Object res = m.invoke(target, keyEvent, mode, -1, 0);
                                if (res instanceof Boolean && (Boolean) res) return;
                                if (res == null) return;
                            }
                        } catch (Throwable t) {
                            lastThrowable = t;
                        }
                    }
                }
            }
        }

        // 5. Fallback via IWindowManager
        try {
            Class<?> smClass = Class.forName("android.os.ServiceManager");
            android.os.IBinder wmBinder = (android.os.IBinder) XposedHelpers.callStaticMethod(smClass, "getService", "window");
            if (wmBinder != null) {
                Class<?> wmStubClass = Class.forName("android.view.IWindowManager$Stub");
                Object iwm = XposedHelpers.callStaticMethod(wmStubClass, "asInterface", wmBinder);
                if (iwm != null) {
                    for (java.lang.reflect.Method m : iwm.getClass().getMethods()) {
                        if ("injectKeyEvent".equals(m.getName())) {
                            m.setAccessible(true);
                            Class<?>[] pTypes = m.getParameterTypes();
                            if (pTypes.length == 2 && pTypes[0] == KeyEvent.class && pTypes[1] == boolean.class) {
                                iwm.getClass().getMethod("injectKeyEvent", KeyEvent.class, boolean.class).invoke(iwm, keyEvent, false);
                                return;
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        if (lastThrowable != null) {
            throw lastThrowable;
        }
    }
}
