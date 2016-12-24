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
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ScrollView;

import com.qozix.tileview.widgets.ZoomPanLayout;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

public class ScreenShotReceiver extends BroadcastReceiver {

  private final String SCREENSHOT_RECEIVER_ACTION = "com.mobile.android.scrollshot";
  private final String SCREENSHOT_PATH = Environment.getExternalStorageDirectory().toString()
      + "/screenshots/";
  private final String NAME_BUNDLE_KEY = "name";

  private static WeakReference<Activity> currentActivityReference;
  private String sceneName = System.currentTimeMillis() + "_screenshot";
  private Drawable background;
  private ViewGroup rootGroup = null;

  public ScreenShotReceiver() {
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
        WebView webView;
        ScrollView scrollView;
        ZoomPanLayout zoomPanLayout;

        if ((scrollView = findViewByType(rootGroup, ScrollView.class)) != null) {
          takeScrollShot(scrollView);
        } else if ((webView = findViewByType(rootGroup, WebView.class)) != null) {
          takeScrollShot(webView);
        } else if ((zoomPanLayout = findViewByType(rootGroup, ZoomPanLayout.class)) != null) {
          takeScrollShot(zoomPanLayout);
        } else {
          takeScrollShot(rootGroup.getChildAt(0));
        }
      } catch (Exception e) {
        try {
          fallBackToNormalScreenShot(rootGroup.getChildAt(0));
        } catch (IOException e1) {
          e1.printStackTrace();
        } catch (InterruptedException e1) {
          e1.printStackTrace();
        }
      }

    }
  }

  @Nullable
  private <T> T findViewByType(ViewGroup content, Class<T> viewType) throws IOException {
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
    if (isScrollShot) {
      view.measure(View.MeasureSpec.makeMeasureSpec(view.getMeasuredWidth(), View.MeasureSpec.EXACTLY),
          View.MeasureSpec.UNSPECIFIED);
    }

    int height = view.getMeasuredHeight();
    int width = view.getMeasuredWidth();

    if (view instanceof ZoomPanLayout) {
      height = ((ZoomPanLayout) view).getScaledHeight();
      width = ((ZoomPanLayout) view).getScaledWidth();
    }

    Bitmap viewScene = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Canvas sceneCanvas = new Canvas(viewScene);

    if (background != null) {
      background.setBounds(0, 0, width, height);
      background.draw(sceneCanvas);
    }

    if (isScrollShot) {
      int widSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
      int heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);
      rootGroup.getChildAt(0).measure(widSpec, heightSpec);
      rootGroup.getChildAt(0).layout(0, 0, width, height);
      rootGroup.getChildAt(0).draw(sceneCanvas);
    } else {
      view.draw(sceneCanvas);
    }

    FalconExtension.takeDilaog(sceneCanvas, currentActivityReference.get());
    writeSceneDataToFile(viewScene);
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

  private void restartActivity() {
    currentActivityReference.get().finish();
    startSelf();
  }

  protected void startSelf() {
    currentActivityReference.get().
        getApplicationContext().
        startActivity(currentActivityReference.get().getIntent());
  }
}
