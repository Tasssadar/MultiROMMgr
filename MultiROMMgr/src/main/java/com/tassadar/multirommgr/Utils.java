package com.tassadar.multirommgr;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

import eu.chainfire.libsuperuser.Shell;

public class Utils {

    public static String extractAsset(String name) {
        Context ctx = MultiROMMgrApplication.getAppContext();

        String path = "/data/data/" + ctx.getPackageName() + "/" + name;
        File f = new File(path);
        if(f.exists())
            return path;

        try {
            InputStream in = ctx.getAssets().open(name);
            FileOutputStream out = new FileOutputStream(path);

            byte[] buff = new byte[4096];
            for(int len; (len = in.read(buff)) != -1; )
                out.write(buff, 0, len);

            out.close();
            in.close();

            f = new File(path);
            f.setExecutable(true);
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

        // We need to mount the real /cache, we might be running in secondary ROM
        String cmd =
                "mkdir -p /data/local/tmp/tmpcache; " +
                "cd /data/local/tmp/; " +
                "mount -t auto " + cacheDev + " tmpcache && " +
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
                        break;
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
            return null;
        return url.substring(idx+1);
    }

    public static String calculateMD5(String file) {
        return calculateMD5(new File(file));
    }

    public static String calculateMD5(File file) {
        String res = null;
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            byte[] buff = new byte[8192];
            MessageDigest digest = MessageDigest.getInstance("MD5");
            int read;

            while((read = in.read(buff)) > 0)
                digest.update(buff, 0, read);

            res = new BigInteger(1, digest.digest()).toString(16);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(in != null)
                try { in.close(); } catch (IOException e) { }
        }
        return res;
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
}
