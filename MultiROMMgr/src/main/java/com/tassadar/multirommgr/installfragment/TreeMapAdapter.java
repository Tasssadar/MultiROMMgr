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

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class TreeMapAdapter<K, E> extends BaseAdapter {

    private static final int DEFAULT_ITEM_RES = android.R.layout.simple_spinner_dropdown_item;

    public interface NameResolver<K, E> {
        public CharSequence getName(K key, E entry);
    }

    public TreeMapAdapter(Context ctx, TreeMap<K, E> data, NameResolver<K, E> resolver) {
        m_data = data;
        m_nameResolver = resolver;
        m_inflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        m_itemRes = DEFAULT_ITEM_RES;
    }

    public TreeMapAdapter(Context ctx, TreeMap<K, E> data, NameResolver<K, E> resolver, int itemLayoutRes) {
        m_data = data;
        m_nameResolver = resolver;
        m_inflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        m_itemRes = itemLayoutRes;
    }

    @Override
    public int getCount() {
        return m_data.size();
    }

    @Override
    public E getItem(int pos) {
        Map.Entry<K, E> e = getEntry(pos);
        if(e != null) {
            return e.getValue();
        } else {
            return null;
        }
    }

    public Map.Entry<K, E> getEntry(int pos) {
        Iterator<Map.Entry<K, E>> itr = m_data.entrySet().iterator();
        while(itr.hasNext()) {
            Map.Entry<K, E> e = itr.next();
            if(pos-- == 0)
                return e;
        }
        return null;
    }

    @Override
    public long getItemId(int pos) {
        return pos;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        TextView text;

        if (convertView == null) {
            view = m_inflater.inflate(m_itemRes, parent, false);
        } else {
            view = convertView;
        }

        try {
            //  If no custom field is assigned, assume the whole resource is a TextView
            text = (TextView) view;
        } catch (ClassCastException e) {
            Log.e("ArrayAdapter", "You must supply a resource ID for a TextView");
            throw new IllegalStateException(
                    "ArrayAdapter requires the resource ID to be a TextView", e);
        }

        Map.Entry<K, E> e = getEntry(position);
        if(m_nameResolver != null) {
            text.setText(m_nameResolver.getName(e.getKey(), e.getValue()));
        } else {
            text.setText(e.getKey().toString());
        }
        return view;
    }

    private Map<K, E> m_data;
    private LayoutInflater m_inflater;
    private NameResolver<K, E> m_nameResolver;
    private int m_itemRes;
}
