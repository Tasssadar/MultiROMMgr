/*
 * This file is part of MultiROM Manager.
 *
 * MultiROM Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MultiROM Manager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MultiROM Manager. If not, see <http://www.gnu.org/licenses/>.
 */

package com.tassadar.multirommgr.installfragment;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.text.Html;

import com.tassadar.multirommgr.Device;
import com.tassadar.multirommgr.Manifest;
import com.tassadar.multirommgr.MultiROM;
import com.tassadar.multirommgr.R;

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

        m_builder = b;
    }

    @Override
    public void onDestroy() {
        m_builder = null;
        m_notificationMgr.cancel(NOTIFICATION_ID);

        releaseWakeLock();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return m_binder;
    }

    public void cancel() {
        if(m_task != null)
            m_task.setCanceled(true);
        releaseWakeLock();
        stopSelf();
    }

    public void setListener(InstallListener l) {
        m_listener = new WeakReference<InstallListener>(l);
    }

    public boolean isInProgress() {
        return m_isInProgress;
    }

    public void startMultiROMInstallation(Manifest man, Device dev,
                                          boolean multirom, boolean recovery,
                                          boolean kernel, String kernel_name) {
        MultiROMInstallTask t = new MultiROMInstallTask(man, dev);
        t.setParts(multirom, recovery, kernel ? kernel_name : null);
        startInstallation(t);
    }

    public void startUbuntuInstallation(UbuntuInstallInfo info, MultiROM multirom, Device dev) {
        startInstallation(new UbuntuInstallTask(info, multirom, dev));
    }

    private void startInstallation(InstallAsyncTask task) {
        startForeground(NOTIFICATION_ID, m_builder.build());

        m_log = new StringBuffer(1024);
        m_isInProgress = true;
        m_enableCancel = true;
        m_requestRecovery = 0;
        m_completed = false;

        PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);
        m_wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE,
                                    "MultiROMMgr");
        m_wakeLock.acquire();

        m_task = task;
        m_task.setListener(this);
        m_task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void)null);
    }

    private void releaseWakeLock() {
        if(m_wakeLock != null) {
            m_wakeLock.release();
            m_wakeLock = null;
        }
    }

    public String getFullLog() {
        return m_log.toString();
    }

    public boolean getEnableCancel() {
        return m_enableCancel;
    }
    public int wasRecoveryRequested() { return m_requestRecovery; }
    public boolean wasCompleted() { return m_completed; }

    @Override
    public void onInstallLog(String str) {
        m_log.append(str);
        InstallListener l = m_listener.get();
        if(l != null)
            l.onInstallLog(str);
    }

    @Override
    public void onInstallComplete(boolean success) {
        InstallListener l = m_listener.get();
        if(l != null)
            l.onInstallComplete(success);

        m_completed = true;
        m_isInProgress = false;
        m_task = null;
        stopForeground(true);

        releaseWakeLock();
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

    @Override
    public void enableCancel(boolean enabled) {
        m_enableCancel = enabled;
        InstallListener l = m_listener.get();
        if(l != null)
            l.enableCancel(enabled);
    }

    @Override
    public void requestRecovery(boolean force) {
        m_requestRecovery = 1 + (force ? 1 : 0);

        InstallListener l = m_listener.get();
        if(l != null)
            l.requestRecovery(force);
    }

    public void setRequestRecovery(int req) {
        m_requestRecovery = req;
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
    private boolean m_enableCancel = true;
    private int m_requestRecovery = 0;
    private boolean m_completed = false;
    private PowerManager.WakeLock m_wakeLock;
    private InstallAsyncTask m_task = null;
}
