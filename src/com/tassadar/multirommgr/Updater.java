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
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
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
    private static final String LINK_APP_VER = "http://dl.dropbox.com/u/54372958/mgr_ver.txt";
    private static final String UPDATE_PACKAGE = "multirom.zip";
    private static final String FILE_VERSIONS = MultiROMMgrActivity.BASE + "/multirom_ver.txt";
    private static final String DOWNLOAD_LOC = MultiROMMgrActivity.BASE;
    private static final String UPDATE_FOLDER = MultiROMMgrActivity.BASE + "multirom/";
    private static final String RECOVERY_LOC = MultiROMMgrActivity.BASE + "recovery.img";
    private static final String RECOVERY_OLD_LOC = MultiROMMgrActivity.BASE + "recovery_old.img";
    private static final int PACKAGE_SIZE = 1024*1024; 
    private static final int RECOVERY_SIZE = 8*1024*1024; 
    
    private static final Device DEVICE[] =
    {
        new Device("LGP500", 
                "http://dl.dropbox.com/u/54372958/multirom.zip",
                "http://dl.dropbox.com/u/54372958/multirom_man.txt",
                "http://dl.dropbox.com/u/54372958/multirom_ver.txt",
                new String[][]
                { 
                    { "AmonRa", "http://dl.dropbox.com/u/54372958/recovery_am.img" },
                    { "CWM", "http://dl.dropbox.com/u/54372958/recovery_cwm.img" },
                    null,
                }),
        new Device("LS670/VM670", 
                "http://dl.dropbox.com/u/54372958/multirom_V.zip",
                "http://dl.dropbox.com/u/54372958/multirom_man_V.txt",
                "http://dl.dropbox.com/u/54372958/multirom_ver_V.txt",
                new String[][]
                { 
                    { "CWM", "http://dl.dropbox.com/u/54372958/recovery_cwm_V.img" },
                    null,
                }),
    };

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
        m_updated = false;
        setDevice();
        GetVersion(true);
    }
    
    @Override
    protected void onDestroy()
    {
        setResult(m_updated ? RESULT_OK : RESULT_CANCELED);
        super.onDestroy();
    }

    @Override
    public void onBackPressed()
    {
        setResult(m_updated ? RESULT_OK : RESULT_CANCELED);
        super.onBackPressed();
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
    
    private void setDevice()
    {
        String res = MultiROMMgrActivity.runRootCommand("cat /proc/cpuinfo | grep Hardware");
        if(res == null)
        {
            m_device = DEVICE[0];
            return;
        }
        m_device = null;
        for(int i = 0; i < DEVICE.length && m_device == null; ++i)
            if(res.contains(DEVICE[i].name))
                m_device = DEVICE[i];    
        if(m_device == null)
            m_device = DEVICE[0];
        SetStatus(getResources().getString(R.string.device) + " " + m_device.name);
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
    
    private class VerCkThread extends Thread
    {
        private boolean m_update;
        public VerCkThread(boolean update)
        {
            m_update = update;
        }

        public void run() {
            if(m_update)
            {
                //Mgr App version
                m_status.sendEmptyMessage(17);
                if(!DownloadAppVersion())
                {
                    m_status.sendEmptyMessage(18);
                    return;
                }
                try {
                    if(m_app_link != null && 
                       m_app_ver > getPackageManager().getPackageInfo(con.getPackageName(), 0).versionCode)
                    {
                        m_status.sendEmptyMessage(19);
                        return;
                    }
                } catch (NameNotFoundException e) { }
            }
            //Manifest
            m_status.sendEmptyMessage(0);
            if(!DownloadVersions() || !DownloadManifest())
            {
                m_status.sendEmptyMessage(2);
                return;
            }
            //Recheck MD5 with new versions file
            String oldVer = MultiROMMgrActivity.getVersion(false);
            String newVer = MultiROMMgrActivity.getVersion(true);
            if(!oldVer.equals(newVer))
                m_updated = true;
            m_status.sendEmptyMessage(9);
        }
    }
    
    private void GetVersion(boolean update)
    {
        setProgressBarIndeterminateVisibility(true);
        new VerCkThread(update).start();
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
        
        m_updated = true;
        
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
                String res = MultiROMMgrActivity.runRootCommand("busybox mkdir /tmp");
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
                
                String sd_ext = Storage.getSDExt();
                if(sd_ext != null)
                    MultiROMMgrActivity.runRootCommand("busybox mkdir " + sd_ext + "/multirom");
                
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
                
                if(m_device.link_recovery[m_recovery] == null)
                {
                    m_status.sendEmptyMessage(5);
                    return;
                }
                m_status.sendEmptyMessage(12);
                if(!DownloadFile(m_device.link_recovery[m_recovery][1], RECOVERY_LOC, RECOVERY_SIZE))
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
            URL address = new URL(m_device.link_manifest);
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
        if(!DownloadFile(m_device.link_package, DOWNLOAD_LOC + UPDATE_PACKAGE, PACKAGE_SIZE))
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
        return DownloadFile(m_device.link_versions, FILE_VERSIONS, 1024*10);
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
            byte buff[] = new byte[1024];
            while((cur = bis.read(buff)) != -1)
                baf.append(buff, 0, cur);

            FileOutputStream fos = new FileOutputStream(target);
            fos.write(baf.toByteArray());
            fos.close();
        }
        catch(IOException e) {
            return false;
        }
        return true;
    }
    
    private boolean DownloadAppVersion()
    {
        try {
            m_app_ver = getPackageManager().getPackageInfo(this.getPackageName(), 0).versionCode;
        } catch (NameNotFoundException e1) {
            m_app_ver = -1;
        }
        try {
            URL address = new URL(LINK_APP_VER);
            URLConnection conn = address.openConnection();
            InputStream is = conn.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            int cur = 0;
            String tmp = "";
            while((cur = bis.read()) != -1)
                 tmp += String.valueOf((char)cur);
            m_app_ver = Integer.valueOf(tmp.split("\n")[0]);
            m_app_link = tmp.split("\n")[1];
        }
        catch(IOException e) {
            return false;
        }
        return true;
    }
    
    private final android.content.DialogInterface.OnClickListener m_onRecoverySelect = new android.content.DialogInterface.OnClickListener()
    {
        @Override
        public void onClick(DialogInterface arg0, int arg1) {
            m_recovery = (byte)arg1;
        }
    };
    
    private final android.content.DialogInterface.OnClickListener m_click_install = new android.content.DialogInterface.OnClickListener()
    {
        @Override
        public void onClick(DialogInterface arg0, int arg1) {
            Intent mktIntent = new Intent(Intent.ACTION_VIEW);
            mktIntent.setData(Uri.parse(m_app_link));
            startActivity(mktIntent);
        }
    };
    
    private final android.content.DialogInterface.OnClickListener m_click_no = new android.content.DialogInterface.OnClickListener()
    {
        @Override
        public void onClick(DialogInterface arg0, int arg1) {
            GetVersion(false);
        }
    };

    private final Handler m_status = new Handler()
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
                case 17: SetStatus(getResources().getString(R.string.app_check));     off=false; break;
            
                case 2:  SetStatus(getResources().getString(R.string.down_manifest_error)); break;
                case 3:  SetStatus(getResources().getString(R.string.down_pack_error));     break;
                case 6:  SetStatus(getResources().getString(R.string.mktmp_fail));          break;
                case 7:  SetStatus(getResources().getString(R.string.flash_fail));          break;
                case 5:  SetStatus(getResources().getString(R.string.update_complete));     break;
                case 11: SetStatus(getResources().getString(R.string.rec_down_err));        break;
                case 14: SetStatus(getResources().getString(R.string.rec_flash_err));       break;
                case 15: SetStatus(getResources().getString(R.string.reboot));              break;
                case 16: SetStatus(getResources().getString(R.string.unzip_error));         break;
                case 18: SetStatus(getResources().getString(R.string.app_check_err));       break;
                
                // Show versions
                case 9:
                {
                    String text = getResources().getString(R.string.current_version);
                    text += " " + MultiROMMgrActivity.getVersion(false) + "\n";
                    text += getResources().getString(R.string.newest_version);
                    text += " " + manifest_version + "\n";
                    if(manifest_version.equals(MultiROMMgrActivity.getVersion(false)))
                        text += getResources().getString(R.string.no_update);
                    else
                    {
                        Btn(true);
                        text += getResources().getString(R.string.press_update);
                    }
                    SetStatus(text);
                    break;
                }
                // Recovery selection
                case 10:
                {
                    final CharSequence[] items = new CharSequence[m_device.link_recovery.length];
                    for(int i = 0; i < m_device.link_recovery.length; ++i)
                    {
                        if(m_device.link_recovery[i] == null)
                            items[i] = getResources().getString(R.string.no_install_rec);
                        else
                            items[i] = m_device.link_recovery[i][0];
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(con);
                    builder.setTitle(getResources().getString(R.string.select_recovery));
                    builder.setItems(items, m_onRecoverySelect);
                    builder.setCancelable(false);
                    builder.create().show();
                    off = false;
                    break;
                }
                // Download app update
                case 19:
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(con);
                    builder.setTitle(getResources().getString(R.string.update));
                    builder.setMessage(getResources().getString(R.string.update_avail));
                    builder.setPositiveButton(getResources().getString(R.string.install), m_click_install);
                    builder.setNegativeButton(getResources().getString(R.string.no_thanks), m_click_no);
                    builder.setCancelable(false);
                    builder.create().show();
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
    private boolean m_updated;
    private WakeLock m_lock;
    private Device m_device;
    private int m_app_ver;
    private String m_app_link;
}