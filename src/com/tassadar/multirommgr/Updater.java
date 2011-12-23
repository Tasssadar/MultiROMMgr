package com.tassadar.multirommgr;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.util.ByteArrayBuffer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class Updater extends Activity
{
    private static final String LINK_PACKAGE = "http://dl.dropbox.com/u/54372958/multirom.zip";
    private static final String LINK_MANIFEST = "http://dl.dropbox.com/u/54372958/multirom_man.txt";
    private static final String LINK_VERSIONS = "http://dl.dropbox.com/u/54372958/multirom_ver.txt";
    private static final String LINK_RECOVERY[] = new String[]
    {
        "http://dl.dropbox.com/u/54372958/recovery_am.img",
        "http://dl.dropbox.com/u/54372958/recovery_cwm.img",
        null
    };
    private static final String UPDATE_PACKAGE = "multirom.zip";
    private static final String DOWNLOAD_LOC = MultiROMMgrActivity.BASE;
    private static final String UPDATE_FOLDER = MultiROMMgrActivity.BASE + "multirom/";
    private static final String RECOVERY_LOC = MultiROMMgrActivity.BASE + "recovery.img";
    private static final String RECOVERY_OLD_LOC = MultiROMMgrActivity.BASE + "recovery_old.img";
    private static final int PACKAGE_SIZE = 1024*1024; 
    private static final int RECOVERY_SIZE = 8*1024*1024; 

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.update);
        ((Button)findViewById(R.id.update_button)).setOnClickListener(update_button_l);
        Btn(false);
        m_updating = false;
        con = this;
        GetVersion();
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if(m_updating && keyCode == KeyEvent.KEYCODE_BACK)
        {
            Toast.makeText(this, getResources().getString(R.string.update_wait), Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    private void SetStatus(String text)
    {
        ((TextView)findViewById(R.id.status_text)).append(text + "\n");
    }
    
    private void Btn(boolean enable)
    {
        Button b = (Button)findViewById(R.id.update_button);
        b.setEnabled(enable);
        b.setClickable(enable);
    }
    
    private OnClickListener update_button_l = new OnClickListener()
    {
        @Override
        public void onClick(View v) {
            Btn(false);
            TryUpdate();
        }
    };
    
    private void GetVersion()
    {
        setProgressBarIndeterminateVisibility(true);
        new Thread(new Runnable() {
            public void run() {
                //Manifest
                m_status.sendEmptyMessage(0);
                if(!DownloadVersions() || !DownloadManifest())
                {
                    m_status.sendEmptyMessage(2);
                    return;
                }
                m_status.sendEmptyMessage(9);
            }
        }).start();
    }
    
    private void RemoveSH(String path)
    {
        File f= new File(path);
        
        try {
            BufferedReader br = new BufferedReader(new FileReader(f));
            br.readLine();
            String file = "";
            String line;
            while((line = br.readLine()) != null)
                file += line + "\n";
            br.close();
            
            file = file.replace("!/sbin/sh >", "");
            MultiROMMgrActivity.runRootCommand("rm " + path);
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            bw.write(file);
            bw.close();
            MultiROMMgrActivity.runRootCommand("chmod 777 " + path);
        } catch (FileNotFoundException e) {} catch (IOException e) {}
    }
    
    private void TryUpdate()
    {
        m_updating = true;
        setProgressBarIndeterminateVisibility(true);
        m_recovery = -1;
        
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        m_lock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "MultiROM updater WakeLock");
        m_lock.acquire();
       
        new Thread(new Runnable() {
            public void run() {
                //Package
                m_status.sendEmptyMessage(1);
                if(!DownloadPackage())
                {
                    m_status.sendEmptyMessage(3);
                    return;
                }
                //Decompress
                m_status.sendEmptyMessage(4);
                Decompress d = new Decompress(DOWNLOAD_LOC + UPDATE_PACKAGE, UPDATE_FOLDER); 
                if(!d.unzip())
                {
                	m_status.sendEmptyMessage(16);
                	CleanUp();
                	return;
                }
                
                m_status.sendEmptyMessage(8);
                // mount / as rw to create tmp
                MultiROMMgrActivity.runRootCommand("mount -o remount,rw rootfs /");
                String res = MultiROMMgrActivity.runRootCommand("mkdir /tmp");
                if(res == null || (!res.equals("") && !res.contains("exists")))
                {
                    m_status.sendEmptyMessage(6);
                    CleanUp();
                    return;
                }

                MultiROMMgrActivity.runRootCommand("mount -o bind " + UPDATE_FOLDER + "kernel /tmp" );
                MultiROMMgrActivity.runRootCommand("chmod 777 /tmp");
                MultiROMMgrActivity.runRootCommand("chmod 777 /tmp/dump_image");
                MultiROMMgrActivity.runRootCommand("chmod 777 /tmp/mkbootimg.sh");
                MultiROMMgrActivity.runRootCommand("chmod 777 /tmp/mkbootimg");
                MultiROMMgrActivity.runRootCommand("chmod 777 /tmp/unpackbootimg");
                MultiROMMgrActivity.runRootCommand("chmod 777 /tmp/edit_ramdisk.sh");
                MultiROMMgrActivity.runRootCommand("chmod 777 /tmp/busybox_arch");
                MultiROMMgrActivity.runRootCommand("chmod 777 " + MultiROMMgrActivity.BASE + "dump_image");
                MultiROMMgrActivity.runRootCommand("chmod 777 " + MultiROMMgrActivity.BASE + "flash_image");
                
                RemoveSH("/tmp/mkbootimg.sh");
                RemoveSH("/tmp/edit_ramdisk.sh");
                
                MultiROMMgrActivity.runRootCommand("chown -R root /tmp/ramdisk/*");
                MultiROMMgrActivity.runRootCommand("cp /tmp/ramdisk/init /init");
                MultiROMMgrActivity.runRootCommand("touch /main_init");
                MultiROMMgrActivity.runRootCommand("touch /preinit.rc");
                MultiROMMgrActivity.runRootCommand("/tmp/dump_image boot /tmp/boot.img");
                MultiROMMgrActivity.runRootCommand("/tmp/unpackbootimg /tmp/boot.img /tmp/");
                MultiROMMgrActivity.runRootCommand("/tmp/edit_ramdisk.sh");
                MultiROMMgrActivity.runRootCommand("/tmp/mkbootimg.sh");
                
                res = MultiROMMgrActivity.runRootCommand(MultiROMMgrActivity.BASE + "flash_image boot /tmp/newboot.img");
                if(res == null || !res.equals(""))
                {
                    MultiROMMgrActivity.runRootCommand(MultiROMMgrActivity.BASE + "flash_image boot /tmp/boot.img");
                    CleanUp();
                    m_status.sendEmptyMessage(7);
                    return;
                }
                CleanUp();
                
                // Exit if multirom was installed
                if(MultiROMMgrActivity.isInstalled())
                {
                    m_status.sendEmptyMessage(5);
                    return;
                }

                // Recovery
                m_status.sendEmptyMessage(10);
                while(m_recovery == -1)
                {
                    try { Thread.sleep(300); } catch (InterruptedException e) { }
                }
                
                if(LINK_RECOVERY[m_recovery] == null)
                {
                    m_status.sendEmptyMessage(5);
                    return;
                }
                m_status.sendEmptyMessage(12);
                if(!DownloadFile(LINK_RECOVERY[m_recovery], RECOVERY_LOC, RECOVERY_SIZE))
                {
                    m_status.sendEmptyMessage(11);
                    return;
                }
                m_status.sendEmptyMessage(13);
                MultiROMMgrActivity.runRootCommand(MultiROMMgrActivity.BASE + "dump_image recovery " + RECOVERY_OLD_LOC);
                res = MultiROMMgrActivity.runRootCommand(MultiROMMgrActivity.BASE + "flash_image recovery " + RECOVERY_LOC);
                if(res == null || !res.equals(""))
                {
                    MultiROMMgrActivity.runRootCommand(MultiROMMgrActivity.BASE + "flash_image recovery " + RECOVERY_OLD_LOC);
                    m_status.sendEmptyMessage(14);
                    CleanRecovery();
                    return;
                }
                CleanRecovery();
                m_status.sendEmptyMessage(5);
            }

            private void CleanUp()
            {
                MultiROMMgrActivity.runRootCommand("umount /tmp");
                MultiROMMgrActivity.runRootCommand("rm -r /tmp");
                MultiROMMgrActivity.runRootCommand("mount -o remount,ro rootfs /");
                MultiROMMgrActivity.runRootCommand("rm " + DOWNLOAD_LOC + UPDATE_PACKAGE);
                MultiROMMgrActivity.runRootCommand("rm -r " + UPDATE_FOLDER);
            }

            private void CleanRecovery()
            {
                MultiROMMgrActivity.runRootCommand("rm " + RECOVERY_LOC);
                MultiROMMgrActivity.runRootCommand("rm " + RECOVERY_OLD_LOC);
            }
        }).start();
    }
    
    private boolean DownloadManifest()
    {
        String manifest = "";
        try {
            URL address = new URL(LINK_MANIFEST);
            URLConnection conn = address.openConnection();
            InputStream is = conn.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            int cur = 0;
            while((cur = bis.read()) != -1)
                manifest += String.valueOf((char)cur);
        }
        catch(IOException e) {
            return false;
        }
        manifest = manifest.replaceAll("\r", "");
        manifest = manifest.replaceAll(" ", "");
        String split[] = manifest.split("\n");
        if(split.length < 2)
            return false;
        manifest_version = split[0];
        manifest_md5 = split[1];
        return true;
    }
    
    private boolean DownloadPackage()
    {
        if(!DownloadFile(LINK_PACKAGE, DOWNLOAD_LOC + UPDATE_PACKAGE, PACKAGE_SIZE))
            return false;

        String md5 = MultiROMMgrActivity.runRootCommand("md5sum " + DOWNLOAD_LOC + UPDATE_PACKAGE);
        String md5Sp[] = null;
        if(md5 == null || md5.contains("No such file") || (md5Sp = md5.split(" ")) == null)
        {
            MultiROMMgrActivity.runRootCommand("rm " + DOWNLOAD_LOC + UPDATE_PACKAGE);
            return false;
        }
        if(!manifest_md5.equals(md5Sp[0]))
            return false;
        return true;
    }
    
    private boolean DownloadVersions()
    {
        return DownloadFile(LINK_VERSIONS, DOWNLOAD_LOC + UPDATE_PACKAGE, 1024*10);
    }
    
    private boolean DownloadFile(String url, String target, int size)
    {
        try {
            URL address = new URL(url);
            URLConnection conn = address.openConnection();
            InputStream is = conn.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            ByteArrayBuffer baf = new ByteArrayBuffer(size);
            int cur = 0;
            while((cur = bis.read()) != -1)
                baf.append((byte)cur);

            FileOutputStream fos = new FileOutputStream(target);
            fos.write(baf.toByteArray());
            fos.close();
        }
        catch(IOException e) {
            return false;
        }
        return true;
    }
    
    private android.content.DialogInterface.OnClickListener m_onRecoverySelect = new android.content.DialogInterface.OnClickListener()
    {
        @Override
        public void onClick(DialogInterface arg0, int arg1) {
            m_recovery = (byte)arg1;
        }
    };

    private Handler m_status = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            boolean off = true;
            switch(msg.what)
            {
                case 0:  SetStatus(getResources().getString(R.string.down_manifest)); off=false; break;
                case 1:  SetStatus(getResources().getString(R.string.down_pack));     off=false; break;
                case 4:  SetStatus(getResources().getString(R.string.decompress));    off=false; break;
                case 8:  SetStatus(getResources().getString(R.string.exec_package));  off=false; break;
                case 12: SetStatus(getResources().getString(R.string.rec_down));      off=false; break;
                case 13: SetStatus(getResources().getString(R.string.rec_flash));     off=false; break;
            
                case 2:  SetStatus(getResources().getString(R.string.down_manifest_error)); break;
                case 3:  SetStatus(getResources().getString(R.string.down_pack_error));     break;
                case 6:  SetStatus(getResources().getString(R.string.mktmp_fail));          break;
                case 7:  SetStatus(getResources().getString(R.string.flash_fail));          break;
                case 5:  SetStatus(getResources().getString(R.string.update_complete));     break;
                case 11: SetStatus(getResources().getString(R.string.rec_down_err));        break;
                case 14: SetStatus(getResources().getString(R.string.rec_flash_err));       break;
                case 15: SetStatus(getResources().getString(R.string.reboot));              break;
                case 16: SetStatus(getResources().getString(R.string.unzip_error));         break;
                
                case 9:
                {
                    String text = getResources().getString(R.string.current_version);
                    text += " " + MultiROMMgrActivity.getMRVersion() + "\n";
                    text += getResources().getString(R.string.newest_version);
                    text += " " + manifest_version + "\n";
                    if(manifest_version.equals(MultiROMMgrActivity.getMRVersion()))
                        text += getResources().getString(R.string.no_update);
                    else
                    {
                        Btn(true);
                        text += getResources().getString(R.string.press_update);
                    }
                    SetStatus(text);
                    break;
                }
                
                case 10:
                {
                    final CharSequence[] items = getResources().getStringArray(R.array.recovery_options);
                    AlertDialog.Builder builder = new AlertDialog.Builder(con);
                    builder.setTitle(getResources().getString(R.string.select_recovery));
                    builder.setItems(items, m_onRecoverySelect);
                    builder.setCancelable(false);
                    builder.create().show();
                    off = false;
                    break;
                }
            }
            if(off)
            {
                if(m_lock != null)
                {
                    m_lock.release();
                    m_lock = null;
                }
                m_updating = false;
                setProgressBarIndeterminateVisibility(false);
            }
        }
    };
    
    private String manifest_version;
    private String manifest_md5;
    private boolean m_updating;
    private byte m_recovery;
    private Context con;
    private WakeLock m_lock;
}