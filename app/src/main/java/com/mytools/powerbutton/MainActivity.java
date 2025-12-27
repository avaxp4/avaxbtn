package com.mytools.powerbutton;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView statusText;
    private Button btnOverlay;
    private Button btnAccess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(50, 50, 50, 50);
        layout.setBackgroundColor(Color.parseColor("#121212"));

        TextView title = new TextView(this);
        title.setText("AvaxBtn");
        title.setTextSize(32);
        title.setTextColor(Color.parseColor("#00E5FF"));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 50);
        layout.addView(title);

        statusText = new TextView(this);
        statusText.setTextSize(18);
        statusText.setTextColor(Color.WHITE);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 0, 0, 80);
        layout.addView(statusText);

        btnOverlay = createButton("1. Grant Overlay Permission");
        btnOverlay.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });
        layout.addView(btnOverlay);

        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(1, 40));
        layout.addView(spacer);

        btnAccess = createButton("2. Enable Accessibility Service");
        btnAccess.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "Find 'AvaxBtn' and turn it ON", Toast.LENGTH_LONG).show();
        });
        layout.addView(btnAccess);
        
        TextView footer = new TextView(this);
        footer.setText("To stop the app, turn off the Accessibility Service.");
        footer.setTextColor(Color.GRAY);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, 100, 0, 0);
        layout.addView(footer);

        setContentView(layout);
    }

    private Button createButton(String text) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setBackgroundColor(Color.DKGRAY);
        btn.setTextColor(Color.WHITE);
        return btn;
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissions();
    }

    private void checkPermissions() {
        boolean overlayOk = Settings.canDrawOverlays(this);
        boolean serviceOk = isAccessibilityServiceEnabled(this, FloatingService.class);

        if (overlayOk) {
            btnOverlay.setText("✅ Overlay Permission Granted");
            btnOverlay.setTextColor(Color.GREEN);
            btnOverlay.setEnabled(false);
        } else {
            btnOverlay.setText("1. Grant Overlay Permission");
            btnOverlay.setTextColor(Color.RED);
            btnOverlay.setEnabled(true);
        }

        if (serviceOk) {
            btnAccess.setText("✅ Service is Running");
            btnAccess.setTextColor(Color.GREEN);
            statusText.setText("Status: ACTIVE 🟢");
        } else {
            btnAccess.setText("2. Enable Accessibility Service");
            btnAccess.setTextColor(Color.RED);
            statusText.setText("Status: STOPPED 🔴");
        }
    }

    public static boolean isAccessibilityServiceEnabled(Context context, Class<?> service) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo enabledService : enabledServices) {
            if (enabledService.getResolveInfo().serviceInfo.name.contains(service.getSimpleName())) {
                return true;
            }
        }
        return false;
    }
}