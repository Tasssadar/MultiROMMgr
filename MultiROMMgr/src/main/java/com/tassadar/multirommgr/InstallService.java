package com.tassadar.multirommgr;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.text.Html;
import android.util.Log;

import java.lang.ref.WeakReference;

public class InstallService extends Service implements InstallListener {

    static final int NOTIFICATION_ID = 1;

    @Override
    public void onCreate() {
        m_notificationMgr = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        Notification.Builder b = new Notification.Builder(this);
        Intent i = new Intent(this, InstallActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i, 0);

        b.setContentTitle(getText(R.string.app_name))
                .setOngoing(true)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_launcher)
                .setProgress(0, 0, true);

        startForeground(NOTIFICATION_ID, b.build());
        m_builder = b;

        Log.e("InstallService", "Create");
    }

    @Override
    public void onDestroy() {
        Log.e("InstallService", "Destroy");
        m_builder = null;
        m_notificationMgr.cancel(NOTIFICATION_ID);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return m_binder;
    }

    public void cancel() {
        stopSelf();
    }

    public void setListener(InstallListener l) {
        m_listener = new WeakReference<InstallListener>(l);
    }

    public boolean isInProgress() {
        return m_isInProgress;
    }

    public void startInstallation(Manifest man, boolean multirom, boolean recovery,
                                  boolean kernel, String kernel_name) {

        m_isInProgress = true;

        InstallAsyncTask task = new InstallAsyncTask(man, multirom, recovery,
                kernel ? kernel_name : null);
        task.setListener(this);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void)null);
    }

    public String getFullLog() {
        return m_log.toString();
    }

    @Override
    public void onInstallLog(String str) {
        m_log.append(str);
        InstallListener l = m_listener.get();
        if(l != null)
            l.onInstallLog(str);
    }

    @Override
    public void onInstallComplete() {

    }

    @Override
    public void onProgressUpdate(int val, int max, boolean indeterminate, String text) {
        m_builder.setProgress(max, val, indeterminate)
                .setContentText(Html.fromHtml(text));
        m_notificationMgr.notify(NOTIFICATION_ID, m_builder.build());

        InstallListener l = m_listener.get();
        if(l != null)
            l.onProgressUpdate(val, max, indeterminate, text);
    }

    public class InstallServiceBinder extends Binder {
        InstallService get() {
            return InstallService.this;
        }
    }

    private final InstallServiceBinder m_binder = new InstallServiceBinder();
    private NotificationManager m_notificationMgr;
    private Notification.Builder m_builder;
    private WeakReference<InstallListener> m_listener;
    private boolean m_isInProgress = false;
    private StringBuffer m_log = new StringBuffer(1024);
}
