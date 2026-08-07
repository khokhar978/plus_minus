package com.khokhar.plusminus;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private HostService hostService;
    private boolean isBound = false;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            HostService.LocalBinder binder = (HostService.LocalBinder) service;
            hostService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
        }
    };

    private void setImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            if (getWindow().getInsetsController() != null) {
                getWindow().getInsetsController().hide(android.view.WindowInsets.Type.systemBars());
                getWindow().getInsetsController().setSystemBarsBehavior(
                        android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    private String getLocalIpAddress() {
        try {
            for (NetworkInterface intf : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress addr : Collections.list(intf.getInetAddresses())) {
                    if (!addr.isLoopbackAddress() && addr.getAddress().length == 4) { // IPv4
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "Failed to get local IP address", ex);
        }
        return null;
    }

    private void loadGameInWebView(WebView webView, LinearLayout qrContainer) {
        HostService.isGameStarted = true;
        qrContainer.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    handler.postDelayed(() -> {
                        if (isBound && hostService != null && hostService.isServerReady()) {
                            view.loadUrl("http://localhost:8080");
                        } else {
                            view.reload();
                        }
                    }, 1000);
                }
            }
        });

        webView.loadUrl("http://localhost:8080");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setImmersiveMode();
        setContentView(R.layout.activity_main);

        ImageView qrImage = findViewById(R.id.qrCodeImage);
        TextView ipText = findViewById(R.id.ipAddressText);
        TextView connectedText = findViewById(R.id.connectedPlayersText);
        Button btnStart = findViewById(R.id.btnStartGame);
        WebView webView = findViewById(R.id.gameWebView);
        LinearLayout qrContainer = findViewById(R.id.qrContainer);

        String ipAddress = getLocalIpAddress();
        if (ipAddress == null) {
            ipText.setText("Please connect to Wi-Fi or start a Hotspot");
            qrImage.setVisibility(View.GONE);
        } else {
            String url = "http://" + ipAddress + ":8080";
            ipText.setText(url);
            qrImage.setVisibility(View.VISIBLE);

            try {
                QRCodeWriter writer = new QRCodeWriter();
                BitMatrix bitMatrix = writer.encode(url, BarcodeFormat.QR_CODE, 512, 512);
                int width = bitMatrix.getWidth();
                int height = bitMatrix.getHeight();
                int[] pixels = new int[width * height];
                for (int y = 0; y < height; y++) {
                    int offset = y * width;
                    for (int x = 0; x < width; x++) {
                        pixels[offset + x] = bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE;
                    }
                }
                Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                bmp.setPixels(pixels, 0, width, 0, 0, width, height);
                qrImage.setImageBitmap(bmp);
            } catch (WriterException e) {
                Log.e(TAG, "Failed to generate QR code", e);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        Intent intent = new Intent(this, HostService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        bindService(intent, connection, Context.BIND_AUTO_CREATE);

        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (isBound && hostService != null) {
                    int count = hostService.getConnectionCount();
                    connectedText.setText("Connected: " + count + "/4");
                    
                    btnStart.setVisibility(count >= 3 ? View.VISIBLE : View.GONE);
                }
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(pollRunnable);

        if (HostService.isGameStarted) {
            loadGameInWebView(webView, qrContainer);
        }

        btnStart.setOnClickListener(v -> {
            loadGameInWebView(webView, qrContainer);
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            setImmersiveMode();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pollRunnable != null) handler.removeCallbacks(pollRunnable);
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
    }
}
