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

package com.tassadar.multirommgr.romlistfragment;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

import com.tassadar.multirommgr.R;
import com.tassadar.multirommgr.Rom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class RomListAdapter implements ListAdapter {

    public RomListAdapter(Context ctx, RomListItem.OnRomActionListener listener) {
        m_inflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        m_listener = listener;
    }

    public void clear() {
        m_roms.clear();
        for(DataSetObserver o : m_observers)
            o.onInvalidated();
    }

    public void set(ArrayList<Rom> roms) {
        m_roms = new ArrayList<Rom>(roms);
        for(DataSetObserver o : m_observers)
            o.onChanged();
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        RomListItem view;

        if (convertView == null) {
            view = (RomListItem)m_inflater.inflate(R.layout.rom_list_item, parent, false);
            view.initializeListeners(m_listener);
        } else {
            view = (RomListItem)convertView;
        }

        view.setRom(getItem(pos));
        return view;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int i) {
        if(i < 0 || i >= m_roms.size())
            throw new ArrayIndexOutOfBoundsException();
        return true;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver dataSetObserver) {
        m_observers.add(dataSetObserver);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver dataSetObserver) {
        m_observers.remove(dataSetObserver);
    }

    @Override
    public int getCount() {
        return m_roms.size();
    }

    @Override
    public Rom getItem(int i) {
        return m_roms.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public int getItemViewType(int i) {
        return 1;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return m_roms.isEmpty();
    }

    private ArrayList<Rom> m_roms = new ArrayList<Rom>();
    private Set<DataSetObserver> m_observers = new HashSet<DataSetObserver>();
    private LayoutInflater m_inflater;
    private RomListItem.OnRomActionListener m_listener;
}
