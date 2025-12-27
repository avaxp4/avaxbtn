package com.mytools.powerbutton;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

public class FloatingService extends AccessibilityService {

    private WindowManager windowManager;
    private View floatingView;
    private View layoutExpanded;
    private View layoutCollapsed;
    private WindowManager.LayoutParams params;
    private AudioManager audioManager;
    private boolean isCollapsed = false;

    private final Handler dimHandler = new Handler(Looper.getMainLooper());
    private final Runnable dimRunnable = () -> {
        if (floatingView != null) floatingView.animate().alpha(0.3f).setDuration(500).start();
    };

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();

        try {
            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                            WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);

            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 0;
            params.y = 200;

            floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_button, null);

            layoutExpanded = floatingView.findViewById(R.id.layout_expanded);
            layoutCollapsed = floatingView.findViewById(R.id.layout_collapsed);

            View btnMinimize = floatingView.findViewById(R.id.btn_minimize);
            View btnPower = floatingView.findViewById(R.id.btn_power);
            View btnVolUp = floatingView.findViewById(R.id.btn_vol_up);
            View btnVolDown = floatingView.findViewById(R.id.btn_vol_down);
            View btnScreenshot = floatingView.findViewById(R.id.btn_screenshot);
            View dragHandle = floatingView.findViewById(R.id.drag_handle);

            View.OnClickListener clickWrapper = (v) -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                resetDimTimer();
            };

            btnMinimize.setOnClickListener(v -> {
                clickWrapper.onClick(v);
                collapseView();
            });

            btnPower.setOnClickListener(v -> {
                clickWrapper.onClick(v);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    boolean locked = performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
                    if (!locked) performGlobalAction(GLOBAL_ACTION_POWER_DIALOG);
                } else {
                    performGlobalAction(GLOBAL_ACTION_POWER_DIALOG);
                }
            });
            
            btnPower.setOnLongClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                performGlobalAction(GLOBAL_ACTION_POWER_DIALOG);
                return true;
            });

            btnVolUp.setOnClickListener(v -> {
                clickWrapper.onClick(v);
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
            });

            btnVolDown.setOnClickListener(v -> {
                clickWrapper.onClick(v);
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
            });

            btnScreenshot.setOnClickListener(v -> {
                clickWrapper.onClick(v);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT);
                }
            });

            View.OnTouchListener touchListener = new View.OnTouchListener() {
                private int initialX, initialY;
                private float initialTouchX, initialTouchY;
                private boolean isClick = false;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    resetDimTimer();
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = params.x;
                            initialY = params.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            isClick = true;
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            int diffX = (int) (event.getRawX() - initialTouchX);
                            int diffY = (int) (event.getRawY() - initialTouchY);
                            if (Math.abs(diffX) > 10 || Math.abs(diffY) > 10) {
                                isClick = false;
                                params.x = initialX + diffX;
                                params.y = initialY + diffY;
                                windowManager.updateViewLayout(floatingView, params);
                            }
                            return true;

                        case MotionEvent.ACTION_UP:
                            if (isClick && v == layoutCollapsed) {
                                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                                expandView();
                            }
                            return true;
                    }
                    return false;
                }
            };

            if (dragHandle != null) dragHandle.setOnTouchListener(touchListener);
            if (layoutCollapsed != null) layoutCollapsed.setOnTouchListener(touchListener);

            windowManager.addView(floatingView, params);
            resetDimTimer();

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void collapseView() {
        if (layoutExpanded != null && layoutCollapsed != null) {
            layoutExpanded.setVisibility(View.GONE);
            layoutCollapsed.setVisibility(View.VISIBLE);
            isCollapsed = true;
        }
    }

    private void expandView() {
        if (layoutExpanded != null && layoutCollapsed != null) {
            layoutCollapsed.setVisibility(View.GONE);
            layoutExpanded.setVisibility(View.VISIBLE);
            isCollapsed = false;
        }
    }

    private void resetDimTimer() {
        if (floatingView != null) {
            floatingView.animate().alpha(1.0f).setDuration(200).start();
            dimHandler.removeCallbacks(dimRunnable);
            dimHandler.postDelayed(dimRunnable, 3000);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null && windowManager != null) windowManager.removeView(floatingView);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}
}