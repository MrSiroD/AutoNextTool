package com.donglm.autoSwipe;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class MyAccessibilityService extends AccessibilityService {

    public static MyAccessibilityService instance;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        instance = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Not used
    }

    @Override
    public void onInterrupt() {
        // Not used
    }

    public void performSwipe() {
//        android.util.Log.d("donglm.hbh", "performSwipe called");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            DisplayMetrics displayMetrics = android.content.res.Resources.getSystem().getDisplayMetrics();
            int screenWidth = displayMetrics.widthPixels;
            int screenHeight = displayMetrics.heightPixels;

            android.content.SharedPreferences sharedPref = getSharedPreferences("MyApp", android.content.Context.MODE_PRIVATE);
            int direction = sharedPref.getInt("swipe_direction", 0);
//            Log.d("donglm.hbh", "direction: " + direction);
            Path swipePath = new Path();
            if (direction == 1) {
                swipePath.moveTo(screenWidth / 2f, screenHeight * 0.2f);
                swipePath.lineTo(screenWidth / 2f, screenHeight * 0.8f);
            } else if (direction == 2) {
                swipePath.moveTo(screenWidth * 0.8f, screenHeight / 2f);
                swipePath.lineTo(screenWidth * 0.2f, screenHeight / 2f);
            } else if (direction == 3) {
                swipePath.moveTo(screenWidth * 0.2f, screenHeight / 2f);
                swipePath.lineTo(screenWidth * 0.8f, screenHeight / 2f);
            } else {
                swipePath.moveTo(screenWidth / 2f, screenHeight * 0.8f);
                swipePath.lineTo(screenWidth / 2f, screenHeight * 0.2f);
            }

            GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
            gestureBuilder.addStroke(new GestureDescription.StrokeDescription(swipePath, 0, 500));

            boolean result = dispatchGesture(gestureBuilder.build(), new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
//                    android.util.Log.d("donglm", "Gesture completed");
                    super.onCompleted(gestureDescription);
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
//                    android.util.Log.d("donglm", "Gesture cancelled");
                    super.onCancelled(gestureDescription);
                }
            }, null);
//            android.util.Log.d("donglm", "dispatchGesture returned " + result);
        }
    }

    public void performDoubleClick() {
        android.util.Log.d("donglm", "performDoubleClick called");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            DisplayMetrics displayMetrics = android.content.res.Resources.getSystem().getDisplayMetrics();
            int screenWidth = displayMetrics.widthPixels;
            int screenHeight = displayMetrics.heightPixels;

            Path clickPath = new Path();
            clickPath.moveTo(screenWidth / 2f, screenHeight / 2f);

            GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
            GestureDescription.StrokeDescription stroke1 = new GestureDescription.StrokeDescription(clickPath, 0, 50);
            GestureDescription.StrokeDescription stroke2 = new GestureDescription.StrokeDescription(clickPath, 100, 50);
            gestureBuilder.addStroke(stroke1);
            gestureBuilder.addStroke(stroke2);

            dispatchGesture(gestureBuilder.build(), null, null);
        }
    }
}
