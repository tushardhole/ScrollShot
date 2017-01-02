package com.mobile.android.scrollshot;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.view.ScrollingView;
import android.text.TextUtils;
import android.util.Log;
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

import static android.view.View.MeasureSpec.EXACTLY;
import static android.widget.Toast.LENGTH_SHORT;

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
      background = findTopMostValidBackground(rootGroup);

      try {
        ZoomPanLayout zoomPanLayout;
        WebView webView;
        originalHeight = rootGroup.getHeight();
        originalWidth = rootGroup.getWidth();
        findOffset(rootGroup);

        if ((zoomPanLayout = findViewByType(rootGroup, ZoomPanLayout.class)) != null) {
          takeScrollShot(zoomPanLayout);
        }
        if ((webView = findViewByType(rootGroup, WebView.class)) != null) {
          takeScrollShot(webView);
        } else {
          takeScrollShot(rootGroup.getChildAt(0));
        }
      } catch (Exception e) {
        try {
          fallBackToNormalScreenShot(rootGroup.getChildAt(0));
        } catch (Exception e1) {
          Toast.makeText(currentActivityReference.get(), "Failed to take scroll shot", LENGTH_SHORT);
        }
      }

    }
  }

  @Nullable
  private <T> T findViewByType(ViewGroup content, Class<T> viewType) throws IOException {
    if (content.getChildAt(0) != null) {
      Log.d("Scroll", content.getClass().getSimpleName() + (content.getChildAt(0).getHeight() > content.getHeight()));
    }
    if (viewType.isAssignableFrom(content.getClass())) {
      return (T) content;
    } else {
      for (int i = 0; i < content.getChildCount(); i++) {
        if (content.getChildAt(i) != null &&
            viewType.isAssignableFrom(content.getChildAt(i).getClass())) {
          return (T) content.getChildAt(i);
        } else if (content.getChildAt(i) instanceof ViewGroup) {
          T viewGroup = findViewByType((ViewGroup) content.getChildAt(i), viewType);
          if (viewGroup != null) {
            return viewGroup;
          }
        }
      }
    }
    return null;
  }

  private void fallBackToNormalScreenShot(View rootView) throws IOException, InterruptedException {
    takeScreenShot(rootView, false);
  }

  private void takeScrollShot(View view) throws IOException, InterruptedException {
    takeScreenShot(view, true);
  }

  private void takeScreenShot(final View view, boolean isScrollShot) throws IOException, InterruptedException {
    Bitmap viewScene = null;
    Canvas sceneCanvas;

    if (isScrollShot) {
      measureHeight(view);
      int measuredHeight = rootGroup.getChildAt(0).getMeasuredHeight();
      viewScene = Bitmap.createBitmap(originalWidth, measuredHeight, Bitmap.Config.ARGB_8888);
      sceneCanvas = new Canvas(viewScene);
      if (background != null) {
        background.setBounds(0, 0, originalWidth, measuredHeight);
        background.draw(sceneCanvas);
      }
      rootGroup.getChildAt(0).layout(0, 0, originalWidth, measuredHeight);
      rootGroup.getChildAt(0).draw(sceneCanvas);
    } else {
      viewScene = Bitmap.createBitmap(originalWidth, originalHeight, Bitmap.Config.ARGB_8888);
      sceneCanvas = new Canvas(viewScene);
      rootGroup.getChildAt(0).draw(sceneCanvas);
    }
    FalconExtension.takeDilaog(sceneCanvas, currentActivityReference.get());
    writeSceneDataToFile(viewScene);
    resetView(originalHeight, originalWidth);
  }

  private void measureHeight(View root) {
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

  private Drawable findTopMostValidBackground(ViewGroup rootGroup) {
    if (rootGroup.getBackground() != null) {
      return rootGroup.getBackground();
    } else {
      for (int i = 0; i < rootGroup.getChildCount(); i++) {
        if (rootGroup.getChildAt(i).getBackground() != null) {
          return rootGroup.getChildAt(i).getBackground();
        } else if (rootGroup.getChildAt(i) instanceof ViewGroup) {
          Drawable background = findTopMostValidBackground((ViewGroup) rootGroup.getChildAt(i));
          if (background != null) {
            return background;
          }
        }
      }
    }
    return null;
  }

  private void resetView(int originalHeight, int originalWidth) {
    int heightSpec = View.MeasureSpec.makeMeasureSpec(originalHeight, EXACTLY);
    int widSpec = View.MeasureSpec.makeMeasureSpec(originalWidth, EXACTLY);
    rootGroup.getChildAt(0).measure(widSpec, heightSpec);
  }

  private void findOffset(ViewGroup root) {
    for (int index = 0; index < root.getChildCount(); index++) {
      View child = root.getChildAt(index);
      if (child instanceof ScrollView || child instanceof ScrollingView || child instanceof AbsListView || child instanceof WebView) {
        int originalHeight = child.getHeight();
        int widSpec = View.MeasureSpec.makeMeasureSpec(originalWidth, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(BIG_ENOUGH_HEIGHT, View.MeasureSpec.UNSPECIFIED);
        child.measure(widSpec, heightSpec);
        if (child.getMeasuredHeight() > originalHeight) {
          offset = offset + (child.getMeasuredHeight() - originalHeight);
        }
      } else if (child instanceof ZoomPanLayout) {
        offset = offset + (((ZoomPanLayout) child).getScaledHeight() - child.getHeight());
      } else if (child instanceof ViewGroup) {
        findOffset((ViewGroup) child);
      }
    }
  }
}
