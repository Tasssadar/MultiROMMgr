package com.tassadar.multirommgr.romlistfragment;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

import com.tassadar.multirommgr.R;

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

    public void set(ArrayList<String> roms) {
        m_roms = new ArrayList<String>(roms);
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
    public String getItem(int i) {
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

    private ArrayList<String> m_roms = new ArrayList<String>();
    private Set<DataSetObserver> m_observers = new HashSet<DataSetObserver>();
    private LayoutInflater m_inflater;
    private RomListItem.OnRomActionListener m_listener;
}
