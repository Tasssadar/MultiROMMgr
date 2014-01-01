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

package com.tassadar.multirommgr.installfragment;

import android.util.Log;

import com.tassadar.multirommgr.Device;
import com.tassadar.multirommgr.Manifest;
import com.tassadar.multirommgr.MgrApp;
import com.tassadar.multirommgr.R;
import com.tassadar.multirommgr.Recovery;
import com.tassadar.multirommgr.UpdateChecker;
import com.tassadar.multirommgr.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class MultiROMInstallTask extends InstallAsyncTask {

    public MultiROMInstallTask(Manifest man, Device dev) {
        super();
        m_manifest = man;
        m_dev = dev;
    }

    public void setParts(boolean multirom, boolean recovery, String kernel) {
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
        String dest = Utils.getDownloadDir();
        File destDir = new File(dest);
        destDir.mkdirs();

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

        String cache = mountTmpCache(m_dev.getCacheDev());
        if(cache == null) {
            m_listener.onInstallComplete(false);
            return null;
        }

        for(int i = 0; i < files.size(); ++i) {
            Manifest.InstallationFile f = files.get(i);
            m_listener.onInstallLog(Utils.getString(R.string.installing_file, f.destFile.getName()));
            if(f.type.equals("recovery")) {
                if(!flashRecovery(f, m_dev)) {
                    m_listener.onInstallComplete(false);
                    return null;
                }
            } else if(f.type.equals("multirom") || f.type.equals("kernel")) {
                needsRecovery = true;
                if(!addScriptInstall(f, script, cache)) {
                    m_listener.onInstallComplete(false);
                    return null;
                }
                m_listener.onInstallLog(Utils.getString(R.string.needs_recovery));
            }
        }

        unmountTmpCache(cache);

        if(UpdateChecker.isEnabled()) {
            String m_ver = null, r_ver = null;

            if(m_multirom) {
                // Assume installation completes successfully in recovery - if not,
                // the version will be updated when the user returns to the MainActivity.
                m_ver = m_manifest.getMultiromVersion();
            }

            if(m_recovery) {
                Recovery r = new Recovery();
                if(r.findRecoveryVersion(m_dev))
                    r_ver = r.getVersionString();
            }

            UpdateChecker.lazyUpdateVersions(m_dev, m_ver, r_ver);
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

        File tmprecovery = new File(MgrApp.getAppContext().getCacheDir(), f.destFile.getName());
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

    private boolean addScriptInstall(Manifest.InstallationFile f, File scriptFile, String cache) {
        String bb = Utils.extractAsset("busybox");

        File tmpfile = new File(MgrApp.getAppContext().getCacheDir(), f.destFile.getName());
        Utils.copyFile(f.destFile, tmpfile);

        List<String> res = Shell.SU.run("%s cp \"%s\" \"%s/recovery/\" && echo success",
                bb, tmpfile.getAbsolutePath(), cache);

        tmpfile.delete();

        if(res == null || res.size() != 1 || !res.get(0).equals("success")) {
            m_listener.onInstallLog("Failed to copy file to cache!");
            return false;
        }

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(scriptFile, true);
            String line = String.format("install /cache/recovery/%s\n", f.destFile.getName());
            out.write(line.getBytes());
            line = String.format("cmd rm \"/cache/recovery/%s\"\n", f.destFile.getName());
            out.write(line.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(out != null)
                try { out.close(); } catch(IOException e) {}
        }
        return true;
    }

    private String mountTmpCache(String cacheDev) {
        String bb = Utils.extractAsset("busybox");
        if(bb == null) {
            Log.e("InstallAsyncTask", "Failed to extract busybox!");
            return null;
        }

        // We need to mount the real /cache, we might be running in secondary ROM
        String cmd =
                "mkdir -p /data/local/tmp/tmpcache; " +
                "cd /data/local/tmp/; " +
                bb + " mount -t auto " + cacheDev + " tmpcache && " +
                "mkdir -p tmpcache/recovery && " +
                "sync && echo /data/local/tmp/tmpcache";

        List<String> out = Shell.SU.run(cmd);
        if(out == null || out.size() != 1) {
            m_listener.onInstallLog("Failed to mount /cache!<br>");
            return null;
        }
        return out.get(0);
    }

    private void unmountTmpCache(String path) {
        Shell.SU.run("umount \"%s\" && rmdir \"%s\"", path, path);
    }

    @Override
    public boolean isCanceled() {
        return m_canceled;
    }

    private boolean m_multirom;
    private boolean m_recovery;
    private String m_kernel;
    private Manifest m_manifest;
    private Device m_dev;
}
