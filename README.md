# ScrollShot

You can use this library to take full screen scroll shots of android app. Currently it supports scroll shots where entire activity content is inside a ScrollView/ListView/RecylerView.
This library also supports ZoomPanLayout from tileview library.

For other types of view, it will still try to take a scroll shot, but failing to do so will result in a normal screenshot.


To add this library to the android application project,
  1. Go to build.gradle add following dependency,

<code>
    compile 'com.github.tushardhole:scrollshot:1.0.0'
</code>

To use this library, add following line in onCreate() of your specific Application Class.

<code>
  ScrollShot.init(this);
</code>

To take a scrollshot fire following command,
  - adb shell am broadcast -a com.mobile.android.scrollshot --es name "myScrollShot"

Where "name" is the value with which scrollshot file will be saved.
Use some file explorer app and open 'scrollshots' folder to view the scrollshot.

You can also shake the phone while the your APP is running to take scrollshot.


