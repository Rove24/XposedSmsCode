package com.tianma.xsmscode.ui.app;

import com.github.rove24.xposed.smscode.R;
import com.google.android.material.color.DynamicColors;
import com.tianma.xsmscode.common.constant.PrefConst;
import com.tianma.xsmscode.data.eventbus.MyEventBusIndex;
import com.tianma.xsmscode.feature.migrate.TransitionTask;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import dagger.android.AndroidInjector;
import dagger.android.DaggerApplication;

public class SmsCodeApplication extends DaggerApplication {

    @Override
    protected AndroidInjector<? extends DaggerApplication> applicationInjector() {
        return DaggerApplicationComponent.factory().create(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this);

        // Ensure all preference defaults from settings.xml are written to the file
        // so that DBProvider.call("get_preferences") returns the complete set.
        androidx.preference.PreferenceManager.setDefaultValues(this, PrefConst.PREF_NAME,
                android.content.Context.MODE_PRIVATE, R.xml.settings, false);

        // Make preferences world-readable for XSharedPreferences fallback
        try {
            getSharedPreferences(PrefConst.PREF_NAME, android.content.Context.MODE_WORLD_READABLE);
        } catch (SecurityException ignored) {}

        installDefaultEventBus();
        performTransitionTask();
        com.tianma.xsmscode.common.utils.PrefSyncUtils.broadcastPreferences(this);
    }

    private void installDefaultEventBus() {
        EventBus.builder().addIndex(new MyEventBusIndex()).installDefaultEventBus();
    }

    // data transition task
    private void performTransitionTask() {
        Executor singlePool = Executors.newSingleThreadExecutor();
        singlePool.execute(new TransitionTask(this));
    }

}
