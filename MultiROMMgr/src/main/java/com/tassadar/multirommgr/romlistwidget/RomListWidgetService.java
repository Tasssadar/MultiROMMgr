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

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.tassadar.multirommgr.R;

import java.io.File;

public class RomListWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RomListViewsFactory(this.getApplicationContext(), intent);
    }
}

class RomListViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    public RomListViewsFactory(Context ctx, Intent intent) {
        m_context = ctx;
    }

    @Override
    public void onCreate() {
        // cursor is initiated in onDataSetChanged();
    }

    @Override
    public void onDataSetChanged() {
        if (m_cursor != null)
            m_cursor.close();

        m_cursor = m_context.getContentResolver()
                .query(RomListDataProvider.CONTENT_URI, null, null, null, null);
    }

    @Override
    public void onDestroy() {
        if(m_cursor != null)
            m_cursor.close();
    }

    @Override
    public int getCount() {
        return m_cursor.getCount();
    }

    @Override
    public RemoteViews getViewAt(int i) {

        if(!m_cursor.moveToPosition(i))
            return null;

        int nameCol = m_cursor.getColumnIndex(RomListOpenHelper.KEY_NAME);
        int typeCol = m_cursor.getColumnIndex(RomListOpenHelper.KEY_TYPE);
        int iconNameCol = m_cursor.getColumnIndex(RomListOpenHelper.KEY_ICON_NAME);

        String name = m_cursor.getString(nameCol);
        int type = m_cursor.getInt(typeCol);
        String iconName = m_cursor.getString(iconNameCol);

        int icon_id = 0;
        Bitmap icon_bitmap = null;
        if(iconName.startsWith(m_context.getApplicationContext().getPackageName())) {
            icon_id = m_context.getResources().getIdentifier(iconName, null, null);
        } else {
            File path = new File(m_context.getDir("icons", 0), iconName + ".png");
            icon_bitmap = BitmapFactory.decodeFile(path.getAbsolutePath());
        }

        if(icon_id == 0 && icon_bitmap == null)
            icon_id = R.drawable.romic_default;

        RemoteViews rv = new RemoteViews(m_context.getPackageName(), R.layout.rom_list_widget_item);
        rv.setTextViewText(R.id.rom_name, name);
        if(icon_id != 0)
            rv.setImageViewResource(R.id.rom_icon, icon_id);
        else
            rv.setImageViewBitmap(R.id.rom_icon, icon_bitmap);

        final Intent fillInIntent = new Intent();
        final Bundle extras = new Bundle();
        extras.putString(RomListOpenHelper.KEY_NAME, name);
        extras.putInt(RomListOpenHelper.KEY_TYPE, type);
        fillInIntent.putExtras(extras);
        rv.setOnClickFillInIntent(R.id.rom_list_widget_item, fillInIntent);

        return rv;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int i) {
        if(m_cursor.moveToPosition(i)) {
            int colId = m_cursor.getColumnIndex(RomListOpenHelper.KEY_ID);
            return m_cursor.getLong(colId);
        }
        return -1;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    private Context m_context;
    private Cursor m_cursor;
}
