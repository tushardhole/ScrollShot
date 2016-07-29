# ScrollShot

You can use library to take full screen scroll shots of android app. Currently it supports scroll shots where entire activiry content is inside a ScrollView or WebView.
For other types of view, it will still try to take a scroll shot, but failing to do so will result in a normal legacy screenshot.


To add this library to the android application project,
  1. Copy [library - aar] (https://github.com/tushardhole/ScrollShot/blob/master/aar/scrollshot.aar) to prebuilt-libs folder of your application
  2. Go to build.gradle add following dependency,
  
      compile(name: 'scrollshot', ext: 'aar')



To use this library in your android application, add following line in onResume() of your specific activity or parent activity of all your activ ities.

  - ScreenShotReceiver.setCurrentActivityReference(new WeakReference<Activity>(this));

To take a screenshot fire following command,
  - adb shell am broadcast -a com.mobile.android.scrollshot --es scene_name "homeScreen"

Where scene_name is the name with which scrollshot file will be saved.
Now to go to,
  - /sdcard/screenshots/ and open homeScreen.pn
  - or use some file explorer app and open 'screenshots' folder.


