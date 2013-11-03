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
    public void setCanceled(boolean canceled) { m_canceled = canceled; }

    @Override
    protected Void doInBackground(Void... results) {
        File destDir = new File(Environment.getExternalStorageDirectory(), "/Download");
        destDir.mkdirs();
        String dest = destDir.toString();

        Log.d("InstallAsyncTask", "Using download directory: " + dest);

        ArrayList<Manifest.InstallationFile> files = new ArrayList<Manifest.InstallationFile>();
        if(m_recovery)
            files.add(m_manifest.getRecoveryFile());
        if(m_multirom)
            files.add(m_manifest.getMultiromFile());
        if(m_kernel != null)
            files.add(m_manifest.getKernelFile(m_kernel));

        m_listener.onProgressUpdate(0, 0, true, "Preparing downloads...");
        m_listener.onInstallLog("Preparing downloads...<br>");

        for(int i = 0; i < files.size(); ++i) {
            Manifest.InstallationFile f = files.get(i);

            String filename = Utils.getFilenameFromUrl(f.url);
            if(filename == null || filename.isEmpty()) {
                m_listener.onInstallLog("Invalid url " + f.url);
                m_listener.onInstallComplete(false);
                return null;
            }

            f.destFile = new File(destDir, filename);
            if(f.destFile.exists()) {
                String md5 = Utils.calculateMD5(f.destFile);
                if(f.md5.equals(md5)) {
                    m_listener.onInstallLog(filename + " was already downloaded, skipping...<br>");
                    continue;
                }
            }

            if(!downloadFile(files.get(i).url, f.destFile)) {
                if(!m_canceled)
                    m_listener.onInstallComplete(false);
                return null;
            }

            m_listener.onInstallLog("Checking file " + filename + "... ");
            String md5 = Utils.calculateMD5(f.destFile);
            if(f.md5.isEmpty() || f.md5.equals(md5))
                m_listener.onInstallLog("<font color=\"green\">ok</font><br>");
            else {
                m_listener.onInstallLog("<font color=\"red\">FAILED!</font><br>");
                m_listener.onInstallComplete(false);
                return null;
            }
        }

        m_listener.onProgressUpdate(0, 0, true, "Installing files...");
        m_listener.enableCancel(false);

        boolean needsRecovery = false;
        File script = Utils.getCacheOpenRecoveryScript();
        if(script.exists())
            script.delete();

        for(int i = 0; i < files.size(); ++i) {
            Manifest.InstallationFile f = files.get(i);
            m_listener.onInstallLog("Installing file " + f.destFile.getName() + "... ");
            if(f.type.equals("recovery")) {
                if(!flashRecovery(f, m_manifest.getDevice())) {
                    m_listener.onInstallComplete(false);
                    return null;
                }
            } else if(f.type.equals("multirom") || f.type.equals("kernel")) {
                needsRecovery = true;
                addScriptInstall(f, script);
                m_listener.onInstallLog("<font color=\"yellow\">needs recovery</font><br>");
            }
        }

        if(needsRecovery)
            m_listener.requestRecovery();

        m_listener.onInstallComplete(true);
        return null;
    }

    private boolean downloadFile(String url, File dest) {
        m_listener.onInstallLog("Downloading file " + dest.getName() + "... ");

        m_lastUpdate = 0;
        m_downFilename = dest.getName();
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

    private boolean flashRecovery(Manifest.InstallationFile f, Device dev) {
        String p = Utils.extractAsset("busybox");
        if(p == null) {
            Log.e("InstallAsyncTask", "Failed to extract busybox!");
            return false;
        }

        File tmprecovery = new File(MultiROMMgrApplication.getAppContext().getCacheDir(), f.destFile.getName());
        Utils.copyFile(f.destFile, tmprecovery);

        String cmd = String.format("$(\"%s\" dd if=\"%s\" of=\"%s\" bs=8192 conv=fsync);" +
                                    "if [ \"$?\" = \"0\" ]; then echo success; fi;",
                                    p, tmprecovery.getAbsolutePath(), dev.getRecoveryDev());

        List<String> out = Shell.SU.run(cmd);

        tmprecovery.delete();

        if(out == null || out.isEmpty() || !out.get(out.size()-1).equals("success")) {
            m_listener.onInstallLog("<font color=\"red\">FAILED!</font><br>");
            return false;
        }
        m_listener.onInstallLog("<font color=\"green\">success</font><br>");
        return true;
    }

    private void addScriptInstall(Manifest.InstallationFile f, File scriptFile) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(scriptFile, true);
            String line = "install Download/" + f.destFile.getName() + "\n";
            out.write(line.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(out != null)
                try { out.close(); } catch(IOException e) {}
        }
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

    @Override
    public boolean isCanceled() {
        return m_canceled;
    }

    private boolean m_multirom;
    private boolean m_recovery;
    private String m_kernel;
    private Manifest m_manifest;
    private InstallListener m_listener;
    private String m_downFilename;
    private String m_downProgressTemplate;
    private long m_lastUpdate;
    private boolean m_canceled = false;
}
