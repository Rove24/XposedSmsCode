package com.tianma.xsmscode.ui.home;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

import androidx.appcompat.app.AppCompatDelegate;
import com.github.rove24.xposed.smscode.BuildConfig;
import com.github.rove24.xposed.smscode.R;
import com.tianma.xsmscode.common.constant.PrefConst;
import com.tianma.xsmscode.common.preference.ResetEditPreference;
import com.tianma.xsmscode.common.preference.ResetEditPreferenceDialogFragCompat;
import com.tianma.xsmscode.common.utils.ClipboardUtils;
import com.tianma.xsmscode.common.utils.ModuleUtils;
import com.tianma.xsmscode.common.utils.PackageUtils;
import com.tianma.xsmscode.common.utils.SPUtils;
import com.tianma.xsmscode.common.utils.SnackbarHelper;
import com.tianma.xsmscode.common.utils.XLog;
import com.tianma.xsmscode.data.db.entity.ApkVersion;
import com.tianma.xsmscode.ui.app.base.BasePreferenceFragment;
import com.tianma.xsmscode.ui.block.AppBlockActivity;
import com.tianma.xsmscode.ui.record.CodeRecordActivity;
import com.tianma.xsmscode.ui.rule.CodeRulesActivity;

import java.util.Objects;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasAndroidInjector;
import dagger.android.support.AndroidSupportInjection;

/**
 * 首选项Fragment
 */
