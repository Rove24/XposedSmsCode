package com.tianma.xsmscode.ui.home;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.github.rove24.xposed.smscode.R;
import com.tianma.xsmscode.common.constant.PrefConst;
import com.tianma.xsmscode.common.utils.ModuleUtils;
import com.tianma.xsmscode.ui.app.base.BaseActivity;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 主界面
 */
public class HomeActivity extends BaseActivity {
    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @BindView(R.id.tv_status_title)
    TextView mTvStatusTitle;

    @BindView(R.id.iv_status_badge)
    ImageView mIvStatusBadge;

    @BindView(R.id.fab_test_sms)
    ExtendedFloatingActionButton mFabTestSms;

    private static final String TAG_NESTED = "tag_nested";

    private Fragment mCurrentFragment;
    private FragmentManager mFragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);
        ButterKnife.bind(this);

        getExternalFilesDir("");

        shareXposedPreferences();

        handleIntent(getIntent());

        // setup toolbar
        setupToolbar();

        // register activation pong and sync receiver
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.github.rove24.xposed.smscode.ACTION_PONG_ACTIVATION");
            filter.addAction("com.github.rove24.xposed.smscode.ACTION_REQUEST_PREF_SYNC");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(mPongReceiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                registerReceiver(mPongReceiver, filter);
            }
        } catch (Throwable ignored) {}

        // check module activation status
        checkModuleActivationStatus();

        if (mFabTestSms != null) {
            mFabTestSms.setOnClickListener(v -> {
                if (mCurrentFragment instanceof SettingsFragment) {
                    ((SettingsFragment) mCurrentFragment).showSmsCodeTestDialog();
                }
            });
        }
    }

    private void setupToolbar() {
        setSupportActionBar(mToolbar);

        refreshActionBar(getString(R.string.app_name));
    }

    private void handleIntent(Intent intent) {
        SettingsFragment settingsFragment = SettingsFragment.newInstance();

        mFragmentManager = getSupportFragmentManager();
        mFragmentManager.beginTransaction()
                .replace(R.id.home_content, settingsFragment)
                .commit();
        mCurrentFragment = settingsFragment;
    }

    private void refreshActionBar(String title) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
            actionBar.setHomeButtonEnabled(true);
            if (mCurrentFragment instanceof SettingsFragment) {
                actionBar.setDisplayHomeAsUpEnabled(false);
            } else {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mFragmentManager.getBackStackEntryCount() == 0) {
            super.onBackPressed();
        } else {
            mFragmentManager.popBackStackImmediate();
            mCurrentFragment = mFragmentManager.findFragmentById(R.id.home_content);
            refreshActionBar(getString(R.string.app_name));
        }
    }

    private final BroadcastReceiver mPongReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            String action = intent.getAction();
            if ("com.github.rove24.xposed.smscode.ACTION_REQUEST_PREF_SYNC".equals(action)) {
                com.tianma.xsmscode.common.utils.PrefSyncUtils.broadcastPreferences(HomeActivity.this);
                return;
            }
            int api = intent.getIntExtra("apiVersion", 102);
            int modVer = intent.getIntExtra("moduleVersion", 13);
            try {
                getSharedPreferences("module_status", Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean("is_activated", true)
                        .putInt("api_version", api)
                        .putInt("module_version", modVer)
                        .putLong("last_heartbeat", System.currentTimeMillis())
                        .apply();
            } catch (Throwable ignored) {}
            updateActivationUI(true);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        checkModuleActivationStatus();
        com.tianma.xsmscode.common.utils.PrefSyncUtils.broadcastPreferences(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(mPongReceiver);
        } catch (Throwable ignored) {}
    }

    private void updateActivationUI(boolean active) {
        if (isFinishing()) return;
        runOnUiThread(() -> {
            if (active) {
                mTvStatusTitle.setText("API 102 · 已激活");
                mIvStatusBadge.setImageResource(R.drawable.ic_done);
                mIvStatusBadge.setBackgroundResource(R.drawable.bg_m3_badge_green);
                mIvStatusBadge.setColorFilter(ContextCompat.getColor(HomeActivity.this, R.color.m3_badge_green_fg));
            } else {
                mTvStatusTitle.setText("模块未激活");
                mIvStatusBadge.setImageResource(R.drawable.ic_help);
                mIvStatusBadge.setBackgroundResource(R.drawable.bg_m3_badge_amber);
                mIvStatusBadge.setColorFilter(ContextCompat.getColor(HomeActivity.this, R.color.m3_badge_amber_fg));
            }
        });
    }

    private void checkModuleActivationStatus() {
        boolean active = ModuleUtils.isModuleEnabled(HomeActivity.this);
        updateActivationUI(active);

        // Ping hooks in com.android.phone and system
        try {
            sendBroadcast(new Intent("com.github.rove24.xposed.smscode.ACTION_PING_ACTIVATION"));
            try {
                Intent p1 = new Intent("com.github.rove24.xposed.smscode.ACTION_PING_ACTIVATION");
                p1.setPackage("com.android.phone");
                sendBroadcast(p1);
            } catch (Throwable ignored) {}
            try {
                Intent p2 = new Intent("com.github.rove24.xposed.smscode.ACTION_PING_ACTIVATION");
                p2.setPackage("android");
                sendBroadcast(p2);
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}

        // Staggered checks for asynchronous ping-pong or IPC updates
        Handler handler = new Handler(Looper.getMainLooper());
        int[] delays = {150, 400, 800, 1500, 2500};
        for (int delay : delays) {
            handler.postDelayed(() -> {
                if (isFinishing()) return;
                if (ModuleUtils.isModuleEnabled(HomeActivity.this)) {
                    updateActivationUI(true);
                }
            }, delay);
        }
    }

    @SuppressLint("WorldReadableFiles")
    private void shareXposedPreferences() {
        try {
            // EdXposed or LSPosed new XSharedPreferences:  https://github.com/LSPosed/LSPosed/wiki/New-XSharedPreferences
            getSharedPreferences(PrefConst.PREF_NAME, Context.MODE_WORLD_READABLE);
        } catch (SecurityException exception) {
            // 如果模块没有被 EdXposed 或者 LSPosed 激活，就会走到这里来
            // ignore
        }
    }
}
