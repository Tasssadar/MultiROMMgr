package com.tassadar.multirommgr;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
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
    public static final String BASE = "/data/data/com.tassadar.multirommgr/";
    public static final String TAG = "MultiROMMgr";
    private static final String XDA = "http://forum.xda-developers.com/showthread.php?t=1304656";
    private static final String MD5[][] = new String[][]
    {
        {"4c02224801dc3ac3fc378a8bf1f2a301", "v1"},
        {"f31ac54fba207f4d67786e201dc3e6b1", "v2"},
        {"51b51b4db2d5e4852cecbc85aa2f48f5", "v3"},
        {"cad81d9e14cf8aa2c591f6e0007ce78b", "v4"},
        {"ce8e4ef6bc63dc9e1cfdda2e795bbf31", "v5"},
        {"75d4331e27a8430a7ae1a44447ae5cb7", "v6"},
        {"e54bd2dcd6b46abed92a4d086c114c60", "v7"},
        {"c77980e782029de9733c0223223919e3", "v8"},
        {"0df56fca2aafdade1228f657d5037fce", "v9"},
        {"e10a2cdb21c15ad9fdcef99fd5a37d6b", "v10"},
        {"c9d0aa68975a7a31b055e954e832472a", "v11"},
    };
    private static final int LOADING_ROOT = 1;
    private static final int LOADING_MR   = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        
        m_installed = true;
        
        setLoadingDialog(getResources().getString(R.string.check_root));
        CopyAssets();
        checkForRoot();
    }
    
    @Override
    protected void onListItemClick (ListView l, View v, int position, long id)
    {
        if(m_installed)
        {
            switch(position)
            {
                case 0: startActivity(new Intent(this, BMgrConf.class));        break;
                case 1: startActivity(new Intent(this, BackupsActivity.class)); break;
                case 2: 
                    Intent mktIntent = new Intent(Intent.ACTION_VIEW);
                    mktIntent.setData(Uri.parse(XDA));
                    startActivity(mktIntent);
                    break;
            }
        }
        else
        {
            switch(position)
            {
                case 0: 
                    Intent mktIntent = new Intent(Intent.ACTION_VIEW);
                    mktIntent.setData(Uri.parse(XDA));
                    startActivity(mktIntent);
                    break;
            }
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
            image = new int[] { R.drawable.ic_menu_preferences, R.drawable.rom_backup, R.drawable.rom_update };
            // set version
            title[2] = title[2] + " " + getVersion();
        }
        else
        {
            title = getResources().getStringArray(R.array.list_titles_not_in);
            summary = getResources().getStringArray(R.array.list_summaries_not_in);
            image = new int[] { R.drawable.rom_update };
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
    
    private String getVersion()
    {
        String res = runRootCommand("md5sum /init");
        if(res != null && res.contains("No such file or directory"))
            res = null;
        else
            res = res.split(" ")[0];
        
        if(res != null)
        {
            for(int i = 0; i < MD5.length; ++i)
                if(MD5[i][0].equals(res))
                    return MD5[i][1];
            res = getResources().getString(R.string.unknown_version);
        }
        return res;
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

    public static String runRootCommand(String command) {
        Process proc = null;
        OutputStreamWriter osw = null;
        StringBuilder sbstdOut = new StringBuilder();

        //Log.e(TAG, command);

        try {

            proc = Runtime.getRuntime().exec("su");
            osw = new OutputStreamWriter(proc.getOutputStream());
            osw.write(command);
            osw.flush();
            osw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        } finally {
            if (osw != null) {
                try {
                    osw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }
        try {
            if (proc != null)
                proc.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
        
        int read;
        try {
            while((read = proc.getInputStream().read()) != -1)
                sbstdOut.append((char)read);
        } catch (IOException e) { }
        
        String res = sbstdOut.toString();
        if(res.split("\n").length == 1)
            res = res.replaceAll("\n", "");
        proc.destroy();
        return res;
    }   
    
    // From ProxyDroid app
    private void CopyAssets() {
        AssetManager assetManager = getAssets();
        String[] files = null;
        try {
            files = assetManager.list("");
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        for (int i = 0; i < files.length; i++) {
            InputStream in = null;
            OutputStream out = null;
            try {

                in = assetManager.open(files[i]);
                out = new FileOutputStream(BASE + files[i]);
                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }

                in.close();
                in = null;
                out.flush();
                out.close();
                out = null;

            } catch (Exception e) {
              //  Log.e(TAG, e.getMessage());
            }
        }
    }

    private ListAdapter m_adapter;
    private ProgressDialog m_loading;
    private boolean m_installed;
}
