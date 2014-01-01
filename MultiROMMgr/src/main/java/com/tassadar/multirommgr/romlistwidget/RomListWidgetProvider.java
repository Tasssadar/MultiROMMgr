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

package com.tassadar.multirommgr.romlistwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.RemoteViews;

import com.tassadar.multirommgr.MainActivity;
import com.tassadar.multirommgr.MgrApp;
import com.tassadar.multirommgr.MultiROM;
import com.tassadar.multirommgr.R;
import com.tassadar.multirommgr.romlistfragment.RomBootActivity;

import java.lang.ref.WeakReference;

public class RomListWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_REFRESH = "com.tassadar.multirommgr.romlistwidget.REFRESH";
    private static final String ACTION_ROM_CLICK = "com.tassadar.multirommgr.romlistwidget.ROM_CLICK";

    static public void notifyChanged() {
        Context ctx = MgrApp.getAppContext();
        AppWidgetManager man = AppWidgetManager.getInstance(ctx);
        int[] ids = man.getAppWidgetIds(new ComponentName(ctx, RomListWidgetProvider.class));
        if(ids != null && ids.length != 0)
            man.notifyAppWidgetViewDataChanged(ids, R.id.rom_list);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.rom_list_widget);
            views.setEmptyView(R.id.rom_list, R.id.rom_list_empty_text);

            final Intent intent = new Intent(context, RomListWidgetService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            views.setRemoteAdapter(R.id.rom_list, intent);

            final Intent refreshIntent = new Intent(context, RomListWidgetProvider.class);
            refreshIntent.setAction(ACTION_REFRESH);
            final PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(context, 0,
                    refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.refresh_btn, refreshPendingIntent);

            final Intent romClickIntent = new Intent(context, RomListWidgetProvider.class);
            romClickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
            romClickIntent.setAction(ACTION_ROM_CLICK);
            romClickIntent.setData(Uri.parse(romClickIntent.toUri(Intent.URI_INTENT_SCHEME)));
            final PendingIntent romClickPendingIntent = PendingIntent.getBroadcast(context, 0,
                    romClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setPendingIntentTemplate(R.id.rom_list, romClickPendingIntent);

            final Intent headerClickedIntent = context.getPackageManager()
                    .getLaunchIntentForPackage(MgrApp.getAppContext().getPackageName());
            if(headerClickedIntent != null) {
                headerClickedIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                headerClickedIntent.putExtra(MainActivity.INTENT_EXTRA_SHOW_ROM_LIST, true);
                headerClickedIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                final PendingIntent headerClickedPendingIntent = PendingIntent.getActivity(context, 0,
                        headerClickedIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                views.setOnClickPendingIntent(R.id.rom_list_widget_header, headerClickedPendingIntent);
            }

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(ACTION_REFRESH)) {
            RefreshThread.startIfNotRunning();
        } else if(action.equals(ACTION_ROM_CLICK)) {
            Intent i = new Intent(MgrApp.getAppContext(), RomBootActivity.class);
            i.putExtras(intent.getExtras());
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            ctx.startActivity(i);
        }
        super.onReceive(ctx, intent);
    }

    private static class RefreshThread extends Thread {
        private static WeakReference<RefreshThread> s_instance = new WeakReference<RefreshThread>(null);

        static public synchronized void startIfNotRunning() {
            RefreshThread t = s_instance.get();
            if(t != null && t.isAlive())
                return;

            t = new RefreshThread();
            t.start();
            s_instance = new WeakReference<RefreshThread>(t);
        }

        @Override
        public void run() {
            MultiROM m = new MultiROM();
            if(!m.findMultiROMDir())
                return;

            m.findRoms();
        }
    }
}
