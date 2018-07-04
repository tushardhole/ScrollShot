# ScrollShot

## Description
You can use this library to take full screen scrollshots of android app.
Currently it supports scroll shots where activity content is inside a ScrollView/ListView/RecylerView.

This library also supports ZoomPanLayout from [tileView](https://github.com/moagrius/TileView) library.
For other types of view, it will still try to take a scroll shot,
but failing to do so will result in a normal screenshot.


## Installation
To add this library to the android application project,
Go to build.gradle add following dependency,

```
`compile 'com.github.tushardhole:scrollshot:1.0.0'`
```

To use this library, add following line in onCreate() of your specific Application Class.

```java
ScrollShot.init(this)
```

## Usage
Just shake the device to take the scrollshot and done 💥💥.


If the requirement is to take scrollshot in some automation script then please use below command,

```ruby
adb shell am broadcast -a com.mobile.android.scrollshot --es name "myScrollShot"
```

Where "name" is the value with which scrollshot file will be saved.
Use some file explorer app and open 'scrollshots' folder to view the scrollshot.

## Build Source Code
```ruby
./gradlew buildScrollShot
```
Latest artifact is laways copied to 'aar' folder at root level

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
Please use the pull request procedure for contributing

## License

    Copyright 2018 Tuhsar Dhole

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
