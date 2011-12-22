package com.tassadar.multirommgr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class BackupsActivity extends ListActivity
{
    private static final String MULTIROM_BACK = "/multirom/backup/";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.backups);
        con = this;
        LoadBackups();
    }
    
    @Override
    protected void onListItemClick (ListView l, View v, int position, long id)
    {
        if(m_folder == null)
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
    
    public void ShowToast(String text)
    {
        Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        toast.show();
    }
    
    private void LoadBackups()
    {
        setProgressBarVisibility(true);
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
                        folder = mount_sp[i].split(" ")[1] + MULTIROM_BACK;
                        list = MultiROMMgrActivity.runRootCommand("ls " + folder);
                        if(list == null || list.contains("No such file or directory"))
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
                m_folder = folder;
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
            String res = MultiROMMgrActivity.runRootCommand("mv " + m_folder + m_f + " " + m_folder + m_t);
            m_backLoading.sendMessage(m_backLoading.obtainMessage(1, res != null ? 0 : 1, 0));
        }
    }
    
    private ListAdapter m_adapter;
    private AlertDialog m_renameDial;
    private List<HashMap<String, String>> m_fillMaps;
    private String m_folder;
    private Context con;
    private ProgressDialog m_loading;
}
