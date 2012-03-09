package com.tassadar.multirommgr;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

public class BackupsSelectActivity extends BackupsActivityBase
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        m_active_in_list = true;
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onListItemClick (ListView l, View v, int position, long id)
    {
        if(m_folder_backups == null)
        {
            ShowToast(getResources().getString(R.string.backups_wait));
            return;
        }
        
        String name = "";
        if(position != 0)
            name = (String) ((TextView)v.findViewById(R.id.title)).getText();
        
        Intent data = new Intent();
        data.putExtra("name", "\"" + name + "\"");
        
        setResult(RESULT_OK, data);
        finish();
    }
}
