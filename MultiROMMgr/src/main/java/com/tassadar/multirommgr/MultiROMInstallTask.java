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

public class MultiROMInstallTask extends InstallAsyncTask {

    public MultiROMInstallTask(Manifest man, boolean multirom, boolean recovery, String kernel) {
        super();
        m_manifest = man;
        m_multirom = multirom;
        m_recovery = recovery;
        m_kernel = kernel;
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

        Log.d("MultiROMInstallTask", "Using download directory: " + dest);

        ArrayList<Manifest.InstallationFile> files = new ArrayList<Manifest.InstallationFile>();
        if(m_recovery)
            files.add(m_manifest.getRecoveryFile());
        if(m_multirom)
            files.add(m_manifest.getMultiromFile());
        if(m_kernel != null)
            files.add(m_manifest.getKernelFile(m_kernel));

        m_listener.onProgressUpdate(0, 0, true, Utils.getString(R.string.preparing_downloads, ""));
        m_listener.onInstallLog(Utils.getString(R.string.preparing_downloads, "<br>"));

        for(int i = 0; i < files.size(); ++i) {
            Manifest.InstallationFile f = files.get(i);

            String filename = Utils.getFilenameFromUrl(f.url);
            if(filename == null || filename.isEmpty()) {
                m_listener.onInstallLog(Utils.getString(R.string.invalid_url, f.url));
                m_listener.onInstallComplete(false);
                return null;
            }

            f.destFile = new File(destDir, filename);
            if(f.destFile.exists()) {
                String md5 = Utils.calculateMD5(f.destFile);
                if(f.md5.equals(md5)) {
                    m_listener.onInstallLog(Utils.getString(R.string.skipping_file, filename));
                    continue;
                }
            }

            if(!downloadFile(files.get(i).url, f.destFile)) {
                if(!m_canceled)
                    m_listener.onInstallComplete(false);
                return null;
            }

            m_listener.onInstallLog(Utils.getString(R.string.checking_file, filename));
            String md5 = Utils.calculateMD5(f.destFile);
            if(f.md5.isEmpty() || f.md5.equals(md5))
                m_listener.onInstallLog(Utils.getString(R.string.ok));
            else {
                m_listener.onInstallLog(Utils.getString(R.string.failed));
                m_listener.onInstallComplete(false);
                return null;
            }
        }

        m_listener.onProgressUpdate(0, 0, true, Utils.getString(R.string.installing_files));
        m_listener.enableCancel(false);

        boolean needsRecovery = false;
        File script = Utils.getCacheOpenRecoveryScript();
        if(script.exists())
            script.delete();

        for(int i = 0; i < files.size(); ++i) {
            Manifest.InstallationFile f = files.get(i);
            m_listener.onInstallLog(Utils.getString(R.string.installing_file, f.destFile.getName()));
            if(f.type.equals("recovery")) {
                if(!flashRecovery(f, m_manifest.getDevice())) {
                    m_listener.onInstallComplete(false);
                    return null;
                }
            } else if(f.type.equals("multirom") || f.type.equals("kernel")) {
                needsRecovery = true;
                addScriptInstall(f, script);
                m_listener.onInstallLog(Utils.getString(R.string.needs_recovery));
            }
        }

        if(needsRecovery)
            m_listener.requestRecovery(false);

        m_listener.onInstallComplete(true);
        return null;
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
            m_listener.onInstallLog(Utils.getString(R.string.failed));
            return false;
        }
        m_listener.onInstallLog(Utils.getString(R.string.success));
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
    public boolean isCanceled() {
        return m_canceled;
    }

    private boolean m_multirom;
    private boolean m_recovery;
    private String m_kernel;
    private Manifest m_manifest;
}
