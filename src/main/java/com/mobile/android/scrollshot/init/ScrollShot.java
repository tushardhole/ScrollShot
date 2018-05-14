package com.mobile.android.scrollshot.init;

import android.app.Application;

public class ScrollShot {

    public synchronized static void init(Application application) {
        application.registerActivityLifecycleCallbacks(new ScrollShotLifeCycleCallBack(application));
    }
}
