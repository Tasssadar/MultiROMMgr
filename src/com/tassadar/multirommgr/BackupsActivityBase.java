package com.tassadar.multirommgr;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Window;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class BackupsActivityBase extends ListActivity
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
        m_rom_list = new String[0];
        LoadBackups();
    }
    
    public void ShowToast(String text)
    {
        Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        toast.show();
    }
    
    protected String fixLen(String s, int len)
    {
        if(s.length() == len)
            return s;
        while(s.length() != len)
            s = "0" + s;
        return s;
    }
    
    protected boolean ROMExists(String name)
    {
        for(int i = 0; i < m_rom_list.length; ++i)
            if(name.equals(m_rom_list[i]))
                return true;
        return false;
    }

    protected String getNewBackName()
    {
        Calendar c = Calendar.getInstance();
        String date = String.valueOf(c.get(Calendar.YEAR));
        date += fixLen(String.valueOf(c.get(Calendar.MONTH)+1), 2);
        date += fixLen(String.valueOf(c.get(Calendar.DATE)), 2) + "-";
        date += fixLen(String.valueOf(c.get(Calendar.HOUR_OF_DAY)), 2);
        date += fixLen(String.valueOf(c.get(Calendar.MINUTE)), 2);
        date = "rom_" + date;
        if(!ROMExists(date))
            return date;
        
        for(int i = 1; true; ++i)
        {
            if(!ROMExists(date + "-" + i))
                return date + "-" + i;
        }
    }
    
    protected String[] CheckSDPath(String path)
    {
        if(path == null)
            return new String[] { null, null };

        String folder = null;
        String list = null;
        // Check main
        m_folder_main = path + MULTIROM_MAIN;
        list = MultiROMMgrActivity.runRootCommand("ls " + m_folder_main);
        if(list == null || list.equals("") || list.contains("No such file or directory"))
            m_activePresent = false;
        else
            m_activePresent = true;

        // Check backups
        folder = path + MULTIROM_BACK;
        list = MultiROMMgrActivity.runRootCommand("ls " + folder);
        if(list == null || list.contains("No such file or directory"))
            folder = null;
        return new String[]{folder, list };
    }
    
    protected void LoadBackups()
    {
        setProgressBarVisibility(true);
        setProgressBarIndeterminateVisibility(true);
        getListView().setEnabled(false);
        getListView().setClickable(false);
        
        new Thread(new Runnable() {
            public void run() {
                String folder = null;
                String list = null;
      
                String s[] = CheckSDPath(Storage.getSDExt());
                folder = s[0];
                list = s[1];
                
                if(folder == null)
                {
                    m_backLoading.sendMessage(m_backLoading.obtainMessage(5, 0, 0));
                    return;
                }
                
                m_folder_backups = folder;
                m_fillMaps = new ArrayList<HashMap<String, String>>();
                
                if(m_active_in_list)
                {
                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put("title", getResources().getString(R.string.default_boot_sd_active));
                    map.put("summary", "");
                    m_fillMaps.add(0, map);
                }

                if(list == null || list.equals(""))
                {
                    m_backLoading.sendMessage(m_backLoading.obtainMessage(5, 1, 0));
                    return;
                }

                m_rom_list = list.split("\n");

                for(int i = 0; i < m_rom_list.length; ++i)
                {
                    String sum = MultiROMMgrActivity.runRootCommand("du -h -d0 " + folder + m_rom_list[i]);
                    if(sum == null)
                        sum = getResources().getString(R.string.size) + " N/A M";
                    else
                        sum = getResources().getString(R.string.size) + " " + sum.split("\t")[0];
                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put("title", m_rom_list[i]);
                    map.put("summary", sum);
                    m_fillMaps.add(map);
                    m_backLoading.sendMessage(m_backLoading.obtainMessage(0, ((i+1)*10000/m_rom_list.length), 0));
                }
            }
        }).start();
    }
    
    protected void ShowLoading(String text)
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
    
    protected void updateAdapter()
    {
        final String[] from = new String[] { "title", "summary" };
        final int[] to = new int[] { R.id.title, R.id.summary };

        ListAdapter a = new SimpleAdapter(con, m_fillMaps, R.layout.backups_item, from, to);
        setListAdapter(a);
    }

    protected final Handler m_backLoading = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch(msg.what)
            {
                case 0:
                {
                    updateAdapter();
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
                {
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
                case 5:
                {
                    String text = null;
                    switch(msg.arg1)
                    {
                        case 0: text = getResources().getString(R.string.error_folder); break;
                        case 1:
                            text = getResources().getString(R.string.error_backup_empty);
                            updateAdapter();
                            break;
                    }
                    ShowToast(text);
                    setProgressBarVisibility(false);
                    setProgressBarIndeterminateVisibility(false);
                    getListView().setEnabled(true);
                    getListView().setClickable(true);
                    break;
                }
            }
        }
    };
    
    protected boolean m_active_in_list;
    protected List<HashMap<String, String>> m_fillMaps;
    protected String m_folder_backups;
    protected String m_folder_main;
    protected Context con;
    protected ProgressDialog m_loading;
    protected String m_selectedBackup;
    protected boolean m_activePresent;
    protected String m_rom_list[];
}
