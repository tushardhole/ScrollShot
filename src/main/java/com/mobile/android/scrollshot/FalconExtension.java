package com.mobile.android.scrollshot;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * This code is inspired/Copied from below open source project.
 * https://github.com/jraska/Falcon
 */
public class FalconExtension {


  public static final String TAG = FalconExtension.class.getSimpleName();
  private static Canvas scene = null;

  public static void takeDilaog(Canvas scene, Activity activity) throws InterruptedException {
    FalconExtension.scene = scene;
    takeBitmapUnchecked(activity);
  }

  @SuppressWarnings("unchecked") // no way to check
  private static List<ViewRootData> getRootViews(Activity activity) {
    List<ViewRootData> rootViews = new ArrayList<>();

    Object globalWindowManager;
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
      globalWindowManager = getFieldValue("mWindowManager", activity.getWindowManager());
    } else {
      globalWindowManager = getFieldValue("mGlobal", activity.getWindowManager());
    }
    Object rootObjects = getFieldValue("mRoots", globalWindowManager);
    Object paramsObject = getFieldValue("mParams", globalWindowManager);

    Object[] roots;
    WindowManager.LayoutParams[] params;

    //  There was a change to ArrayList implementation in 4.4
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      roots = ((List) rootObjects).toArray();

      List<WindowManager.LayoutParams> paramsList = (List<WindowManager.LayoutParams>) paramsObject;
      params = paramsList.toArray(new WindowManager.LayoutParams[paramsList.size()]);
    } else {
      roots = (Object[]) rootObjects;
      params = (WindowManager.LayoutParams[]) paramsObject;
    }

    for (int i = 0; i < roots.length; i++) {
      Object root = roots[i];

      View view = (View) getFieldValue("mView", root);

      Object attachInfo = getFieldValue("mAttachInfo", root);
      int top = (int) getFieldValue("mWindowTop", attachInfo);
      int left = (int) getFieldValue("mWindowLeft", attachInfo);

      Rect winFrame = (Rect) getFieldValue("mWinFrame", root);
      Rect area = new Rect(left, top, left + winFrame.width(), top + winFrame.height());

      rootViews.add(new ViewRootData(view, area, params[i]));
    }

    if (rootViews.isEmpty()) {
      return Collections.emptyList();
    }
    ensureDialogsAreAfterItsParentActivities(rootViews);

    return rootViews;
  }

  private static void ensureDialogsAreAfterItsParentActivities(List<ViewRootData> viewRoots) {
    if (viewRoots.size() <= 1) {
      return;
    }

    for (int dialogIndex = 0; dialogIndex < viewRoots.size() - 1; dialogIndex++) {
      ViewRootData viewRoot = viewRoots.get(dialogIndex);
      if (!viewRoot.isDialogType()) {
        continue;
      }

      Activity dialogOwnerActivity = ownerActivity(viewRoot.context());
      if (dialogOwnerActivity == null) {
        // make sure we will never compare null == null
        return;
      }

      for (int parentIndex = dialogIndex + 1; parentIndex < viewRoots.size(); parentIndex++) {
        ViewRootData possibleParent = viewRoots.get(parentIndex);
        if (possibleParent.isActivityType()
            && ownerActivity(possibleParent.context()) == dialogOwnerActivity) {
          viewRoots.remove(possibleParent);
          viewRoots.add(dialogIndex, possibleParent);

          break;
        }
      }
    }
  }


  // This fixes issue #11. It is not perfect solution and maybe there is another case
  // of different type of view, but it works for most common case of dialogs.
  private static Activity ownerActivity(Context context) {
    Context currentContext = context;

    while (currentContext != null) {
      if (currentContext instanceof Activity) {
        return (Activity) currentContext;
      }

      if (currentContext instanceof ContextWrapper && !(currentContext instanceof Application)) {
        currentContext = ((ContextWrapper) currentContext).getBaseContext();
      } else {
        break;
      }
    }

    return null;
  }

  private static void offsetRootsTopLeft(List<ViewRootData> rootViews) {
    int minTop = Integer.MAX_VALUE;
    int minLeft = Integer.MAX_VALUE;
    for (ViewRootData rootView : rootViews) {
      if (rootView._winFrame.top < minTop) {
        minTop = rootView._winFrame.top;
      }

      if (rootView._winFrame.left < minLeft) {
        minLeft = rootView._winFrame.left;
      }
    }

    for (ViewRootData rootView : rootViews) {
      rootView._winFrame.offset(-minLeft, -minTop);
    }
  }

  private static Object getFieldValue(String fieldName, Object target) {
    try {
      return getFieldValueUnchecked(fieldName, target);
    } catch (Exception e) {
      //throw new Exception("Unable to take Dialog");
    }
    return null;
  }


  private static Field findField(String name, Class clazz) throws NoSuchFieldException {
    Class currentClass = clazz;
    while (currentClass != Object.class) {
      for (Field field : currentClass.getDeclaredFields()) {
        if (name.equals(field.getName())) {
          return field;
        }
      }

      currentClass = currentClass.getSuperclass();
    }

    throw new NoSuchFieldException("Field " + name + " not found for class " + clazz);
  }

  private static Object getFieldValueUnchecked(String fieldName, Object target)
      throws NoSuchFieldException, IllegalAccessException {
    Field field = findField(fieldName, target.getClass());

    field.setAccessible(true);
    return field.get(target);
  }

  private static class ViewRootData {
    private final View _view;
    private final Rect _winFrame;
    private final WindowManager.LayoutParams _layoutParams;

    ViewRootData(View view, Rect winFrame, WindowManager.LayoutParams layoutParams) {
      _view = view;
      _winFrame = winFrame;
      _layoutParams = layoutParams;
    }

    boolean isDialogType() {
      return _layoutParams.type == WindowManager.LayoutParams.TYPE_APPLICATION;
    }

    boolean isActivityType() {
      return _layoutParams.type == WindowManager.LayoutParams.TYPE_BASE_APPLICATION;
    }

    Context context() {
      return _view.getContext();
    }
  }

  private static void takeBitmapUnchecked(Activity activity) throws InterruptedException {
    final List<ViewRootData> viewRoots = getRootViews(activity);
    if (viewRoots.isEmpty()) {
      //throw new UnableToTakeScreenshotException("Unable to capture any view data in " + activity);
    }

    int maxWidth = Integer.MIN_VALUE;
    int maxHeight = Integer.MIN_VALUE;

    for (ViewRootData viewRoot : viewRoots) {
      if (viewRoot._winFrame.right > maxWidth) {
        maxWidth = viewRoot._winFrame.right;
      }

      if (viewRoot._winFrame.bottom > maxHeight) {
        maxHeight = viewRoot._winFrame.bottom;
      }
    }

    // We need to do it in main thread
    if (Looper.myLooper() == Looper.getMainLooper()) {
      drawRootsToBitmap(viewRoots);
    } else {
      final CountDownLatch latch = new CountDownLatch(1);
      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          try {
            drawRootsToBitmap(viewRoots);
          } finally {
            latch.countDown();
          }
        }
      });

      latch.await();
    }
  }

  private static void drawRootsToBitmap(List<ViewRootData> viewRoots) {
    for (ViewRootData rootData : viewRoots) {
      if (rootData.isDialogType()) {
        drawRootToBitmap(rootData);
      }
    }
  }

  private static void drawRootToBitmap(ViewRootData config) {
    int restoreCount = scene.save();
    int[] location = new int[2];
    Rect r = new Rect();
    config._view.getDrawingRect(r);
    config._view.getLocationOnScreen(location);
    scene.clipRect(r);
    config._view.draw(scene);
    scene.restoreToCount(restoreCount);
  }
}
