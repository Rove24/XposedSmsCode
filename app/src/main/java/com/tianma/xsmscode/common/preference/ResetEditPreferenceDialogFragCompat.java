package com.tianma.xsmscode.common.preference;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceDialogFragmentCompat;

import com.github.rove24.xposed.smscode.R;

public class ResetEditPreferenceDialogFragCompat extends PreferenceDialogFragmentCompat {

    private static final String SAVE_STATE_TEXT = "ResetEditPreferenceDialogFragCompat.text";
    private EditText mEditText;
    private CharSequence mText;

    public ResetEditPreferenceDialogFragCompat() {
    }

    public static ResetEditPreferenceDialogFragCompat newInstance(String key) {
        ResetEditPreferenceDialogFragCompat fragment = new ResetEditPreferenceDialogFragCompat();
        Bundle b = new Bundle(1);
        b.putString("key", key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            this.mText = this.getResetEditPreference().getText();
        } else {
            this.mText = savedInstanceState.getCharSequence(SAVE_STATE_TEXT);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(SAVE_STATE_TEXT, this.mText);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = requireContext();
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_m3_reset_edit, null);
        dialog.setContentView(dialogView);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.92),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        mEditText = dialogView.findViewById(R.id.et_keyword_content);
        if (mEditText != null) {
            mEditText.setText(mText);
            if (mEditText.getText() != null) {
                mEditText.setSelection(mEditText.getText().length());
            }
        }

        View btnReset = dialogView.findViewById(R.id.btn_keyword_reset);
        if (btnReset != null) {
            btnReset.setOnClickListener(v -> {
                ResetEditPreference pref = getResetEditPreference();
                if (pref != null && mEditText != null) {
                    mEditText.setText(pref.getDefaultValue());
                    if (mEditText.getText() != null) {
                        mEditText.setSelection(mEditText.getText().length());
                    }
                }
            });
        }

        View btnCancel = dialogView.findViewById(R.id.btn_keyword_cancel);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        View btnSave = dialogView.findViewById(R.id.btn_keyword_save);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                ResetEditPreference pref = getResetEditPreference();
                if (pref != null && mEditText != null && mEditText.getText() != null) {
                    String value = mEditText.getText().toString();
                    if (pref.callChangeListener(value)) {
                        pref.setText(value);
                    }
                }
                dialog.dismiss();
            });
        }

        return dialog;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        // Handled via custom save button
    }

    private ResetEditPreference getResetEditPreference() {
        return (ResetEditPreference) getPreference();
    }
}
