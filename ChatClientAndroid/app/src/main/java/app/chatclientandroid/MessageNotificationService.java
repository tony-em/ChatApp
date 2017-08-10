package app.chatclientandroid;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

public class MessageNotificationService extends Service {

    private NotificationManager notificationManager;
    private NotificationCompat.Builder msgNotifBuilder;

    private int msgCounter;

    @Override
    public void onCreate() {
        super.onCreate();
        buildMsgNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String[] msg;
        try {
            msg = intent.getExtras().getStringArray("msgNotification");
            showMsgNotification(msg);
        } catch (NullPointerException e) {
        }

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        msgCounter = 0;
        notificationManager.cancelAll();
    }

    private void buildMsgNotification() {
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);

        msgNotifBuilder = new NotificationCompat.Builder(getApplicationContext())
                .setContentIntent(PendingIntent.getActivity(
                        getApplicationContext(), 0, intent, 0))
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setVisibility(Notification.VISIBILITY_SECRET);

            msgCounter = 0;
    }

    private void showMsgNotification(String[] msg) {
        msgNotifBuilder.setNumber(++msgCounter)
                .setTicker("New message from " + msg[1])
                .setContentTitle(msg[1])
                .setContentText(msg[2])
                .setDefaults(Notification.DEFAULT_LIGHTS |
                        Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);

        notificationManager.notify(1, msgNotifBuilder.build());
    }
}
