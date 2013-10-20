package com.tassadar.multirommgr;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

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

            byte[] buff = new byte[1024];
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
}
