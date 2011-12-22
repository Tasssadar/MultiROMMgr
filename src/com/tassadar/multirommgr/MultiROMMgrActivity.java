package com.tassadar.multirommgr;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class MultiROMMgrActivity extends ListActivity
{
    private static final String TAG = "MultiROMMgr";
    private static final int LOADING_ROOT = 1;
    private static final int LOADING_MR   = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        
        m_installed = true;
        
        setLoadingDialog(getResources().getString(R.string.check_root));
        checkForRoot();
    }
    
    @Override
    protected void onListItemClick (ListView l, View v, int position, long id)
    {
        switch(position)
        {
            case 0: startActivity(new Intent(this, BMgrConf.class));        break;
            case 1: startActivity(new Intent(this, BackupsActivity.class)); break;
        }
    }
    
    private void checkForRoot()
    {
        new Thread(new Runnable() {
            public void run() {
                String rootTest = runRootCommand("echo test");
                m_loadingHandler.sendMessage(
                        m_loadingHandler.obtainMessage(LOADING_ROOT,
                                (rootTest == null || !rootTest.equals("test")) ? 0 : 1, 0));
                
            }
          }).start();
    }
    
    private void checkForMultiROM()
    {
        new Thread(new Runnable() {
            public void run() {
                String[] files = new String[] { "main_init", "preinit.rc" };
                for(int i = 0; i < files.length; ++i)
                {
                    String res = runRootCommand("ls / | grep " + files[i]);
                    if(res == null || !res.equals(files[i]))
                    {
                        send(0);
                        return;
                    }
                }  
                send(1);
            }
            private void send(int res)
            {
                m_loadingHandler.sendMessage(
                        m_loadingHandler.obtainMessage(LOADING_MR, res, 0));
            }
          }).start();
    }
    
    private void setLoadingDialog(String text)
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
    
    private void ShowErrorAndExit(String text)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(text)
               .setTitle(getResources().getString(R.string.error))
               .setPositiveButton(getResources().getString(R.string.exit),
                                   new DialogInterface.OnClickListener()
               {
                   public void onClick(DialogInterface dialog, int id)
                   {
                       dialog.dismiss();
                       finish();
                   }
               });
        AlertDialog alert = builder.create();
        alert.show();
    }
    
    public void ShowToast(String text)
    {
        Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        toast.show();
    }
    
    private void ChangeInstalledStatus(boolean status)
    {
        m_installed = status;
        String[] title = null;
        String[] summary = null;
        int[] image = null; 
        String[] from = new String[] { "image", "title", "summary" };
        if(status)
        {
            title = getResources().getStringArray(R.array.list_titles_installed);
            summary = getResources().getStringArray(R.array.list_summaries_installed);
            image = new int[] {R.drawable.ic_menu_preferences, R.drawable.rom_backup };
        }
        else
        {
            title = new String[] {};
            summary = new String[] {};
            image = new int[] {};
        }
            
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

        setListAdapter(m_adapter);
    }
    
    private final Handler m_loadingHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch(msg.what)
            {
                case LOADING_ROOT:
                {
                    if(msg.arg1 == 1)
                    {
                        setLoadingDialog(getResources().getString(R.string.check_multirom));
                        checkForMultiROM();
                    }
                    else
                    {
                        setLoadingDialog(null);
                        ShowErrorAndExit(getResources().getString(R.string.root_error));
                    }
                    break;
                }
                case LOADING_MR:
                {
                    setLoadingDialog(null);
                    ChangeInstalledStatus(msg.arg1 == 1);
                    if(msg.arg1 == 0)
                        ShowToast(getResources().getString(R.string.mr_error));
                    break;
                }
            }
        }
    };
    
    // From SuperUser app
    public static String runRootCommand(String command)
    {
        String inLine = null;
        Process process;
        try {
            process = Runtime.getRuntime().exec("su");
        } catch (IOException e1) {
            Log.e(TAG, e1.getMessage());
            return inLine;
        }
        DataOutputStream os = new DataOutputStream(process.getOutputStream());
        BufferedReader is = new BufferedReader(new InputStreamReader(
                new DataInputStream(process.getInputStream())), 64);
        try {
            inLine = executeCommand(os, is, 1000, command);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        try { os.writeBytes("exit\n"); }
        catch (IOException e) { Log.e(TAG, e.getMessage()); }
        return inLine;
    }
    
    private static String executeCommand(DataOutputStream os, BufferedReader is, int timeout,
            String... commands) throws IOException
    {
        if (commands.length == 0) return null;
        StringBuilder command = new StringBuilder();
        for (String s : commands) {
            command.append(s).append(" ");
        }
        command.append("\n");
        Log.d(TAG, command.toString());
        os.writeBytes(command.toString());
        if (is != null) {
            for (int i = 0; i < timeout; i++) {
                if (is.ready()) break;
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    Log.w(TAG, "Sleep timer interrupted", e);
                }
            }
            if (is.ready()) {
                String res = "";
                int count = 0;
                while(is.ready())
                {
                    String tmp = null;
                    try {
                        tmp = is.readLine();
                    }
                    catch(IOException e) {    
                    }
                    if(tmp == null)
                        break;
                    res += tmp + "\n";
                    ++count;
                }
                if(count == 1)
                    res = res.replaceAll("\n", "");
                return res;                
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
    
    private ListAdapter m_adapter;
    private ProgressDialog m_loading;
    private boolean m_installed;
}
