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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class RomListDataProvider extends ContentProvider {

    private static final String AUTHORITY = "com.tassadar.multirommgr.provider";
    private static final String BASE_PATH = "rom_list";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);

    private static final int ALL_ROMS   = 1;
    private static final int SINGLE_ROM = 2;

    private static final UriMatcher URI_MATCHER;
    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI(AUTHORITY, BASE_PATH, ALL_ROMS);
        URI_MATCHER.addURI(AUTHORITY, BASE_PATH + "/#", SINGLE_ROM);
    }

    @Override
    public boolean onCreate() {
        m_db_helper = new RomListOpenHelper(getContext());
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = m_db_helper.getWritableDatabase();
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(RomListOpenHelper.ROMLIST_TABLE_NAME);

        if(URI_MATCHER.match(uri) == SINGLE_ROM) {
            String id = uri.getPathSegments().get(1);
            queryBuilder.appendWhere(RomListOpenHelper.KEY_ID + "=" + id);
        }

        return queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
    }

    @Override
    public String getType(Uri uri) {
        switch (URI_MATCHER.match(uri)) {
            case ALL_ROMS:
                return "vnd.android.cursor.dir/" + AUTHORITY + "." + BASE_PATH;
            case SINGLE_ROM:
                return "vnd.android.cursor.item/" + AUTHORITY + "." + BASE_PATH;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        if(URI_MATCHER.match(uri) != ALL_ROMS)
            throw new IllegalArgumentException("Unsupported URI: " + uri);

        SQLiteDatabase db = m_db_helper.getWritableDatabase();
        long id = db.insert(RomListOpenHelper.ROMLIST_TABLE_NAME, null, contentValues);
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(CONTENT_URI + "/" + id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = m_db_helper.getWritableDatabase();

        if(URI_MATCHER.match(uri) == SINGLE_ROM) {
            String id = uri.getPathSegments().get(1);
            selection = RomListOpenHelper.KEY_ID + "=" + id
                    + (!TextUtils.isEmpty(selection) ?
                    " AND (" + selection + ')' : "");
        }

        int rowsDeleted = db.delete(RomListOpenHelper.ROMLIST_TABLE_NAME, selection, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        SQLiteDatabase db = m_db_helper.getWritableDatabase();

        if(URI_MATCHER.match(uri) == SINGLE_ROM) {
            String id = uri.getPathSegments().get(1);
            selection = RomListOpenHelper.KEY_ID + "=" + id
                    + (!TextUtils.isEmpty(selection) ?
                    " AND (" + selection + ')' : "");
        }

        int rowsUpdated = db.update(RomListOpenHelper.ROMLIST_TABLE_NAME, contentValues, selection, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }

    private RomListOpenHelper m_db_helper;
}
