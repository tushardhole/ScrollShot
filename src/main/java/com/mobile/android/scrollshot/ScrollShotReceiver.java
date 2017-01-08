package com.mobile.android.scrollshot;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.view.ScrollingView;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.ScrollView;
import android.widget.Toast;

import com.qozix.tileview.widgets.ZoomPanLayout;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

import static android.widget.Toast.LENGTH_SHORT;
import static com.mobile.android.scrollshot.FalconExtension.takeDilaog;

public class ScrollShotReceiver extends BroadcastReceiver {

  public static final int BIG_ENOUGH_HEIGHT = 12000;
  private final String SCREENSHOT_RECEIVER_ACTION = "com.mobile.android.scrollshot";
  private final String SCREENSHOT_PATH = Environment.getExternalStorageDirectory().toString()
      + "/screenshots/";
  private final String NAME_BUNDLE_KEY = "name";

  private static WeakReference<Activity> currentActivityReference;
  private String sceneName = System.currentTimeMillis() + "_screenshot";
  private Drawable background;
  private ViewGroup rootGroup = null;
  private int offset = 0;
  private int originalWidth = 0;
  private int originalHeight = 0;

  public ScrollShotReceiver() {
  }

  @SuppressWarnings("unused")
  public synchronized static void setCurrentActivityReference(WeakReference<Activity> activity) {
    currentActivityReference = activity;
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    if (!TextUtils.isEmpty(intent.getStringExtra(NAME_BUNDLE_KEY))) {
      sceneName = intent.getStringExtra(NAME_BUNDLE_KEY);
    }
    Activity activity = currentActivityReference.get();
    if (intent.getAction().equals(SCREENSHOT_RECEIVER_ACTION) && activity != null) {
      rootGroup = (ViewGroup) activity.findViewById(android.R.id.content);
      try {
        originalHeight = rootGroup.getHeight();
        originalWidth = rootGroup.getWidth();
        findOffset(rootGroup);
        takeScrollShot(rootGroup.getChildAt(0));
      } catch (Exception e) {
        try {
          fallBackToNormalScreenShot(rootGroup.getChildAt(0));
        } catch (Exception e1) {
          Toast.makeText(currentActivityReference.get(), "Failed to take scroll shot", LENGTH_SHORT);
        }
      }
    }
  }

  private void fallBackToNormalScreenShot(View rootView) throws IOException, InterruptedException {
    takeScreenShot(false);
  }

  private void takeScrollShot(View view) throws IOException, InterruptedException {
    takeScreenShot(true);
  }

  private void takeScreenShot(boolean isScrollShot) throws IOException, InterruptedException {
    Bitmap viewScene;
    if (isScrollShot) {
      viewScene = takeScrollShot();
    } else {
      viewScene = takeNormalScreenShot();
    }
    writeSceneDataToFile(viewScene);
    resetViewLayout();
  }

  @NonNull
  private Bitmap takeScrollShot() throws InterruptedException {
    measureHeightWithScrollOffset();
    int measuredHeight = rootGroup.getChildAt(0).getMeasuredHeight();
    Bitmap viewScene = Bitmap.createBitmap(originalWidth, measuredHeight, Bitmap.Config.ARGB_8888);
    Canvas sceneCanvas = createCanvasWithWindowBG(viewScene);
    rootGroup.getChildAt(0).layout(0, 0, originalWidth, measuredHeight);
    rootGroup.getChildAt(0).draw(sceneCanvas);
    takeDilaog(sceneCanvas, currentActivityReference.get());
    return viewScene;
  }

  private Bitmap takeNormalScreenShot() throws InterruptedException {
    Bitmap viewScene = Bitmap.createBitmap(originalWidth, originalHeight, Bitmap.Config.ARGB_8888);
    Canvas sceneCanvas = createCanvasWithWindowBG(viewScene);
    rootGroup.getChildAt(0).draw(sceneCanvas);
    takeDilaog(sceneCanvas, currentActivityReference.get());
    return viewScene;
  }

  private Canvas createCanvasWithWindowBG(@NonNull Bitmap viewScene) {
    Canvas sceneCanvas = new Canvas(viewScene);
    currentActivityReference.get().getWindow()
        .getDecorView().getBackground().draw(sceneCanvas);
    return sceneCanvas;
  }

  private void measureHeightWithScrollOffset() {
    int baseHeightOfContainer = originalHeight + offset;
    int widSpec = View.MeasureSpec.makeMeasureSpec(originalWidth, View.MeasureSpec.EXACTLY);
    int heightSpec = View.MeasureSpec.makeMeasureSpec(baseHeightOfContainer, View.MeasureSpec.EXACTLY);
    rootGroup.getChildAt(0).measure(widSpec, heightSpec);
  }

  private void writeSceneDataToFile(Bitmap viewScene) throws IOException {
    File imageFile = new File(SCREENSHOT_PATH);
    imageFile.mkdirs();
    imageFile = new File(imageFile + "/" + sceneName + ".png");
    FileOutputStream fos = new FileOutputStream(imageFile);

    try {
      ByteArrayOutputStream screenShotOutputStream = new ByteArrayOutputStream();
      viewScene.compress(Bitmap.CompressFormat.PNG, 90, screenShotOutputStream);
      byte[] sceneData = screenShotOutputStream.toByteArray();

      fos.write(sceneData);
      fos.flush();
      fos.close();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      fos.close();
    }
  }

  private void resetViewLayout() {
    rootGroup.requestLayout();
  }

  private void findOffset(ViewGroup root) {
    for (int index = 0; index < root.getChildCount(); index++) {
      View child = root.getChildAt(index);
      if (child instanceof ScrollView || child instanceof ScrollingView || child instanceof AbsListView || child instanceof WebView) {
        int widSpec = View.MeasureSpec.makeMeasureSpec(originalWidth, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(BIG_ENOUGH_HEIGHT, View.MeasureSpec.UNSPECIFIED);
        child.measure(widSpec, heightSpec);
        updateOffset(child.getHeight(), child.getMeasuredHeight());
      } else if (child instanceof ZoomPanLayout) {
        updateOffset(child.getHeight(), ((ZoomPanLayout) child).getScaledHeight());
      } else if (child instanceof ViewGroup) {
        findOffset((ViewGroup) child);
      }
    }
  }

  private void updateOffset(int oldHeight, int newHeight) {
    offset = newHeight > oldHeight ?
        offset + (newHeight - oldHeight) : offset;
  }
}
