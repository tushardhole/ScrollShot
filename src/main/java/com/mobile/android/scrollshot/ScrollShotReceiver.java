package com.mobile.android.scrollshot;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
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
import java.util.HashMap;
import java.util.Map;

import static android.widget.Toast.LENGTH_SHORT;
import static com.mobile.android.scrollshot.FalconExtension.takeDilaog;

public class ScrollShotReceiver extends BroadcastReceiver {

    public static final int BIG_ENOUGH_HEIGHT = 12000;
    public static final String SCREENSHOT_RECEIVER_ACTION = "com.mobile.android.scrollshot";
    private final String SCREENSHOT_PATH = Environment.getExternalStorageDirectory().toString()
            + "/scrollshots/";
    private final String NAME_BUNDLE_KEY = "name";

    private static WeakReference<Activity> currentActivityReference;
    private String fileName;
    private View decorView = null;
    private int offset = 0;
    private Map<Integer, Integer> offsetWithY = new HashMap<>();
    private Map<View, int[]> scrollPos = new HashMap<>();
    private int originalWidth = 0;
    private int originalHeight = 0;

    public ScrollShotReceiver() {
    }

    @SuppressWarnings("unused")
    public synchronized static void setCurrentActivityReference(WeakReference<Activity> activity) {
        synchronized (ScrollShotReceiver.class) {
            currentActivityReference = activity;
        }
    }

    @Override
    public void onReceive(Context context, @NonNull Intent intent) {
        synchronized (ScrollShotReceiver.class) {
            if (isActivityPresent()) {
                setFileName(intent);
                if (intent.getAction().equals(SCREENSHOT_RECEIVER_ACTION)) {
                    decorView = getActivity().getWindow().getDecorView();
                    try {
                        originalHeight = getActivity().getWindow().getDecorView().getHeight();
                        originalWidth = getActivity().getWindow().getDecorView().getWidth();
                        findOffset((ViewGroup) getActivity().findViewById(android.R.id.content));
                        updateTotalOffset();
                        takeScreenShot(true);
                    } catch (Exception e) {
                        try {
                            fallBackToNormalScreenShot();
                        } catch (Exception e1) {
                            Toast.makeText(getActivity(), "Failed to take scroll shot", LENGTH_SHORT);
                        }
                    }
                }
            }
        }
    }

    private void setFileName(@NonNull Intent intent) {
        String intendedName = intent.getStringExtra(NAME_BUNDLE_KEY);
        fileName = !TextUtils.isEmpty(intendedName) ? intendedName : System.currentTimeMillis() + "_scrollshot";
    }

    private Activity getActivity() {
        return currentActivityReference.get();
    }

    private boolean isActivityPresent() {
        return currentActivityReference != null && getActivity() != null;
    }

    private void fallBackToNormalScreenShot() throws IOException, InterruptedException {
        takeScreenShot(false);
    }

    private void takeScreenShot(boolean isScrollShot) throws IOException, InterruptedException {
        Bitmap viewScene;
        if (isScrollShot) {
            viewScene = takeScrolledScreenShot();
        } else {
            viewScene = takeNormalScreenShot();
        }
        writeSceneDataToFile(viewScene);
        viewScene.recycle();
        resetViewLayout();
    }

    @NonNull
    private Bitmap takeScrolledScreenShot() throws InterruptedException {
        measureHeightWithScrollOffset();
        int measuredHeight = decorView.getMeasuredHeight();
        Bitmap viewScene = Bitmap.createBitmap(originalWidth, measuredHeight, Bitmap.Config.RGB_565);
        Canvas sceneCanvas = new Canvas(viewScene);
        decorView.layout(0, 0, originalWidth, measuredHeight);
        decorView.draw(sceneCanvas);
        takeDilaog(sceneCanvas, getActivity());
        return viewScene;
    }

    private Bitmap takeNormalScreenShot() throws InterruptedException {
        Bitmap viewScene = Bitmap.createBitmap(originalWidth, originalHeight, Bitmap.Config.RGB_565);
        Canvas sceneCanvas = new Canvas(viewScene);
        decorView.draw(sceneCanvas);
        takeDilaog(sceneCanvas, getActivity());
        return viewScene;
    }

    private void measureHeightWithScrollOffset() {
        int baseHeightOfContainer = originalHeight + offset;
        int widSpec = View.MeasureSpec.makeMeasureSpec(originalWidth, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(baseHeightOfContainer, View.MeasureSpec.EXACTLY);
        getActivity().getWindow().getDecorView().measure(widSpec, heightSpec);
    }

    private void writeSceneDataToFile(Bitmap viewScene) throws IOException {
        viewScene = cropStatusBar(viewScene);
        File imageFile = new File(SCREENSHOT_PATH);
        imageFile.mkdirs();
        imageFile = new File(imageFile + "/" + fileName + ".jpeg");
        FileOutputStream fos = new FileOutputStream(imageFile);

        try {
            ByteArrayOutputStream screenShotOutputStream = new ByteArrayOutputStream();
            viewScene.compress(Bitmap.CompressFormat.JPEG, 90, screenShotOutputStream);
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

    private Bitmap cropStatusBar(Bitmap decorView) {
        Rect rect = new Rect();
        getActivity().getWindow().
                getDecorView().getWindowVisibleDisplayFrame(rect);
        int statusBarHeight = rect.top;
        Bitmap crop = Bitmap.createBitmap(decorView, 0, statusBarHeight,
                decorView.getWidth(), decorView.getHeight() - statusBarHeight);
        decorView.recycle();
        return crop;
    }

    private void resetViewLayout() {
        decorView.requestLayout();
    }

    private void findOffset(ViewGroup root) {
        for (int index = 0; index < root.getChildCount(); index++) {
            View child = root.getChildAt(index);
            if (child instanceof ScrollView || child instanceof ScrollingView || child instanceof AbsListView || child instanceof WebView) {
                int widSpec = View.MeasureSpec.makeMeasureSpec(originalWidth, View.MeasureSpec.EXACTLY);
                int heightSpec = View.MeasureSpec.makeMeasureSpec(BIG_ENOUGH_HEIGHT, View.MeasureSpec.AT_MOST);
                child.measure(widSpec, heightSpec);

                if (child.getMeasuredHeight() == BIG_ENOUGH_HEIGHT) {
                    heightSpec = View.MeasureSpec.makeMeasureSpec(BIG_ENOUGH_HEIGHT, View.MeasureSpec.UNSPECIFIED);
                    child.measure(widSpec, heightSpec);
                }
                updateOffset(child.getHeight(), child.getMeasuredHeight(), child);
            } else if (child instanceof ZoomPanLayout) {
                updateOffset(child.getHeight(), ((ZoomPanLayout) child).getScaledHeight(), child);
            } else if (child instanceof ViewGroup) {
                findOffset((ViewGroup) child);
            }
        }
    }

    private void updateOffset(int oldHeight, int newHeight, View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int y = location[1];
        if (newHeight > oldHeight) {
            Integer conflictingHeightWithSameY = offsetWithY.get(y);
            newHeight = conflictingHeightWithSameY != null && conflictingHeightWithSameY > newHeight ?
                    conflictingHeightWithSameY : newHeight;
            offsetWithY.put(y, newHeight - oldHeight);
        }
    }

    private void updateTotalOffset() {
        for (Map.Entry<Integer, Integer> entry : offsetWithY.entrySet()) {
            offset = offset + offsetWithY.get(entry.getKey());
        }
    }
}
