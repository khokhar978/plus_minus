package com.khokhar.plusminus;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import com.khokhar.game.GameServer;
import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public class MainActivity extends Activity {

    private GameServer gameServer;
    private WebAppServer webServer;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView qrImage = findViewById(R.id.qrCodeImage);
        TextView ipText = findViewById(R.id.ipAddressText);
        TextView connectedText = findViewById(R.id.connectedPlayersText);
        Button btnStart = findViewById(R.id.btnStartGame);
        WebView webView = findViewById(R.id.gameWebView);
        LinearLayout qrContainer = findViewById(R.id.qrContainer);

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        int ip = wifiManager.getConnectionInfo().getIpAddress();
        String ipAddress = String.format(Locale.getDefault(), "%d.%d.%d.%d",
                (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
        
        String url = "http://" + ipAddress + ":8080";
        ipText.setText(url);

        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(url, BarcodeFormat.QR_CODE, 512, 512);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            qrImage.setImageBitmap(bmp);
        } catch (WriterException e) {
            e.printStackTrace();
        }

        gameServer = new GameServer(8887);
        gameServer.start();

        try {
            webServer = new WebAppServer(this, 8080);
            webServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (gameServer != null) {
                    int count = gameServer.getConnectionCount();
                    connectedText.setText("Connected: " + count + "/4");
                    
                    if (count >= 3) {
                        btnStart.setVisibility(View.VISIBLE);
                    }
                }
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(pollRunnable);

        btnStart.setOnClickListener(v -> {
            qrContainer.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            
            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            webView.loadUrl("http://localhost:8080");
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pollRunnable != null) handler.removeCallbacks(pollRunnable);
        if (webServer != null) webServer.stop();
        if (gameServer != null) {
            try {
                gameServer.stop();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static class WebAppServer extends NanoHTTPD {
        private Context context;

        public WebAppServer(Context context, int port) {
            super(port);
            this.context = context;
        }

        @Override
        public Response serve(IHTTPSession session) {
            String uri = session.getUri();
            if (uri.equals("/")) {
                uri = "/index.html";
            }
            String assetPath = uri.substring(1);
            try {
                InputStream is = context.getAssets().open(assetPath);
                String mimeType = getMimeTypeForFile(uri);
                return newChunkedResponse(Response.Status.OK, mimeType, is);
            } catch (IOException e) {
                try {
                    InputStream is = context.getAssets().open("index.html");
                    return newChunkedResponse(Response.Status.OK, "text/html", is);
                } catch (IOException ex) {
                    return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "404 Not Found");
                }
            }
        }
    }
}
