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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.http.HttpResponseCache;
import android.os.Environment;
import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class Utils {

    private static final int BUSYBOX_VER = 4;

    private static String m_downloadDir = null;
    public static String getDownloadDir() {
        if(m_downloadDir != null)
            return m_downloadDir;

        SharedPreferences p = MgrApp.getPreferences();
        m_downloadDir = p.getString(SettingsActivity.GENERAL_DOWNLOAD_DIR, getDefaultDownloadDir());
        return m_downloadDir;
    }

    public static String getDefaultDownloadDir() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download";
    }

    public static void setDownloadDir(String path) {
        m_downloadDir = path;

        SharedPreferences.Editor p = MgrApp.getPreferences().edit();
        p.putString(SettingsActivity.GENERAL_DOWNLOAD_DIR, path);
        p.commit();
    }

    public static String extractAsset(String name) {
        Context ctx = MgrApp.getAppContext();

        String path = "/data/data/" + ctx.getPackageName() + "/" + name;
        File f = new File(path);
        if(f.exists()) {
            if(!name.equals("busybox"))
                return path;

            SharedPreferences pref = MgrApp.getPreferences();
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
                SharedPreferences pref = MgrApp.getPreferences();
                pref.edit().putInt("busybox_ver", BUSYBOX_VER).commit();
            }
            return path;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static File getCacheOpenRecoveryScript() {
        return new File(MgrApp.getAppContext().getCacheDir(), "openrecoveryscript");
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

        ShellThread t = new ShellThread(cmd);
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

        ShellThread t = new ShellThread(cmd);
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

    private static class ShellThread extends Thread {
        private String m_cmd;
        public ShellThread(String cmd) {
            m_cmd = cmd;
        }

        public ShellThread(String fmt, Object... args) {
            m_cmd = String.format(fmt, args);
        }

        @Override
        public void run() {
            Shell.SU.run(m_cmd);
        }
    }

    public interface DownloadProgressListener {
        public void onProgressChanged(long downloaded, long total);
        public boolean isCanceled();
    }

    public static boolean downloadFile(String strUrl, OutputStream output, DownloadProgressListener listener) throws IOException {
        return downloadFile(strUrl, output, listener, false, 0);
    }

    public static boolean downloadFile(String strUrl, OutputStream output, DownloadProgressListener listener, boolean useCache) throws IOException {
        return downloadFile(strUrl, output, listener, useCache, 0);
    }

    public static boolean downloadFile(String strUrl, OutputStream output, DownloadProgressListener listener, boolean useCache, long offset) throws IOException {
        InputStream in = null;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(strUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setUseCaches(useCache);

            if(useCache)
                conn.addRequestProperty("Cache-Control", "max-age=0");

            if(offset > 0) {
                conn.addRequestProperty("Range", "Bytes=" + offset + "-");
            } else {
                offset = 0;
            }

            conn.connect();

            int res = conn.getResponseCode();
            if(res != HttpURLConnection.HTTP_OK && res != HttpURLConnection.HTTP_PARTIAL) {
                Log.e("Utils", "downloadFile failed for url \"" + strUrl + "\" with code " + res);
                return false;
            }

            long total = conn.getContentLength() + offset;
            long downloaded = offset;

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

    public static void installHttpCache(Context ctx) {
        try {
            File httpCacheDir = new File(ctx.getCacheDir(), "http");
            // 1MB should be enough for manifests
            HttpResponseCache.install(httpCacheDir, 1 * 1024 * 1024);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void flushHttpCache() {
        HttpResponseCache c = HttpResponseCache.getInstalled();
        if(c != null)
            c.flush();
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
        return MgrApp.getAppContext().getResources().getString(id);
    }

    public static String getString(int id, Object... args) {
        return MgrApp.getAppContext().getResources().getString(id, args);
    }

    public static boolean isIntentAvailable(String action) {
        final PackageManager packageManager = MgrApp.getAppContext().getPackageManager();
        final Intent intent = new Intent(action);
        List<ResolveInfo> list =
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    public static Bitmap resizeBitmap(Bitmap bmp, int targetW, int targetH) {
        if(bmp == null)
            return null;

        int w = bmp.getWidth();
        int h = bmp.getHeight();

        float scale = ((float)targetW) / w;
        if(h * scale > targetH)
            scale = ((float)targetH) / h;

        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        return Bitmap.createBitmap(bmp, 0, 0, w, h, matrix, false);
    }

    public static String readFile(String path) {
        FileInputStream in = null;
        try {
            in = new FileInputStream(path);
            StringBuilder builder = new StringBuilder();
            byte buff[] = new byte[4096];
            int read;
            while((read = in.read(buff)) > 0)
                builder.append(new String(buff, 0, read, "UTF-8"));
            return builder.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if(in != null)
                try { in.close(); } catch(IOException e) {}
        }
    }

    public static void close(Closeable c) {
        if(c == null)
            return;

        try {
            c.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
