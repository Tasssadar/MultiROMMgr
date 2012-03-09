package com.tassadar.multirommgr;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;

public class BackupsActivity extends BackupsActivityBase
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        m_active_in_list = false;
        super.onCreate(savedInstanceState);
        getListView().setOnItemLongClickListener(m_longClick);
    }
    
    @Override
    public boolean onPrepareOptionsMenu (Menu menu)
    {
        return prepareMenu(menu);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        return prepareMenu(menu);
    }
    
    private boolean prepareMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        menu.clear();
        inflater.inflate(R.menu.backups_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        
        switch(item.getItemId())
        {
            case R.id.menu_reload:
                if(getListView().isEnabled())
                    LoadBackups();
                return true;
            case R.id.menu_move_act:
                if(getListView().isEnabled())
                    MoveActToBack();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onListItemClick (ListView l, View v, int position, long id)
    {
        if(m_folder_backups == null)
        {
            ShowToast(getResources().getString(R.string.backups_wait));
            return;
        }

        String name = (String) ((TextView)v.findViewById(R.id.title)).getText();

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.back_dialog, (ViewGroup)findViewById(R.id.back_dialog));
        ((TextView)layout.findViewById(R.id.text)).setText(name);
        ((TextView)layout.findViewById(R.id.text_orig)).setText(name);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(layout);
        builder.setNeutralButton(getResources().getString(R.string.rename), new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface arg0, int arg1)
            {
                String name_from = ((TextView)m_renameDial.findViewById(R.id.text_orig)).getText().toString();
                String name = ((TextView)m_renameDial.findViewById(R.id.text)).getText().toString();
                m_renameDial = null;
                if(name.equals("") || !name.matches("^[a-zA-Z0-9-_#|]+$"))
                {
                    ShowToast(getResources().getString(R.string.wrong_name));
                    return;
                }
                m_backLoading.sendEmptyMessage(2);
                RenameThread r = new RenameThread(name_from, name);
                r.start();
            }
        });
        builder.setTitle(getResources().getString(R.string.rename_back));
        builder.setCancelable(true);
       
        m_renameDial = builder.create();
        m_renameDial.show();
    }
    
    private final OnItemLongClickListener m_longClick = new OnItemLongClickListener()
    {
        @Override
        public boolean onItemLongClick(AdapterView<?> arg0, View v,
                int pos, long id) {
            
            m_selectedBackup = ((TextView)v.findViewById(R.id.title)).getText().toString();
            final CharSequence[] items = getResources().getStringArray(R.array.backup_options);
            
            AlertDialog.Builder builder = new AlertDialog.Builder(con);
            builder.setTitle(getResources().getString(R.string.select));
            builder.setItems(items, m_onOptionsClick);
            AlertDialog alert = builder.create();
            alert.show();
            return false;
        }
        
    };
    
    private final OnClickListener m_onOptionsClick = new OnClickListener()
    {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch(which)
            {
                case 0:
                    SwitchWithActive();
                    break;
                case 1:
                    Erase();
                    break;
            }
        }
    };

    private void MoveActToBack()
    {
        if(!m_activePresent)
        {
            ShowToast(getResources().getString(R.string.no_active));
            return;
        }
        ShowLoading(getResources().getString(R.string.working));
        new Thread(new Runnable() {
            public void run() {
                String res =
                        MultiROMMgrActivity.runRootCommand("mv " + m_folder_main + " " + m_folder_backups + getNewBackName());
                m_backLoading.sendMessage(m_backLoading.obtainMessage(4, res != null && res.equals("") ? 0 : -1, 0));
            }
        }).start();
    }
    
    private void Erase()
    {
        ShowLoading(getResources().getString(R.string.working));
        new Thread(new Runnable() {
            public void run() {
                String res = MultiROMMgrActivity.runRootCommand("rm -r " + m_folder_backups + m_selectedBackup);
                m_backLoading.sendMessage(m_backLoading.obtainMessage(3, res != null && res.equals("") ? 1 : 0, 0));
            }
        }).start();
    }

    private void SwitchWithActive()
    {
        if(!m_activePresent)
            ShowToast(getResources().getString(R.string.main_not_found));

        ShowLoading(getResources().getString(R.string.working));
        new Thread(new Runnable() {
            public void run() {
                String res;
                if(m_activePresent)
                {
                    res = MultiROMMgrActivity.runRootCommand(
                             "mv " + m_folder_main + " " + m_folder_backups + getNewBackName());
                    if(res == null || !res.equals(""))
                    {
                        send(-1);
                        return;
                    }
                }
                else
                {
                    res = MultiROMMgrActivity.runRootCommand("rm -r " + m_folder_main);
                    if(res == null || (!res.equals("") && !res.contains("No such file")))
                    {
                        send(-3);
                        return;
                    }
                }
                
                res = MultiROMMgrActivity.runRootCommand(
                        "mv " + m_folder_backups + m_selectedBackup + " " + m_folder_main);
                if(res == null || !res.equals(""))
                {
                    send(-2);
                    return;
                }
                send(0);
            }
            private void send(int res)
            {
                m_backLoading.sendMessage(m_backLoading.obtainMessage(4, res, 0));
            }
        }).start();
    }
    
    private class RenameThread extends Thread
    {
        private String m_f;
        private String m_t;

        public RenameThread(String from, String to)
        {
            m_f = from;
            m_t = to;
        }
        
        public void run()
        {
            String res = MultiROMMgrActivity.runRootCommand("mv " + m_folder_backups + m_f + " " + m_folder_backups + m_t);
            m_backLoading.sendMessage(m_backLoading.obtainMessage(1, res != null && res.equals("") ? 1 : 0, 0));
        }
    }

    private AlertDialog m_renameDial;
}
