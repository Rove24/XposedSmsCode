package com.tianma.xsmscode.common.utils;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;

public class ClipboardUtils {

    private ClipboardUtils() {
    }

    public static void copyToClipboard(Context context, String text) {
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null) {
            XLog.e("Copy failed, clipboard manager is null");
            return;
        }
        ClipData clipData = ClipData.newPlainText("Copy text", text);
        cm.setPrimaryClip(clipData);
        XLog.i("Copy to clipboard succeed");
    }

    public static void clearClipboard(Context context) {
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null) {
            XLog.e("Clear failed, clipboard manager is null");
            return;
        }
        if(cm.hasPrimaryClip()) {
            ClipDescription cd = cm.getPrimaryClipDescription();
            if (cd != null) {
                if (cd.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                    cm.setPrimaryClip(ClipData.newPlainText("Copy text", ""));
                    XLog.i("Clear clipboard succeed");
                }
            }
        }
    }

    public static String getTextFromClipboard(Context context) {
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null || !cm.hasPrimaryClip()) {
            return null;
        }
        ClipData clip = cm.getPrimaryClip();
        if (clip != null && clip.getItemCount() > 0) {
            CharSequence text = clip.getItemAt(0).getText();
            return text != null ? text.toString() : null;
        }
        return null;
    }

}
