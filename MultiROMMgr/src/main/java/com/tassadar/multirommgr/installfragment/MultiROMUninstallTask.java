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
import java.util.Date;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class MultiROMUninstallTask extends MultiROMTask {
    private static final String TAG = "MROMMgr::MultiromUninstallTask";

    public MultiROMUninstallTask(Manifest man, Device dev) {
        super(man, dev);
    }

    @Override
    protected Void doInBackground(Void... results) {
        String dest = Utils.getDownloadDir();
        File destDir = new File(dest);
        destDir.mkdirs();

        Log.d(TAG, "Using download directory: " + dest);

        Manifest.InstallationFile uninstaller = m_manifest.getUninstallerFile();

        m_listener.onProgressUpdate(0, 0, true, Utils.getString(R.string.preparing_downloads, ""));
        m_listener.onInstallLog(Utils.getString(R.string.preparing_downloads, "<br>"));

        if(!downloadInstallationFile(uninstaller, destDir))
            return null;

        m_listener.onProgressUpdate(0, 0, true, Utils.getString(R.string.installing_files));
        m_listener.enableCancel(false);

        m_listener.onInstallLog(Utils.getString(R.string.installing_file,
                uninstaller.destFile.getName()));

        File script = Utils.getCacheOpenRecoveryScript();
        if(script.exists())
            script.delete();

        String cache = mountTmpCache(m_dev.getCacheDev());
        if(cache == null) {
            m_listener.onInstallComplete(false);
            return null;
        }

        if(!addScriptInstall(uninstaller, script, cache)) {
            m_listener.onInstallComplete(false);
            return null;
        }

        m_listener.onInstallLog(Utils.getString(R.string.needs_recovery));
        unmountTmpCache(cache);

        if(UpdateChecker.isEnabled()) {
            UpdateChecker.lazyUpdateVersions(m_dev, "0", Recovery.VER_FMT.format(new Date(0)));
        }

        m_listener.requestRecovery(false);
        m_listener.onInstallComplete(true);
        return null;
    }
}
