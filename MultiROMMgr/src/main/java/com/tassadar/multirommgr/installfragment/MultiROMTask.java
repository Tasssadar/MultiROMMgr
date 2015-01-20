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
import com.tassadar.multirommgr.Gpg;
import com.tassadar.multirommgr.Manifest;
import com.tassadar.multirommgr.MgrApp;
import com.tassadar.multirommgr.R;
import com.tassadar.multirommgr.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public abstract class MultiROMTask extends InstallAsyncTask {
    private static final String TAG = "MROMMgr::MultiROMTask";

    public MultiROMTask(Manifest man, Device dev) {
        super();
        m_manifest = man;
        m_dev = dev;
        m_gpg = null;

        if(m_manifest.checkDataGpg()) {
            try {
                m_gpg = new Gpg(Gpg.RING_MULTIROM);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setListener(InstallListener listener) {
        m_listener = listener;
    }
    public void setCanceled(boolean canceled) { m_canceled = canceled; }

    protected boolean downloadInstallationFile(Manifest.InstallationFile f, File destDir) {
        String filename = Utils.getFilenameFromUrl(f.url);
        if(filename == null || filename.isEmpty()) {
            m_listener.onInstallLog(Utils.getString(R.string.invalid_url, f.url));
            m_listener.onInstallComplete(false);
            return false;
        }

        long startOffset = 0;
        f.destFile = new File(destDir, filename);
        if(f.destFile.exists()) {
            long size = f.destFile.length();
            if(size < f.size) {
                startOffset = size;
            } else {
                String md5 = Utils.calculateMD5(f.destFile);
                if(f.md5.equals(md5)) {
                    m_listener.onInstallLog(Utils.getString(R.string.skipping_file, filename));
                    return true;
                }
            }
        }

        if(!downloadFile(f.url, f.destFile, startOffset)) {
            if(!m_canceled)
                m_listener.onInstallComplete(false);
            return false;
        }

        if(m_manifest.checkDataGpg()) {
            if(m_gpg == null) {
                m_listener.onInstallLog(Utils.getString(R.string.gpg_failed));
                m_listener.onInstallComplete(false);
                return false;
            }

            File signFile = new File(f.destFile.getAbsolutePath() + ".asc");
            if(!downloadFile(f.url + ".asc", signFile, 0)) {
                if(!m_canceled)
                    m_listener.onInstallComplete(false);
                return false;
            }

            m_listener.onInstallLog(Utils.getString(R.string.checking_file, filename));
            boolean res = m_gpg.verifyFile(f.destFile.getAbsolutePath());
            signFile.delete();
            if(res) {
                m_listener.onInstallLog(Utils.getString(R.string.ok));
            } else {
                m_listener.onInstallLog(Utils.getString(R.string.failed));
                m_listener.onInstallComplete(false);
                return false;
            }
        } else {
            m_listener.onInstallLog(Utils.getString(R.string.checking_file, filename));
            String md5 = Utils.calculateMD5(f.destFile);
            if(f.md5.isEmpty() || f.md5.equals(md5))
                m_listener.onInstallLog(Utils.getString(R.string.ok));
            else {
                m_listener.onInstallLog(Utils.getString(R.string.failed));
                m_listener.onInstallComplete(false);
                return false;
            }
        }
        return true;
    }

    protected boolean addScriptInstall(Manifest.InstallationFile f, File scriptFile, String cache) {
        String bb = Utils.extractAsset("busybox");

        File tmpfile = new File(MgrApp.getAppContext().getCacheDir(), f.destFile.getName());
        Utils.copyFile(f.destFile, tmpfile);

        if(Utils.isSELinuxEnforcing())
            Utils.chcon(Utils.CHCON_EXECUTABLE, bb);

        List<String> res = Shell.SU.run("%s cp \"%s\" \"%s/recovery/\" && echo success",
                bb, tmpfile.getAbsolutePath(), cache);

        if(Utils.isSELinuxEnforcing())
            Utils.chcon(Utils.CHCON_ORIGINAL, bb);

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

    protected String mountTmpCache(String cacheDev) {
        String bb = Utils.extractAsset("busybox");
        if(bb == null) {
            Log.e(TAG, "Failed to extract busybox!");
            return null;
        }

        // We need to mount the real /cache, we might be running in secondary ROM
        String cmd =
                "mkdir -p /data/local/tmp/tmpcache; " +
                        "cd /data/local/tmp/; " +
                        bb + " mount -t auto " + cacheDev + " tmpcache && " +
                        "mkdir -p tmpcache/recovery && " +
                        "sync && echo /data/local/tmp/tmpcache";

        if(Utils.isSELinuxEnforcing())
            Utils.chcon(Utils.CHCON_BLOCK_ACCESS, bb);

        List<String> out = Shell.SU.run(cmd);

        if(Utils.isSELinuxEnforcing())
            Utils.chcon(Utils.CHCON_BLOCK_ACCESS, bb);

        if(out == null || out.size() != 1) {
            m_listener.onInstallLog("Failed to mount /cache!<br>");
            return null;
        }
        return out.get(0);
    }

    protected void unmountTmpCache(String path) {
        Shell.SU.run("umount \"%s\" && rmdir \"%s\"", path, path);
    }

    @Override
    public boolean isCanceled() {
        return m_canceled;
    }

    protected Manifest m_manifest;
    protected Device m_dev;
    protected Gpg m_gpg;
}
