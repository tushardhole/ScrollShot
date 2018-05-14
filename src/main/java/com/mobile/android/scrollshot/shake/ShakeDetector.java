package com.mobile.android.scrollshot.shake;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class ShakeDetector implements SensorEventListener {

    public static final String TAG = ShakeDetector.class.getSimpleName();
    private ShakeListener shakeListener;
    private static final float SHAKE_THRESHOLD_GRAVITY = 2.7F;
    private static final int SHAKE_SLOP_TIME_MS = 500;
    private long mShakeTimestamp;

    public ShakeDetector(ShakeListener shakeListener) {
        this.shakeListener = shakeListener;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        synchronized (ShakeDetector.class) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            float gX = x / SensorManager.GRAVITY_EARTH;
            float gY = y / SensorManager.GRAVITY_EARTH;
            float gZ = z / SensorManager.GRAVITY_EARTH;

            float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

            if (gForce > SHAKE_THRESHOLD_GRAVITY) {
                final long now = System.currentTimeMillis();
                if (mShakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                    return;
                }
                mShakeTimestamp = now;
                this.shakeListener.onShake();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
