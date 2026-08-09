package com.khokhar.plusminus;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Icon;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.khokhar.game.GameServer;
import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.io.InputStream;

public class HostService extends Service {
    private static final String CHANNEL_ID = "PlusMinusServerChannel";
    private static final String TAG = "HostService";
    public static final String ACTION_STOP_SERVICE = "com.khokhar.plusminus.STOP_SERVICE";
    
    private GameServer gameServer;
    private WebAppServer webServer;
    
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        HostService getService() {
            return HostService.this;
        }
    }

    public static boolean isGameStarted = false;

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public static final String EXTRA_TARGET_SCORE = "target_score";

    private boolean serversStarted = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP_SERVICE.equals(intent.getAction())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE);
            } else {
                stopForeground(true);
            }
            stopSelf();
            return START_NOT_STICKY;
        }

        // Read target score and start servers on first launch
        if (!serversStarted) {
            int targetScore = 21;
            if (intent != null) {
                targetScore = intent.getIntExtra(EXTRA_TARGET_SCORE, 21);
            }
            startServers(targetScore);
        }

        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopSelf();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        try {
            createNotificationChannel();
            
            Intent stopIntent = new Intent(this, HostService.class);
            stopIntent.setAction(ACTION_STOP_SERVICE);
            PendingIntent pendingStopIntent = PendingIntent.getService(
                    this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            
            Intent appIntent = new Intent(this, MainActivity.class);
            appIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingAppIntent = PendingIntent.getActivity(
                    this, 0, appIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

            Notification.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder = new Notification.Builder(this, CHANNEL_ID);
            } else {
                builder = new Notification.Builder(this);
            }

            // Use Icon-based Action.Builder (available since API 23, minSdk is 26)
            Notification.Action stopAction = new Notification.Action.Builder(
                    Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
                    "Stop Server",
                    pendingStopIntent
            ).build();

            Notification notification = builder
                    .setContentTitle("Plus Minus Server")
                    .setContentText("Hosting the game on your local network")
                    .setSmallIcon(android.R.drawable.sym_def_app_icon)
                    .setContentIntent(pendingAppIntent)
                    .setOngoing(true)
                    .addAction(stopAction)
                    .build();
            
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
                } else {
                    startForeground(1, notification);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to start foreground with dataSync, falling back", e);
                startForeground(1, notification);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "FATAL: HostService onCreate crashed", e);
        }
    }

    private void startServers(int targetScore) {
        serversStarted = true;
        try {
            gameServer = new GameServer(8887);
            gameServer.setTargetScore(targetScore);
            gameServer.start();
        } catch (Exception e) {
            Log.e(TAG, "Failed to start GameServer", e);
        }

        try {
            webServer = new WebAppServer(this, 8080);
            webServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        } catch (IOException e) {
            Log.e(TAG, "Failed to start WebAppServer", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        HostService.isGameStarted = false;
        if (webServer != null) webServer.stop();
        if (gameServer != null) {
            try {
                gameServer.stop();
            } catch (InterruptedException e) {
                Log.e(TAG, "Failed to stop GameServer", e);
            }
        }
    }

    public int getConnectionCount() {
        if (gameServer != null) return gameServer.getConnectionCount();
        return 0;
    }

    public boolean isServerReady() {
        return webServer != null && webServer.isAlive();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Plus Minus Server Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private static class WebAppServer extends NanoHTTPD {
        private final Context context;

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
