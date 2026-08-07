package com.khokhar.plusminus;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopSelf();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        
        Intent stopIntent = new Intent(this, HostService.class);
        stopIntent.setAction(ACTION_STOP_SERVICE);
        PendingIntent pendingStopIntent = PendingIntent.getService(
                this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Intent appIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingAppIntent = PendingIntent.getActivity(
                this, 0, appIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Action stopAction = new Notification.Action.Builder(
                android.R.drawable.ic_delete, "Stop Server", pendingStopIntent).build();

        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("Plus Minus Server")
                    .setContentText("Hosting the game on your local network")
                    .setSmallIcon(android.R.drawable.sym_def_app_icon)
                    .setContentIntent(pendingAppIntent)
                    .setOngoing(true)
                    .addAction(stopAction)
                    .build();
        } else {
            notification = new Notification.Builder(this)
                    .setContentTitle("Plus Minus Server")
                    .setContentText("Hosting the game on your local network")
                    .setSmallIcon(android.R.drawable.sym_def_app_icon)
                    .setContentIntent(pendingAppIntent)
                    .setOngoing(true)
                    .addAction(stopAction)
                    .build();
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, 1); // 1 = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            startForeground(1, notification);
        }

        gameServer = new GameServer(8887);
        gameServer.start();

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
