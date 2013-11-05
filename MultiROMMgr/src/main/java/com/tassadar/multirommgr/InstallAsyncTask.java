package com.tassadar.multirommgr;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public abstract class InstallAsyncTask extends AsyncTask<Void, Void, Void> implements Utils.DownloadProgressListener {

    public InstallAsyncTask() {
        super();

        m_downFilename = "";
        m_downProgressTemplate = MultiROMMgrApplication.getAppContext()
                .getResources().getString(R.string.download_progress);

        m_lastUpdate = System.currentTimeMillis();
    }

    protected boolean downloadFile(String url, File dest) {
        m_downFilename = Utils.trim(dest.getName(), 40);
        m_listener.onInstallLog("Downloading file " + m_downFilename + "... ");

        m_lastUpdate = 0;
        onProgressChanged(0, 0);

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(dest);
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
        if(m_canceled || cur - m_lastUpdate < 100)
            return;

        m_lastUpdate = cur;

        // divide by 1024 to kB
        downloaded >>= 10;
        total >>= 10;

        String text = String.format(m_downProgressTemplate, m_downFilename, downloaded, total);
        m_listener.onProgressUpdate(downloaded, total, total == 0, text);
    }

    public void setListener(InstallListener listener) { m_listener = listener; }
    public void setCanceled(boolean canceled) { m_canceled = canceled; }

    @Override
    public boolean isCanceled() {
        return m_canceled;
    }

    protected InstallListener m_listener;
    protected boolean m_canceled = false;
    protected String m_downFilename;
    protected String m_downProgressTemplate;
    protected long m_lastUpdate;
}
