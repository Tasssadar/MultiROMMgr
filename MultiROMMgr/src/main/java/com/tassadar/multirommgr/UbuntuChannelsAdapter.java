package com.tassadar.multirommgr;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.Iterator;
import java.util.Map;

public class UbuntuChannelsAdapter extends BaseAdapter {

    private static final int ITEM_RES = android.R.layout.simple_spinner_dropdown_item;

    public UbuntuChannelsAdapter(Context ctx, Map<String, UbuntuChannel> channels) {
        m_channels = channels;
        m_inflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return m_channels.size();
    }

    @Override
    public UbuntuChannel getItem(int pos) {
        Iterator<Map.Entry<String, UbuntuChannel>> itr = m_channels.entrySet().iterator();
        while(itr.hasNext()) {
            Map.Entry<String, UbuntuChannel> e = itr.next();
            if(pos-- == 0)
                return e.getValue();
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
            view = m_inflater.inflate(ITEM_RES, parent, false);
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

        UbuntuChannel c = getItem(position);
        text.setText(c.getDisplayName());
        return view;
    }

    private Map<String, UbuntuChannel> m_channels;
    private LayoutInflater m_inflater;
}
