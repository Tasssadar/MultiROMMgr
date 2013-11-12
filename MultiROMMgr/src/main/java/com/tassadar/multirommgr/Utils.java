/*
 * This file is part of MultiROM Manager.
 *
 * MultiROM Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MultiROM Manager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MultiROM Manager. If not, see <http://www.gnu.org/licenses/>.
 */

package com.tassadar.multirommgr;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

import eu.chainfire.libsuperuser.Shell;

public class Utils {

    private static final int BUSYBOX_VER = 2;

    private static String m_downloadDir = null;
    public static String getDownloadDir() {
        if(m_downloadDir != null)
            return m_downloadDir;

        SharedPreferences p = MultiROMMgrApplication.getPreferences();
        m_downloadDir = p.getString(SettingsActivity.GENERAL_DOWNLOAD_DIR, getDefaultDownloadDir());
        return m_downloadDir;
    }

    public static String getDefaultDownloadDir() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download";
    }

    public static void setDownloadDir(String path) {
        m_downloadDir = path;

        SharedPreferences.Editor p = MultiROMMgrApplication.getPreferences().edit();
        p.putString(SettingsActivity.GENERAL_DOWNLOAD_DIR, path);
        p.commit();
    }

    public static String extractAsset(String name) {
        Context ctx = MultiROMMgrApplication.getAppContext();

        String path = "/data/data/" + ctx.getPackageName() + "/" + name;
        File f = new File(path);
        if(f.exists()) {
            if(!name.equals("busybox"))
                return path;

            SharedPreferences pref = MultiROMMgrApplication.getPreferences();
            if(pref.getInt("busybox_ver", 0) == BUSYBOX_VER)
                return path;
        }

        try {
            InputStream in = ctx.getAssets().open(name);
            FileOutputStream out = new FileOutputStream(path);

            byte[] buff = new byte[4096];
            for(int len; (len = in.read(buff)) != -1; )
                out.write(buff, 0, len);

            out.close();
            in.close();

            f.setExecutable(true);

            if(name.equals("busybox")) {
                SharedPreferences pref = MultiROMMgrApplication.getPreferences();
                pref.edit().putInt("busybox_ver", BUSYBOX_VER).commit();
            }
            return path;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static File getCacheOpenRecoveryScript() {
        return new File(MultiROMMgrApplication.getAppContext().getCacheDir(), "openrecoveryscript");
    }

    public static void deployOpenRecoveryScript(String cacheDev) {
        File script = getCacheOpenRecoveryScript();

        String bb = Utils.extractAsset("busybox");

        // We need to mount the real /cache, we might be running in secondary ROM
        String cmd =
                "mkdir -p /data/local/tmp/tmpcache; " +
                "cd /data/local/tmp/; " +
                bb + " mount -t auto " + cacheDev + " tmpcache && " +
                "mkdir -p tmpcache/recovery && " +
                "cat \"" + script.getAbsolutePath() + "\" > tmpcache/recovery/openrecoveryscript; " +
                "sync;" +
                "umount tmpcache && rmdir tmpcache";

        Thread t = new Thread(new ShellRunnable(cmd));
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static boolean reboot(String target) {
        String cmd = "sync; reboot";
        if(target != null && !target.isEmpty())
            cmd += " " + target;

        Thread t = new Thread(new ShellRunnable(cmd));
        t.start();
        try {
            t.join(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.e("Utils", "reboot with target " + target + " failed!");
        return false;
    }

    public static String trim(String str, int length) {
        if(str.length() <= length)
            return str;

        int part = length/2;
        return str.substring(0, part-1) + "..." + str.substring(str.length()-part+2);
    }

    private static class ShellRunnable implements Runnable {
        private String m_cmd;
        public ShellRunnable(String cmd) {
            m_cmd = cmd;
        }

        @Override
        public void run() {
            Shell.SU.run(m_cmd);
        }
    }

    public interface DownloadProgressListener {
        public void onProgressChanged(int downloaded, int total);
        public boolean isCanceled();
    }

    public static boolean downloadFile(String strUrl, OutputStream output, DownloadProgressListener listener) throws IOException {
        InputStream in = null;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(strUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.connect();

            if(conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.e("Utils", "downloadFile failed for url \"" + strUrl + "\" with code " + conn.getResponseCode());
                return false;
            }

            int total = conn.getContentLength();
            int downloaded = 0;

            byte[] buff = new byte[8192];
            in = conn.getInputStream();

            for(int len; (len = in.read(buff)) != -1;) {
                downloaded += len;
                output.write(buff, 0, len);

                if(listener != null) {
                    listener.onProgressChanged(downloaded, total);
                    if(listener.isCanceled())
                        return false;
                }
            }
        } catch(IOException ex) {
            throw ex;
        } finally {
            try {
                if(in != null)
                    in.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            if(conn != null)
                conn.disconnect();
        }
        return true;
    }

    public static boolean isNumeric(char c) {
        return (c >= '0' && c <= '9');
    }

    public static String getFilenameFromUrl(String url) {
        int idx = url.lastIndexOf('/');
        if(idx == -1)
            return url;
        return url.substring(idx+1);
    }

    public static String calculateMD5(String file) {
        return calculateMD5(new File(file));
    }

    public static String calculateMD5(File file) {
        return calculateChecksum(file, "MD5");
    }

    public static String calculateSHA256(String file) {
        return calculateSHA256(new File(file));
    }

    public static String calculateSHA256(File file) {
        return calculateChecksum(file, "SHA-256");
    }

    public static String calculateChecksum(File file, String checksumType) {
        String res = null;
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            byte[] buff = new byte[8192];
            MessageDigest digest = MessageDigest.getInstance(checksumType);
            int read;

            while((read = in.read(buff)) > 0)
                digest.update(buff, 0, read);

            return Utils.bytesToHex(digest.digest());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(in != null)
                try { in.close(); } catch (IOException e) { }
        }
        return res;
    }

    private static final char[] HEX = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
    public static String bytesToHex(byte[] bytes) {
        StringBuilder b = new StringBuilder(bytes.length*2);
        for(int i = 0; i < bytes.length; ++i) {
            b.append(HEX[(bytes[i] & 0xFF) >> 4]);
            b.append(HEX[(bytes[i] & 0xFF) & 0x0F]);
        }
        return b.toString();
    }

    public static boolean copyFile(File src, File dst) {
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dst);
            byte[] buff = new byte[8192];

            int read;
            while((read = in.read(buff)) > 0)
                out.write(buff, 0, read);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if(in != null)
                try { in.close(); } catch (IOException e) { }
            if(out != null)
                try { out.close(); } catch (IOException e) { }

        }
        return true;
    }

    public static String getString(int id) {
        return MultiROMMgrApplication.getAppContext().getResources().getString(id);
    }

    public static String getString(int id, Object... args) {
        return MultiROMMgrApplication.getAppContext().getResources().getString(id, args);
    }
}
