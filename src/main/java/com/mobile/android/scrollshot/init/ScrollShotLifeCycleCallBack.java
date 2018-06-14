package com.mobile.android.scrollshot.init;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;

import com.mobile.android.scrollshot.ScrollShotReceiver;
import com.mobile.android.scrollshot.shake.ScrollShotShakeListener;
import com.mobile.android.scrollshot.shake.ShakeDetector;

import java.lang.ref.WeakReference;

public class ScrollShotLifeCycleCallBack implements Application.ActivityLifecycleCallbacks {

    private SensorManager sensorManager;
    private ShakeDetector shakeDetector;

    public ScrollShotLifeCycleCallBack(Application application) {
        sensorManager = (SensorManager) application.getSystemService(Context.SENSOR_SERVICE);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        WeakReference<Activity> activityReference = new WeakReference(activity);
        registerShake(activityReference);
        ScrollShotReceiver.setCurrentActivityReference(activityReference);
    }


    @Override
    public void onActivityPaused(Activity activity) {
        unregisterShake();
        ScrollShotReceiver.setCurrentActivityReference(null);
    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

    private void registerShake(WeakReference<Activity> activityReference) {
        shakeDetector = new ShakeDetector(new ScrollShotShakeListener(activityReference));
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(shakeDetector, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }


    private void unregisterShake() {
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.unregisterListener(shakeDetector, sensor);
    }
}
