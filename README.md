# ScrollShot

## Description
You can use this library to take full screen scrollshots of android app.
Currently it supports scroll shots where activity content is inside a ScrollView/ListView/RecylerView.

This library also supports ZoomPanLayout from [tileView](https://github.com/moagrius/TileView) library.
For other types of view, it will still try to take a scroll shot,
but failing to do so will result in a normal screenshot.


## Installation

```
To add this library to the android application project,
Go to build.gradle add following dependency,
```

   _**`compile 'com.github.tushardhole:scrollshot:1.0.0'`**_

```
To use this library, add following line in onCreate() of your specific Application Class.
```

   **_`ScrollShot.init(this);`_**

## Usage

To take a scrollshot fire following command,


    adb shell am broadcast -a com.mobile.android.scrollshot --es name "myScrollShot"


Where "name" is the value with which scrollshot file will be saved.
Use some file explorer app and open 'scrollshots' folder to view the scrollshot.


Instead of command, You can also shake the phone while the your APP is running to take scrollshot.

The command approach is implemented to support scrollshot in automation scripts.

## Reporting Bugs
If you find any bugs please try to replicate that in [ScrollShotTester](https://github.com/tushardhole/ScrollShotTester) by creating a new view in that Application for which scrollshot breaks.


## Known things
```
1. This library is not tested on many physical devices
2. This library is supports only vertical scroll
3. This library is not tested much against Webviews
4. Crashes for OOM with long webviews such as android os wiki page
```
## Contributing
```
BAU PR way is the way to contribute!
```
