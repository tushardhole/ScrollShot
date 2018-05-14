package com.mobile.android.scrollshot.shake;

import android.app.Activity;
import android.content.Intent;

import com.mobile.android.scrollshot.ScrollShotReceiver;

import java.lang.ref.WeakReference;

public class ScrollShotShakeListener implements ShakeListener {

    private WeakReference<Activity> activityWeakReference;

    public ScrollShotShakeListener(WeakReference<Activity> activityWeakReference) {
        this.activityWeakReference = activityWeakReference;

    }


    @Override
    public void onShake() {
        synchronized (ScrollShotShakeListener.class) {
            if (activityWeakReference.get() != null) {
                Intent scrollShotIntent = new Intent();
                scrollShotIntent.setAction(ScrollShotReceiver.SCREENSHOT_RECEIVER_ACTION);
                ScrollShotReceiver scrollShotReceiver = new ScrollShotReceiver();
                scrollShotReceiver.onReceive(activityWeakReference.get(), scrollShotIntent);
            }
        }
    }
}
