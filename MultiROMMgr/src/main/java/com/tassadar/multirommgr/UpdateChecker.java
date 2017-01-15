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

package com.tassadar.multirommgr;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

public class UpdateChecker {
    private static final String TAG = "MROMMgr::UpdateChecker";
    private static final int REQ_UPDATE_CHECK = 1;
    private static final int UPDATE_NOTIFICATION_ID = 1;

    static void setVersions(Device d, MultiROM m, Recovery r) {
        SharedPreferences.Editor p = MgrApp.getPreferences().edit();
        if(m != null && r != null) {
            p.putBoolean("has_versions", true);
            p.putString("update_device", d.getName());
            p.putString("last_multirom_ver", m.getVersion());
            p.putString("last_recovery_ver", r.getVersionString());
        } else {
            p.putBoolean("has_versions", false);
        }
        p.apply();

        updateAlarmStatus();
    }

    public static void lazyUpdateVersions(Device d, String m_ver, String r_ver) {
        SharedPreferences p = MgrApp.getPreferences();
        if(!p.getBoolean("has_versions", false))
            return;

        SharedPreferences.Editor e = p.edit();

        e.putString("update_device", d.getName());
        if(m_ver != null)
            e.putString("last_multirom_ver", m_ver);

        if(r_ver != null)
            e.putString("last_recovery_ver", r_ver);

        e.apply();
    }

    public static boolean isEnabled() {
        final SharedPreferences p = MgrApp.getPreferences();
        return p.getBoolean(SettingsFragment.GENERAL_UPDATE_CHECK, false);
    }

    private static PendingIntent getIntent(Context ctx, int flags) {
        Intent i = new Intent(BuildConfig.APPLICATION_ID + ".CHECK_UPDATES");
        return PendingIntent.getBroadcast(ctx, REQ_UPDATE_CHECK, i, flags);
    }

    static void updateAlarmStatus() {
        Context ctx = MgrApp.getAppContext();
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(ctx);
        PendingIntent i = getIntent(ctx, PendingIntent.FLAG_NO_CREATE);

        boolean isRunning = (i != null);
        boolean run = p.getBoolean(SettingsFragment.GENERAL_UPDATE_CHECK, false) &&
                p.getBoolean("has_versions", false);

        if(isRunning == run)
            return;

        AlarmManager mgr = (AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
        if(run) {
            i = getIntent(ctx, 0);
            mgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, 2000,
                    AlarmManager.INTERVAL_HALF_DAY, i);
        } else {
            mgr.cancel(i);
            i.cancel();
        }

        Log.d(TAG, "Setting update alarm to " + run);
    }

    private static void spawnUpdateNotification(Context ctx) {
        Intent i = new Intent(ctx, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra("force_refresh", true);
        PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        CharSequence update_notify = ctx.getText(R.string.update_notification);

        Notification.Builder b = new Notification.Builder(ctx);
        b.setContentTitle(ctx.getText(R.string.app_name))
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_stat_notify_generic)
                .setContentText(update_notify)
                .setTicker(update_notify);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            b.setColor(ctx.getResources().getColor(R.color.notification_bg));
            b.setVisibility(Notification.VISIBILITY_PUBLIC);
        }

        NotificationManager mgr = (NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        mgr.notify(UPDATE_NOTIFICATION_ID, b.build());
    }

    private static class UpdateCheckThread extends Thread {
        @Override
        public void run() {
            Log.i(TAG, "Checking for updates...");

            SharedPreferences p = MgrApp.getPreferences();
            String mromVer = p.getString("last_multirom_ver", null);
            String recoveryVer = p.getString("last_recovery_ver", null);
            Device dev = Device.load(p.getString(SettingsFragment.DEV_DEVICE_NAME, Build.DEVICE));

            if(dev == null || mromVer == null || recoveryVer == null)
                return;

            Manifest man = new Manifest();

            // Do not check GPG signature here, it requires root, and I don't want phantom
            // "..has acquired root.." toasts. The signature will be checked when the user opens
            // the app anyway
            if(!man.downloadAndParse(dev, false)) {
                Log.e(TAG, "Failed to download manifest in update checker!");
                return;
            }

            man.compareVersions(mromVer, recoveryVer, null);
            if(man.hasMultiromUpdate() || man.hasRecoveryUpdate())
                spawnUpdateNotification(MgrApp.getAppContext());
        }
    }

    public static class UpdateCheckReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Utils.installHttpCache(context);

            new UpdateCheckThread().start();
        }
    }

    public static class BootCompletedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Updating alarm status on boot...");
            updateAlarmStatus();
        }
    }
}
