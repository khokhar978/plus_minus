package com.khokhar.plusminus;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
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
import java.util.Enumeration;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private static final int NOTIFICATION_PERMISSION_REQUEST = 101;
    
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
        try {
            if (getWindow() == null) return;
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                getWindow().setDecorFitsSystemWindows(false);
                android.view.WindowInsetsController controller = getWindow().getInsetsController();
                if (controller != null) {
                    controller.hide(android.view.WindowInsets.Type.systemBars());
                    controller.setSystemBarsBehavior(
                            android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            } else {
                View decorView = getWindow().getDecorView();
                if (decorView != null) {
                    decorView.setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "setImmersiveMode failed", e);
        }
    }

    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) return null;
            
            String fallbackIp = null;
            for (NetworkInterface intf : Collections.list(interfaces)) {
                for (InetAddress addr : Collections.list(intf.getInetAddresses())) {
                    if (!addr.isLoopbackAddress() && addr.getAddress().length == 4) {
                        String name = intf.getName().toLowerCase();
                        if (name.contains("wlan") || name.contains("ap") || name.contains("hotspot") || name.contains("swlan")) {
                            return addr.getHostAddress();
                        }
                        if (fallbackIp == null) {
                            fallbackIp = addr.getHostAddress();
                        }
                    }
                }
            }
            return fallbackIp;
        } catch (Exception ex) {
            Log.e(TAG, "Failed to get local IP address", ex);
        }
        return null;
    }

    private void loadGameInWebView(WebView webView, LinearLayout qrContainer) {
        HostService.isGameStarted = true;
        qrContainer.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        
        android.webkit.WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        
        webView.setWebViewClient(new android.webkit.WebViewClient() {
            @Override
            public void onReceivedError(WebView view, android.webkit.WebResourceRequest request, android.webkit.WebResourceError error) {
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

    private void startHostService() {
        try {
            Intent intent = new Intent(this, HostService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            bindService(intent, connection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start HostService", e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_main);
            setImmersiveMode();

            ImageView qrImage = findViewById(R.id.qrCodeImage);
            TextView ipText = findViewById(R.id.ipAddressText);
            TextView connectedText = findViewById(R.id.connectedPlayersText);
            Button btnStart = findViewById(R.id.btnStartGame);
            LinearLayout qrContainer = findViewById(R.id.qrContainer);
            WebView webView = findViewById(R.id.gameWebView);

            String ipAddress = getLocalIpAddress();
            
            if (ipAddress == null) {
                if (ipText != null) ipText.setText("Please connect to Wi-Fi or start a Hotspot");
                if (qrImage != null) qrImage.setVisibility(View.GONE);
            } else {
                String url = "http://" + ipAddress + ":8080";
                if (ipText != null) ipText.setText(url);
                if (qrImage != null) qrImage.setVisibility(View.VISIBLE);

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
                    if (qrImage != null) qrImage.setImageBitmap(bmp);
                } catch (WriterException e) {
                    Log.e(TAG, "Failed to generate QR code", e);
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST);
                } else {
                    startHostService();
                }
            } else {
                startHostService();
            }

            pollRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isBound && hostService != null) {
                        int count = hostService.getConnectionCount();
                        if (connectedText != null) {
                            connectedText.setText("Connected: " + count + "/4");
                        }
                        
                        // Hide Start Game button if there are already 4 devices connected (lobby full)
                        // Or if the game is already started on this device
                        if (btnStart != null) {
                            if (HostService.isGameStarted) {
                                btnStart.setVisibility(View.GONE);
                            } else {
                                btnStart.setVisibility(count >= 4 ? View.GONE : View.VISIBLE);
                            }
                        }
                    }
                    handler.postDelayed(this, 1000);
                }
            };
            handler.post(pollRunnable);
            
            if (HostService.isGameStarted && webView != null && qrContainer != null) {
                loadGameInWebView(webView, qrContainer);
            }

            if (btnStart != null && webView != null && qrContainer != null) {
                btnStart.setOnClickListener(v -> {
                    loadGameInWebView(webView, qrContainer);
                });
            }

            Button btnStop = findViewById(R.id.btnStopServer);
            if (btnStop != null) {
                btnStop.setOnClickListener(v -> {
                    stopService(new Intent(MainActivity.this, HostService.class));
                    finish();
                });
            }
            
        } catch (Exception e) {
            Log.e(TAG, "FATAL: onCreate crashed", e);
        }
    }

    @Override
    public void onBackPressed() {
        WebView webView = findViewById(R.id.gameWebView);
        LinearLayout qrContainer = findViewById(R.id.qrContainer);
        if (webView != null && webView.getVisibility() == View.VISIBLE) {
            // If they are in the game, go back to the server screen
            webView.setVisibility(View.GONE);
            if (qrContainer != null) {
                qrContainer.setVisibility(View.VISIBLE);
            }
            HostService.isGameStarted = false; // We left the game view
            webView.loadUrl("about:blank"); // clear the webview
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST) {
            startHostService();
        }
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
