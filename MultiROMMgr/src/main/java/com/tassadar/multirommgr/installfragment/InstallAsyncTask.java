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

import android.os.AsyncTask;

import com.tassadar.multirommgr.MgrApp;
import com.tassadar.multirommgr.R;
import com.tassadar.multirommgr.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public abstract class InstallAsyncTask extends AsyncTask<Void, Void, Void> implements Utils.DownloadProgressListener {

    public InstallAsyncTask() {
        super();

        m_downFilename = "";
        m_downProgressTemplate = MgrApp.getAppContext()
                .getResources().getString(R.string.download_progress);

        m_lastUpdate = System.currentTimeMillis();
    }

    protected boolean downloadFile(String url, File dest) {
        m_downFilename = Utils.trim(dest.getName(), 40);
        m_listener.onInstallLog(Utils.getString(R.string.downloading_file, m_downFilename));

        m_lastUpdate = 0;
        onProgressChanged(0, 0);

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(dest);
            if(Utils.downloadFile(url, out, this)) {
                m_listener.onInstallLog(Utils.getString(R.string.success));
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
        m_listener.onInstallLog(Utils.getString(R.string.failed));
        return false;
    }

    @Override
    public void onProgressChanged(int downloaded, int total) {
        long cur = System.currentTimeMillis();
        if(m_canceled || cur - m_lastUpdate < 200)
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