public class SettingsFragment extends BasePreferenceFragment implements
        Preference.OnPreferenceClickListener,
        Preference.OnPreferenceChangeListener,
        HasAndroidInjector,
        SettingsContract.View {

    private HomeActivity mActivity;

    @Inject
    DispatchingAndroidInjector<Object> androidInjector;

    @Inject
    SettingsContract.Presenter mPresenter;

    public SettingsFragment() {
    }

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    protected RecyclerView.Adapter onCreateAdapter(PreferenceScreen preferenceScreen) {
        return new M3ExpressivePreferenceAdapter(preferenceScreen);
    }

    @Override
    public AndroidInjector<Object> androidInjector() {
        return androidInjector;
    }

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    @NonNull
    @Override
    public <T extends Preference> T findPreference(@NonNull CharSequence key) {
        return Objects.requireNonNull(super.findPreference(key));
    }

    @Override
    protected void doOnCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings);

        // general group
        if (!ModuleUtils.isModuleEnabled(requireContext())) {
            Preference enablePref = findPreference(PrefConst.KEY_ENABLE);
            enablePref.setSummary(R.string.pref_enable_summary_alt);
        }

        findPreference(PrefConst.KEY_HIDE_LAUNCHER_ICON).setOnPreferenceChangeListener(this);
        findPreference(PrefConst.KEY_CHOOSE_THEME).setOnPreferenceClickListener(this);
        // general group end

        // SMS code group
        EditTextPreference autoInputDelayPref = findPreference(PrefConst.KEY_AUTO_INPUT_CODE_DELAY);
        if (autoInputDelayPref != null) {
            autoInputDelayPref.setOnBindEditTextListener(editText -> {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                editText.setSelection(editText.getText().length());
            });
            showAutoInputDelaySummary(autoInputDelayPref, autoInputDelayPref.getText());
            autoInputDelayPref.setOnPreferenceChangeListener(this);
        }

        androidx.preference.ListPreference retentionPref = findPreference(PrefConst.KEY_NOTIFICATION_RETENTION_TIME);
        if (retentionPref != null) {
            retentionPref.setSummary(retentionPref.getEntry());
        }

        findPreference(PrefConst.KEY_APP_BLOCK_ENTRY).setOnPreferenceClickListener(this);
        // SMS code group end

        // experimental group
        // experimental group end

        // code rule group
        findPreference(PrefConst.KEY_CODE_RULES).setOnPreferenceClickListener(this);
        findPreference(PrefConst.KEY_SMSCODE_TEST).setOnPreferenceClickListener(this);
        // code rule group end

        // code records group
        Preference recordsEntryPref = findPreference(PrefConst.KEY_ENTRY_CODE_RECORDS);
        recordsEntryPref.setOnPreferenceClickListener(this);
        initRecordEntryPreference(recordsEntryPref);
        // code records group end

        // others group
        findPreference(PrefConst.KEY_VERBOSE_LOG_MODE).setOnPreferenceChangeListener(this);
        // others group end

        // about group
        // version info preference
        Preference versionPref = findPreference(PrefConst.KEY_VERSION);
        versionPref.setOnPreferenceClickListener(this);
        showVersionInfo(versionPref);
        findPreference(PrefConst.KEY_SOURCE_CODE).setOnPreferenceClickListener(this);
        findPreference(PrefConst.KEY_PRIVACY_POLICY).setOnPreferenceClickListener(this);
        // about group end
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setDivider(null);
        setDividerHeight(0);
        mActivity = (HomeActivity) requireActivity();

        mPresenter.handleArguments(getArguments());
    }

    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener mPrefChangeListener =
            (sharedPreferences, key) -> {
                XLog.d("Preference changed: %s", key);
                String preferencesName = getPreferenceManager().getSharedPreferencesName();
                mPresenter.setPreferenceWorldWritable(preferencesName);
                if (getContext() != null) {
                    com.tianma.xsmscode.common.utils.PrefSyncUtils.broadcastPreferences(getContext());
                }
            };

    @Override
    public void onResume() {
        super.onResume();
        android.content.SharedPreferences sp = getPreferenceManager().getSharedPreferences();
        if (sp != null) {
            sp.registerOnSharedPreferenceChangeListener(mPrefChangeListener);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        android.content.SharedPreferences sp = getPreferenceManager().getSharedPreferences();
        if (sp != null) {
            sp.unregisterOnSharedPreferenceChangeListener(mPrefChangeListener);
        }
        String preferencesName = getPreferenceManager().getSharedPreferencesName();
        mPresenter.setPreferenceWorldWritable(preferencesName);
        mPresenter.setInternalFilesWritable();
        if (getContext() != null) {
            com.tianma.xsmscode.common.utils.PrefSyncUtils.broadcastPreferences(getContext());
        }
    }

    @Override
    public void showAppAlreadyNewest() {
        SnackbarHelper.makeLong(getListView(), R.string.app_already_newest).show();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        if (PrefConst.KEY_CHOOSE_THEME.equals(key)) {
            showThemeChooserDialog();
        } else if (PrefConst.KEY_CODE_RULES.equals(key)) {
            CodeRulesActivity.startToMe(mActivity);
        } else if (PrefConst.KEY_SMSCODE_TEST.equals(key)) {
            showSmsCodeTestDialog();
        } else if (PrefConst.KEY_SOURCE_CODE.equals(key)) {
            mPresenter.showSourceProject();
        } else if (PrefConst.KEY_ENTRY_CODE_RECORDS.equals(key)) {
            CodeRecordActivity.startToMe(mActivity);
        } else if (PrefConst.KEY_APP_BLOCK_ENTRY.equals(key)) {
            AppBlockActivity.startMe(mActivity);
        } else if (PrefConst.KEY_VERSION.equals(key)) {
            mPresenter.checkUpdate();
        } else if(PrefConst.KEY_PRIVACY_POLICY.equals(key)) {
            showPrivacyPolicy();
        } else {
            return false;
        }
        return true;
    }

    private void showVersionInfo(Preference preference) {
        String summary = getString(R.string.pref_version_summary, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
        preference.setSummary(summary);
    }

    private void showThemeChooserDialog() {
        if (mActivity == null || mActivity.isFinishing()) {
            return;
        }
        android.app.Dialog dialog = new android.app.Dialog(mActivity);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        View dialogView = android.view.LayoutInflater.from(mActivity).inflate(R.layout.dialog_m3_theme_chooser, null);
        dialog.setContentView(dialogView);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.92),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        View layoutSystem = dialogView.findViewById(R.id.layout_theme_system);
        View layoutLight = dialogView.findViewById(R.id.layout_theme_light);
        View layoutDark = dialogView.findViewById(R.id.layout_theme_dark);

        androidx.appcompat.widget.AppCompatRadioButton rbSystem = dialogView.findViewById(R.id.rb_theme_system);
        androidx.appcompat.widget.AppCompatRadioButton rbLight = dialogView.findViewById(R.id.rb_theme_light);
        androidx.appcompat.widget.AppCompatRadioButton rbDark = dialogView.findViewById(R.id.rb_theme_dark);

        int currentMode = AppCompatDelegate.getDefaultNightMode();
        final int[] selectedMode = new int[]{currentMode};

        Runnable updateSelection = () -> {
            rbSystem.setChecked(selectedMode[0] == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM || selectedMode[0] == -100);
            rbLight.setChecked(selectedMode[0] == AppCompatDelegate.MODE_NIGHT_NO);
            rbDark.setChecked(selectedMode[0] == AppCompatDelegate.MODE_NIGHT_YES);
        };
        updateSelection.run();

        layoutSystem.setOnClickListener(v -> {
            selectedMode[0] = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            updateSelection.run();
        });
        layoutLight.setOnClickListener(v -> {
            selectedMode[0] = AppCompatDelegate.MODE_NIGHT_NO;
            updateSelection.run();
        });
        layoutDark.setOnClickListener(v -> {
            selectedMode[0] = AppCompatDelegate.MODE_NIGHT_YES;
            updateSelection.run();
        });

        android.widget.TextView btnCancel = dialogView.findViewById(R.id.btn_theme_cancel);
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        android.widget.TextView btnConfirm = dialogView.findViewById(R.id.btn_theme_confirm);
        btnConfirm.setOnClickListener(v -> {
            AppCompatDelegate.setDefaultNightMode(selectedMode[0]);
            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (PrefConst.KEY_HIDE_LAUNCHER_ICON.equals(key)) {
            mPresenter.hideOrShowLauncherIcon((Boolean) newValue);
        } else if (PrefConst.KEY_VERBOSE_LOG_MODE.equals(key)) {
            onVerboseLogModeSwitched((Boolean) newValue);
        } else if(PrefConst.KEY_AUTO_INPUT_CODE_DELAY.equals(key)) {
            return onAutoInputDelayPrefChanged(preference, newValue);
        } else {
            return false;
        }
        return true;
    }

    private void onVerboseLogModeSwitched(boolean on) {
        XLog.setLogLevel(on ? Log.VERBOSE : BuildConfig.LOG_LEVEL);
    }

    public void showSmsCodeTestDialog() {
        if (mActivity == null || mActivity.isFinishing()) {
            return;
        }
        android.app.Dialog dialog = new android.app.Dialog(mActivity);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        View dialogView = android.view.LayoutInflater.from(mActivity).inflate(R.layout.dialog_m3_sms_test, null);
        dialog.setContentView(dialogView);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.92),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        com.google.android.material.textfield.TextInputEditText etContent =
                dialogView.findViewById(R.id.et_sms_content);

        android.widget.TextView btnPaste = dialogView.findViewById(R.id.btn_paste_clipboard);
        btnPaste.setOnClickListener(v -> {
            String clip = ClipboardUtils.getTextFromClipboard(mActivity);
            if (!TextUtils.isEmpty(clip)) {
                etContent.setText(clip);
                if (etContent.getText() != null) {
                    etContent.setSelection(etContent.getText().length());
                }
            }
        });

        android.widget.TextView btnCancel = dialogView.findViewById(R.id.btn_test_cancel);
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        android.widget.TextView btnConfirm = dialogView.findViewById(R.id.btn_test_confirm);
        btnConfirm.setOnClickListener(v -> {
            CharSequence text = etContent.getText();
            mPresenter.performSmsCodeTest(text != null ? text.toString() : "");
            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        boolean handled = false;
        if (preference instanceof ResetEditPreference) {
            DialogFragment dialogFragment =
                    ResetEditPreferenceDialogFragCompat.newInstance(preference.getKey());

            FragmentManager fm = getFragmentManager();
            if (fm != null) {
                dialogFragment.setTargetFragment(this, 0);
                dialogFragment.show(fm, "android.support.v7.preference.PreferenceFragment.DIALOG");
                handled = true;
            }
        } else if (preference instanceof EditTextPreference && PrefConst.KEY_AUTO_INPUT_CODE_DELAY.equals(preference.getKey())) {
            showAutoInputDelayDialog((EditTextPreference) preference);
            handled = true;
        } else if (preference instanceof androidx.preference.ListPreference && PrefConst.KEY_NOTIFICATION_RETENTION_TIME.equals(preference.getKey())) {
            showNotificationRetentionTimeDialog((androidx.preference.ListPreference) preference);
            handled = true;
        }
        if (!handled) {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    private void initRecordEntryPreference(Preference preference) {
        String summary = getString(R.string.pref_entry_code_records_summary, PrefConst.MAX_SMS_RECORDS_COUNT_DEFAULT);
        preference.setSummary(summary);
    }

    @Override
    public void showSmsCodeTestResult(String code) {
        String text = TextUtils.isEmpty(code) ? getString(R.string.cannot_parse_smscode)
                : getString(R.string.current_sms_code, code);
        SnackbarHelper.makeLong(getListView(), text).show();
    }

    @Override
    public void showCheckError(Throwable t) {
        SnackbarHelper.makeShort(getListView(), R.string.check_update_failed).show();
    }

    @Override
    public void showUpdateDialog(ApkVersion latestVersion) {
        if (mActivity == null || mActivity.isFinishing()) {
            return;
        }
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(mActivity, R.style.ThemeOverlay_App_MaterialAlertDialog)
                .setTitle(R.string.new_version_found)
                .setMessage(latestVersion.getVersionInfo())
                .setPositiveButton(R.string.update_from_coolapk, (dialog, which) -> mPresenter.updateFromCoolApk())
                .setNegativeButton(R.string.update_from_github, (dialog, which) -> mPresenter.updateFromGithub())
                .setNeutralButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void showPrivacyPolicy() {
        if (mActivity == null || mActivity.isFinishing()) {
            return;
        }
        android.app.Dialog dialog = new android.app.Dialog(mActivity);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        View dialogView = android.view.LayoutInflater.from(mActivity).inflate(R.layout.dialog_m3_privacy_policy, null);
        dialog.setContentView(dialogView);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.92),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        android.widget.TextView tvContent = dialogView.findViewById(R.id.tv_privacy_content);
        tvContent.setText(getString(R.string.privacy_dialog_content));

        android.widget.TextView btnReject = dialogView.findViewById(R.id.btn_privacy_reject);
        btnReject.setOnClickListener(v -> {
            SPUtils.setPrivacyPolicyAccepted(mActivity, false);
            dialog.dismiss();
            mActivity.finish();
        });

        android.widget.TextView btnAccept = dialogView.findViewById(R.id.btn_privacy_accept);
        btnAccept.setOnClickListener(v -> {
            SPUtils.setPrivacyPolicyAccepted(mActivity, true);
            dialog.dismiss();
        });

        dialog.show();
    }

    private boolean onAutoInputDelayPrefChanged(Preference preference, Object newValue) {
        if (newValue instanceof String) {
            String value = (String) newValue;
            showAutoInputDelaySummary(preference, value);
            return true;
        } else {
            return false;
        }
    }

    private void showAutoInputDelaySummary(Preference preference, String value) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        String summary = context.getString(R.string.pref_auto_input_code_delay_summary, value);
        preference.setSummary(summary);
    }

    private android.app.Dialog mCurrentDelayDialog;
    private android.app.Dialog mCurrentRetentionDialog;

    private void showAutoInputDelayDialog(EditTextPreference pref) {
        if (mActivity == null || mActivity.isFinishing()) {
            return;
        }
        if (mCurrentDelayDialog != null && mCurrentDelayDialog.isShowing()) {
            return;
        }
        android.app.Dialog dialog = new android.app.Dialog(mActivity);
        mCurrentDelayDialog = dialog;
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        View dialogView = android.view.LayoutInflater.from(mActivity).inflate(R.layout.dialog_m3_auto_input_delay, null);
        dialog.setContentView(dialogView);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.92),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        com.google.android.material.textfield.TextInputEditText etValue = dialogView.findViewById(R.id.et_delay_value);
        String currentText = pref.getText();
        if (currentText == null || currentText.trim().isEmpty()) {
            currentText = "0";
        }
        etValue.setText(currentText);
        if (etValue.getText() != null) {
            etValue.setSelection(etValue.getText().length());
        }

        View chip0 = dialogView.findViewById(R.id.chip_delay_0);
        View chip05 = dialogView.findViewById(R.id.chip_delay_0_5);
        View chip1 = dialogView.findViewById(R.id.chip_delay_1);
        View chip2 = dialogView.findViewById(R.id.chip_delay_2);

        if (chip0 != null) chip0.setOnClickListener(v -> { etValue.setText("0"); if (etValue.getText() != null) etValue.setSelection(1); });
        if (chip05 != null) chip05.setOnClickListener(v -> { etValue.setText("0.5"); if (etValue.getText() != null) etValue.setSelection(3); });
        if (chip1 != null) chip1.setOnClickListener(v -> { etValue.setText("1"); if (etValue.getText() != null) etValue.setSelection(1); });
        if (chip2 != null) chip2.setOnClickListener(v -> { etValue.setText("2"); if (etValue.getText() != null) etValue.setSelection(1); });

        View btnCancel = dialogView.findViewById(R.id.btn_delay_cancel);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        View btnConfirm = dialogView.findViewById(R.id.btn_delay_confirm);
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                String val = etValue.getText() != null ? etValue.getText().toString().trim() : "0";
                if (val.isEmpty()) {
                    val = "0";
                }
                if (pref.callChangeListener(val)) {
                    pref.setText(val);
                    showAutoInputDelaySummary(pref, val);
                }
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    private void showNotificationRetentionTimeDialog(androidx.preference.ListPreference pref) {
        if (mActivity == null || mActivity.isFinishing()) {
            return;
        }
        if (mCurrentRetentionDialog != null && mCurrentRetentionDialog.isShowing()) {
            return;
        }
        android.app.Dialog dialog = new android.app.Dialog(mActivity);
        mCurrentRetentionDialog = dialog;
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        View dialogView = android.view.LayoutInflater.from(mActivity).inflate(R.layout.dialog_m3_retention_time, null);
        dialog.setContentView(dialogView);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.92),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        View row5 = dialogView.findViewById(R.id.row_retention_5);
        View row10 = dialogView.findViewById(R.id.row_retention_10);
        View row30 = dialogView.findViewById(R.id.row_retention_30);
        View row60 = dialogView.findViewById(R.id.row_retention_60);
        View row300 = dialogView.findViewById(R.id.row_retention_300);

        androidx.appcompat.widget.AppCompatRadioButton rb5 = dialogView.findViewById(R.id.rb_retention_5);
        androidx.appcompat.widget.AppCompatRadioButton rb10 = dialogView.findViewById(R.id.rb_retention_10);
        androidx.appcompat.widget.AppCompatRadioButton rb30 = dialogView.findViewById(R.id.rb_retention_30);
        androidx.appcompat.widget.AppCompatRadioButton rb60 = dialogView.findViewById(R.id.rb_retention_60);
        androidx.appcompat.widget.AppCompatRadioButton rb300 = dialogView.findViewById(R.id.rb_retention_300);

        final String[] selectedVal = new String[]{ pref.getValue() != null ? pref.getValue() : "5" };

        Runnable updateRbs = () -> {
            if (rb5 != null) rb5.setChecked("5".equals(selectedVal[0]));
            if (rb10 != null) rb10.setChecked("10".equals(selectedVal[0]));
            if (rb30 != null) rb30.setChecked("30".equals(selectedVal[0]));
            if (rb60 != null) rb60.setChecked("60".equals(selectedVal[0]));
            if (rb300 != null) rb300.setChecked("300".equals(selectedVal[0]));
        };
        updateRbs.run();

        if (row5 != null) row5.setOnClickListener(v -> { selectedVal[0] = "5"; updateRbs.run(); });
        if (row10 != null) row10.setOnClickListener(v -> { selectedVal[0] = "10"; updateRbs.run(); });
        if (row30 != null) row30.setOnClickListener(v -> { selectedVal[0] = "30"; updateRbs.run(); });
        if (row60 != null) row60.setOnClickListener(v -> { selectedVal[0] = "60"; updateRbs.run(); });
        if (row300 != null) row300.setOnClickListener(v -> { selectedVal[0] = "300"; updateRbs.run(); });

        View btnCancel = dialogView.findViewById(R.id.btn_retention_cancel);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        View btnConfirm = dialogView.findViewById(R.id.btn_retention_confirm);
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                if (pref.callChangeListener(selectedVal[0])) {
                    pref.setValue(selectedVal[0]);
                    pref.setSummary(pref.getEntry());
                }
                dialog.dismiss();
            });
        }

        dialog.show();
    }
}
