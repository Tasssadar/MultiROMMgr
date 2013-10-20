package com.tassadar.multirommgr;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

public class InstallAsyncTask extends AsyncTask<Void, Void, Void> implements Utils.DownloadProgressListener {

    public InstallAsyncTask(Manifest man, boolean multirom, boolean recovery, String kernel) {
        super();
        m_manifest = man;
        m_multirom = multirom;
        m_recovery = recovery;
        m_kernel = kernel;
        m_downFilename = "";
        m_downProgressTemplate = MultiROMMgrApplication.getAppContext()
                .getResources().getString(R.string.download_progress);

        m_lastUpdate = System.currentTimeMillis();
    }

    public void setListener(InstallListener listener) {
        m_listener = listener;
    }

    @Override
    protected Void doInBackground(Void... results) {
        File destDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        destDir.mkdirs();
        String dest = destDir.toString();

        Log.d("InstallAsyncTask", "Using download directory: " + dest);

        ArrayList<String> to_download = new ArrayList<String>();
        if(m_multirom)
            to_download.add(m_manifest.getMultiromUrl());
        if(m_recovery)
            to_download.add(m_manifest.getRecoveryUrl());
        if(m_kernel != null)
            to_download.add(m_manifest.getKernelUrl(m_kernel));

        for(int i = 0; i < to_download.size(); ++i)
            if(!downloadFile(to_download.get(i), dest))
                break;

        m_listener.onProgressUpdate(0, 0, true, "Checking files...");
        return null;
    }

    private boolean downloadFile(String url, String dest) {
        String filename = Utils.getFilenameFromUrl(url);
        if(filename == null) {
            Log.e("InstallAsyncTask", "Invalid url " + url);
            return false;
        }

        m_downFilename = filename;

        m_listener.onInstallLog("Downloading file " + filename + "... ");

        m_lastUpdate = 0;
        onProgressChanged(0, 0);

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(dest + "/" + filename);
            if(Utils.downloadFile(url, out, this)) {
                m_listener.onInstallLog("<font color=\"green\">success</font><br>");
                return true;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(out != null)
                    out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        m_listener.onInstallLog("<font color=\"red\">FAILED!</font><br>");
        return false;
    }

    @Override
    public void onProgressChanged(int downloaded, int total) {
        long cur = System.currentTimeMillis();
        if(cur - m_lastUpdate < 100)
            return;

        m_lastUpdate = cur;

        // divide by 1024 to kB
        downloaded >>= 10;
        total >>= 10;

        String text = String.format(m_downProgressTemplate, m_downFilename, downloaded, total);
        m_listener.onProgressUpdate(downloaded, total, total == 0, text);
    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    private boolean m_multirom;
    private boolean m_recovery;
    private String m_kernel;
    private Manifest m_manifest;
    private InstallListener m_listener;
    private String m_downFilename;
    private String m_downProgressTemplate;
    private long m_lastUpdate;
}
