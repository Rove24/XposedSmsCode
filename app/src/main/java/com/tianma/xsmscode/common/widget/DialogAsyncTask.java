package com.tianma.xsmscode.common.widget;

import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.widget.ProgressBar;

import androidx.appcompat.app.AlertDialog;

import com.github.rove24.xposed.smscode.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public abstract class DialogAsyncTask<Param, Progress, Result> extends AsyncTask<Param, Progress, Result> implements DialogInterface.OnCancelListener {

    private boolean mCancelable;

    private AlertDialog mProgressDialog;

    public DialogAsyncTask(Context context, String progressMsg, boolean cancelable) {
        mCancelable = cancelable;
        ProgressBar progressBar = new ProgressBar(context);
        int pad = (int) (24 * context.getResources().getDisplayMetrics().density);
        progressBar.setPadding(pad, pad, pad, pad);
        mProgressDialog = new MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_App_MaterialAlertDialog)
                .setMessage(progressMsg)
                .setView(progressBar)
                .setCancelable(mCancelable)
                .create();
    }

    @Override
    protected void onPreExecute() {
        if (mCancelable) {
            mProgressDialog.setOnCancelListener(this);
        }
        mProgressDialog.show();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        cancel(true);
    }

    @Override
    protected void onPostExecute(Result result) {
        if (mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    protected void onCancelled() {
        if (mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }
}
