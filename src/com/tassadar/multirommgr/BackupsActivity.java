package com.tassadar.multirommgr;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class BackupsActivity extends ListActivity
{
    private static final String MULTIROM_BACK = "/multirom/backup/";
    private static final String MULTIROM_MAIN = "/multirom/rom";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.backups);
        con = this;
        getListView().setOnItemLongClickListener(m_longClick);
        LoadBackups();
    }
    
    @Override
    public boolean onPrepareOptionsMenu (Menu menu)
    {
        prepareMenu(menu);
        return true;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        prepareMenu(menu);
        return true;
    }
    
    private void prepareMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        menu.clear();
        inflater.inflate(R.menu.backups_menu, menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        
        switch(item.getItemId())
        {
            case R.id.menu_reload:
                if(getListView().isEnabled())
                    LoadBackups();
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
    
    OnItemLongClickListener m_longClick = new OnItemLongClickListener()
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
    
    OnClickListener m_onOptionsClick = new OnClickListener()
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
    
    public void ShowToast(String text)
    {
        Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        toast.show();
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
    
    private String fixLen(String s, int len)
    {
        if(s.length() == len)
            return s;
        while(s.length() != len)
            s = "0" + s;
        return s;
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
                    Calendar c = Calendar.getInstance();
                    String date = String.valueOf(c.get(Calendar.YEAR));
                    date += fixLen(String.valueOf(c.get(Calendar.MONTH)), 2);
                    date += fixLen(String.valueOf(c.get(Calendar.DATE)), 2) + "-";
                    date += fixLen(String.valueOf(c.get(Calendar.HOUR_OF_DAY)), 2);
                    date += fixLen(String.valueOf(c.get(Calendar.MINUTE)), 2);
                    
                    res = MultiROMMgrActivity.runRootCommand(
                             "mv " + m_folder_main + " " + m_folder_backups + "rom_" + date);
                    if(res == null || !res.equals(""))
                    {
                        send(-1);
                        return;
                    }
                    
                }
                else
                {
                    res = MultiROMMgrActivity.runRootCommand("rm -r " + m_folder_main);
                    if(res == null || !res.equals(""))
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
    
    private void LoadBackups()
    {
        setProgressBarVisibility(true);
        setProgressBarIndeterminateVisibility(true);
        getListView().setEnabled(false);
        getListView().setClickable(false);
        
        new Thread(new Runnable() {
            public void run() {
                String folder = null;
                String list = null;
                String mount = MultiROMMgrActivity.runRootCommand("mount");
                if(mount == null)
                    return;

                String mount_sp[] = mount.split("\n");
                for(int i = 0; i < mount_sp.length; ++i)
                {
                    if(mount_sp[i].startsWith(("/dev/block/mmcblk0p2")) || mount_sp[i].contains("/sd-ext"))
                    {
                        // Check main
                        m_folder_main = mount_sp[i].split(" ")[1] + MULTIROM_MAIN;
                        list = MultiROMMgrActivity.runRootCommand("ls " + m_folder_main);
                        if(list == null || list.equals("") || list.contains("No such file or directory"))
                            m_activePresent = false;
                        else
                            m_activePresent = true;
                        
                        
                        // Check backups
                        folder = mount_sp[i].split(" ")[1] + MULTIROM_BACK;
                        list = MultiROMMgrActivity.runRootCommand("ls " + folder);
                        if(list == null || list.equals("") || list.contains("No such file or directory"))
                            folder = null;
                        break;
                    }
                }
                
                if(folder == null)
                {
                    ShowToast(getResources().getString(R.string.error_folder));
                    return;
                }
                
                if(list.equals(""))
                {
                    ShowToast(getResources().getString(R.string.error_backup_empty));
                    return;
                }

                String list_sp[] = list.split("\n");
                m_fillMaps = new ArrayList<HashMap<String, String>>();
                for(int i = 0; i < list_sp.length; ++i)
                {
                    String sum = MultiROMMgrActivity.runRootCommand("du -h -d0 " + folder + list_sp[i]);
                    if(sum == null)
                        sum = getResources().getString(R.string.size) + " N/A M";
                    else
                        sum = getResources().getString(R.string.size) + " " + sum.split("\t")[0];
                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put("title", list_sp[i]);
                    map.put("summary", sum);
                    m_fillMaps.add(map);
                    m_backLoading.sendMessage(m_backLoading.obtainMessage(0, ((i+1)*10000/list_sp.length), 0));
                }
                m_folder_backups = folder;
            }
        }).start();
    }
    
    private void ShowLoading(String text)
    {
        if(text == null)
        {
            if(m_loading != null)
            {
                m_loading.dismiss();
                m_loading = null;
            }
            return;    
        }
        
        if(m_loading == null)
        {
            m_loading = new ProgressDialog(this);
            m_loading.setCancelable(true);
            m_loading.setOnCancelListener(new Dialog.OnCancelListener()
            {
                public void onCancel(DialogInterface dia)
                {
                    finish();
                }
            });
            m_loading.setProgressStyle(ProgressDialog.STYLE_SPINNER); 
        }
        m_loading.setMessage(text);
        m_loading.show();
    }
    
    private Handler m_backLoading = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch(msg.what)
            {
                case 0:
                {
                    String[] from = new String[] { "title", "summary" };
                    int[] to = new int[] { R.id.title, R.id.summary };
                    m_adapter = new SimpleAdapter(con, m_fillMaps, R.layout.backups_item, from, to); 
                    setListAdapter(m_adapter);
                    if(msg.arg1 == 10000)
                    {
                        getListView().setEnabled(true);
                        getListView().setClickable(true);
                        setProgressBarIndeterminateVisibility(false);
                    }
                    setProgress(msg.arg1);
                    break;
                }
                case 1:
                    if(msg.arg1 == 1)
                        ShowToast(getResources().getString(R.string.renamed));
                    else
                        ShowToast(getResources().getString(R.string.renamed_error));
                    ShowLoading(null);
                    LoadBackups();
                    break;
                case 2:
                    ShowLoading(getResources().getString(R.string.working));
                    break;
                case 3:
                    if(msg.arg1 == 1)
                        ShowToast(getResources().getString(R.string.erased));
                    else
                        ShowToast(getResources().getString(R.string.erased_error));
                    ShowLoading(null);
                    LoadBackups();
                    break;
                case 4:
                    String text = null;
                    switch(msg.arg1)
                    {
                        case 0:  text = getResources().getString(R.string.switched); break;
                        case -1: text = getResources().getString(R.string.switched_e_move); break;
                        case -2: text = getResources().getString(R.string.switched_e_move2); break;
                        case -3: text = getResources().getString(R.string.switched_e_erase); break;
                    }
                    ShowToast(text);
                    ShowLoading(null);
                    LoadBackups();
                    break;
            }
        }
    };
    
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
    
    private ListAdapter m_adapter;
    private AlertDialog m_renameDial;
    private List<HashMap<String, String>> m_fillMaps;
    private String m_folder_backups;
    private String m_folder_main;
    private Context con;
    private ProgressDialog m_loading;
    private String m_selectedBackup;
    private boolean m_activePresent;
}
