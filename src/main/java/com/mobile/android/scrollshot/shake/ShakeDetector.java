package com.mobile.android.scrollshot.shake;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class ShakeDetector implements SensorEventListener {

    private ShakeListener onShakeListener;
    private static final float SHAKE_THRESHOLD_GRAVITY = 2.7F;
    private static final int SHAKE_SLOP_TIME_MS = 500; // to take care of very close multiple shake events
    private long mShakeTimestamp;

    public ShakeDetector(ShakeListener shakeListener) {
        this.onShakeListener = shakeListener;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        float gX = x / SensorManager.GRAVITY_EARTH;
        float gY = y / SensorManager.GRAVITY_EARTH;
        float gZ = z / SensorManager.GRAVITY_EARTH;

        float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

        if (gForce > SHAKE_THRESHOLD_GRAVITY) {
            if(mShakeTimestamp + SHAKE_SLOP_TIME_MS > now()) {
                return;
            }
            mShakeTimestamp = now();
            this.onShakeListener.onShake();
        }
    }

    public long now() {
        return System.currentTimeMillis();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}