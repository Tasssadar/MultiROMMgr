package com.tassadar.multirommgr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;

public class MultiROMMgrActivity extends ListActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        String[] title = getResources().getStringArray(R.array.list_titles);
        String[] summary = getResources().getStringArray(R.array.list_summaries);
        int[] image = new int[] {R.drawable.ic_menu_preferences, R.drawable.rom_backup };
        String[] from = new String[] { "image", "title", "summary" };
        int[] to = new int[] { R.id.image, R.id.title, R.id.summary };
        

        List<HashMap<String, String>> fillMaps = new ArrayList<HashMap<String, String>>();
        for(int i = 0; i < title.length; i++){
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("image", String.valueOf(image[i]));
            map.put("title", title[i]);
            map.put("summary", summary[i]);
            fillMaps.add(map);
        }
        
        m_adapter = new SimpleAdapter(this, fillMaps, R.layout.list_item, from, to); 

        // Bind to our new adapter.
        setListAdapter(m_adapter);
    }
    
    private ListAdapter m_adapter;
}
