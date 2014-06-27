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

public class MultiROMInstallTask extends MultiROMTask {

    public MultiROMInstallTask(Manifest man, Device dev) {
        super(man, dev);
    }

    public void setParts(boolean multirom, boolean recovery, String kernel) {
        m_multirom = multirom;
        m_recovery = recovery;
        m_kernel = kernel;
    }

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
            if(!downloadInstallationFile(files.get(i), destDir))
                return null;
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
                    unmountTmpCache(cache);
                    m_listener.onInstallComplete(false);
                    return null;
                }
            } else if(f.type.equals("multirom") || f.type.equals("kernel")) {
                needsRecovery = true;
                if(!addScriptInstall(f, script, cache)) {
                    unmountTmpCache(cache);
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

        if(Utils.isSELinuxEnforcing())
            Utils.chcon(Utils.CHCON_BLOCK_ACCESS, p);

        List<String> out = Shell.SU.run(cmd);

        if(Utils.isSELinuxEnforcing())
            Utils.chcon(Utils.CHCON_ORIGINAL, p);

        tmprecovery.delete();

        if(out == null || out.isEmpty() || !out.get(out.size()-1).equals("success")) {
            m_listener.onInstallLog(Utils.getString(R.string.failed));
            return false;
        }
        m_listener.onInstallLog(Utils.getString(R.string.success));
        return true;
    }

    private boolean m_multirom;
    private boolean m_recovery;
    private String m_kernel;
}
