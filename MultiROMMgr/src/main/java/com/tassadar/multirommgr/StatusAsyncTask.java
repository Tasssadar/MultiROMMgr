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

package com.tassadar.multirommgr;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

import eu.chainfire.libsuperuser.Shell;

public class StatusAsyncTask extends AsyncTask <Void, String, StatusAsyncTask.Result> {

    public static final int RES_OK                 = 0x00;
    public static final int RES_NO_SU              = 0x01;
    public static final int RES_NO_MULTIROM        = 0x02;
    public static final int RES_FAIL_MROM_VER      = 0x04;
    public static final int RES_UNSUPPORTED        = 0x08;
    public static final int RES_NO_RECOVERY        = 0x10;
    public static final int RES_MANIFEST_FAIL      = 0x20;
    public static final int RES_NO_MANIFEST        = 0x40;

    public interface StatusAsyncTaskListener {
        public void onStatusTaskFinished(Result res);
    }

    private static StatusAsyncTask instance = null;
    public static StatusAsyncTask instance() {
        if(instance == null)
            instance = new StatusAsyncTask();
        return instance;
    }

    public static void destroy() {
        instance = null;
    }

    private StatusAsyncTask() {
        super();
        m_layout = new WeakReference<View>(null);
        m_res = null;
    }

    public void setListener(StatusAsyncTaskListener listener) {
        m_listener = listener;
        if(m_listener != null && m_res != null)
            m_listener.onStatusTaskFinished(m_res);
    }

    public void setStatusCardLayout(View layout) {
        m_layout = new WeakReference<View>(layout);
        applyResult();
    }

    public Manifest getManifest() {
        return m_res != null ? m_res.manifest : null;
    }

    public Device getDevice() {
        return m_res != null ? m_res.device : null;
    }

    public MultiROM getMultiROM() {
        return m_res != null ? m_res.multirom : null;
    }

    public boolean hasKexecKernel() {
        return (m_res != null && m_res.kernel != null) ? m_res.kernel.hasKexec() : false;
    }

    public void execute() {
        if(this.getStatus() == Status.PENDING)
            this.execute((Void) null);
    }

    public boolean isComplete() {
        return getStatus() == Status.FINISHED;
    }

    protected Result doInBackground(Void ...arg) {
        Result res = new Result();

        publishProgress(Utils.getString(R.string.prog_detecting_dev));

        SharedPreferences p = MgrApp.getPreferences();
        Device dev = Device.load(p.getString(SettingsActivity.DEV_DEVICE_NAME, Build.DEVICE));
        if(dev == null) {
            res.code = RES_UNSUPPORTED;
            return res;
        }

        publishProgress(Utils.getString(R.string.prog_checking_root));

        if(!Shell.SU.available()) {
            res.code = RES_NO_SU;
            return res;
        }

        res.device = dev;

        publishProgress(Utils.getString(R.string.prog_looking_for_multirom));

        MultiROM m = new MultiROM();
        if(!m.findMultiROMDir()) {
            res.code |= RES_NO_MULTIROM;
        } else {
            if(!m.findVersion())
                res.code |= RES_FAIL_MROM_VER;
            else {
                publishProgress(Utils.getString(R.string.prog_getting_roms));

                m.findRoms();
                res.multirom = m;
            }
        }

        publishProgress(Utils.getString(R.string.prog_check_recovery));

        Recovery rec = new Recovery();
        if(!rec.findRecoveryVersion(dev))
            res.code |= RES_NO_RECOVERY;
        else
            res.recovery = rec;

        publishProgress(Utils.getString(R.string.prog_check_kernel));

        res.kernel = new Kernel();
        res.kernel.findKexecHardboot(m != null ? m.getPath() + "busybox" : "");

        publishProgress(Utils.getString(R.string.prog_download_manifest));

        Manifest man = new Manifest();
        while(true) {
            if(man.downloadAndParse(dev, true)) {
                res.manifest = man;
                res.manifest.compareVersions(res.multirom, res.recovery, res.kernel);
            } else if(!dev.hasManifest()) {
                // device has no manifest and none was set in developer settings
                res.code |= RES_NO_MANIFEST;
            } else {
                if(man.hasCommand("RESET_MAN_URL")) {
                    res.manifest_reset_status = man.getCommandArg("RESET_MAN_URL");
                    SharedPreferences.Editor e = p.edit();
                    e.remove(SettingsActivity.DEV_MANIFEST_URL);
                    e.apply();
                    continue;
                }
                else if(man.getStatus() != null && !man.getStatus().equals("ok"))
                    res.statusText = man.getStatus();
                res.code |= RES_MANIFEST_FAIL;
            }

            break;
        }

        UpdateChecker.setVersions(res.device, res.multirom, res.recovery);

        return res;
    }

    protected void onProgressUpdate(String... progress) {
        m_progressText = progress[0];

        View l = m_layout.get();
        if(l != null) {
            TextView t = (TextView)l.findViewById(R.id.progress_text);
            t.setText(m_progressText);
        }
    }

    protected void onPostExecute(Result res) {
        m_res = res;
        applyResult();
        if(m_listener != null && m_res != null)
            m_listener.onStatusTaskFinished(res);
    }

    protected void applyResult() {
        View l = m_layout.get();

        if(l == null)
            return;

        if(m_res == null) {
            TextView t = (TextView)l.findViewById(R.id.progress_text);
            t.setText(m_progressText);
            return;
        }

        if(m_res.manifest_reset_status != null) {
            final Spanned text = Html.fromHtml("<b>MultiROM Manager:</b> " + m_res.manifest_reset_status);
            Toast.makeText(MgrApp.getAppContext(), text, Toast.LENGTH_LONG).show();
            m_res.manifest_reset_status = null;
        }

        View v = l.findViewById(R.id.progress_bar);
        v.setVisibility(View.GONE);
        v = l.findViewById(R.id.progress_text);
        v.setVisibility(View.GONE);

        TextView t = (TextView) l.findViewById(R.id.error_text);
        t.setText("");
        switch (m_res.code) {
            case RES_NO_SU:
                t.setText(R.string.no_su);
                t.setVisibility(View.VISIBLE);
                return;
            case RES_UNSUPPORTED: {
                String s = t.getResources().getString(R.string.unsupported, Build.DEVICE);
                t.setText(s);
                t.setVisibility(View.VISIBLE);
                return;
            }
        }

        if ((m_res.code & RES_FAIL_MROM_VER) != 0)
            t.append("\n" + t.getResources().getString(R.string.mrom_ver_fail));
        if ((m_res.code & RES_NO_MULTIROM) != 0)
            t.append("\n" + t.getResources().getString(R.string.no_multirom));
        if ((m_res.code & RES_NO_RECOVERY) != 0)
            t.append("\n" + t.getResources().getString(R.string.no_recovery));
        if ((m_res.code & RES_MANIFEST_FAIL) != 0) {
            if(m_res.statusText != null)
                t.append("\n" + m_res.statusText);
            else
                t.append("\n" + t.getResources().getString(R.string.manifest_fail));
        }

        if (t.getText().length() != 0)
            t.setVisibility(View.VISIBLE);

        String recovery_date = null;
        if (m_res.recovery != null) {
            recovery_date = Recovery.DISPLAY_FMT.format(m_res.recovery.getVersion());
        }

        String kexec_text = t.getResources().getString(
                m_res.kernel.hasKexec() ? R.string.has_kexec : R.string.no_kexec);

        String update = t.getResources().getString(R.string.update_available);

        Manifest man = m_res.manifest;

        t = (TextView) l.findViewById(R.id.info_text);
        Spanned s = Html.fromHtml(t.getResources().getString(R.string.status_text,
                m_res.multirom != null ? m_res.multirom.getVersion() : "N/A",
                man != null && man.hasMultiromUpdate() ? update : "",
                recovery_date != null ? recovery_date : "N/A",
                man != null && man.hasRecoveryUpdate() ? update : "",
                kexec_text));
        t.setText(s);
        t.setVisibility(View.VISIBLE);

        boolean canUninstall = (m_res.code == RES_OK && man.getUninstallerFile() != null);
        ImageButton b = (ImageButton) l.findViewById(R.id.uninstall_btn);
        b.setVisibility(canUninstall ? View.VISIBLE : View.GONE);

        if(man != null && man.hasCommand("NOTICE")) {
            String text = man.getCommandArg("NOTICE");
            if(text != null) {
                String hash = Utils.calculateChecksum(text.getBytes(), "MD5");
                SharedPreferences p = MgrApp.getPreferences();
                Set<String> shownHashes = p.getStringSet("shownNotices", new HashSet<String>());
                if(!shownHashes.contains(hash)) {
                    new AlertDialog.Builder(l.getContext())
                            .setTitle(R.string.notice)
                            .setCancelable(true)
                            .setMessage(text)
                            .setIcon(R.drawable.action_about)
                            .setPositiveButton(R.string.ok_nohtml, null)
                            .create()
                            .show();

                    shownHashes.add(hash);
                    SharedPreferences.Editor e = p.edit();
                    e.putStringSet("shownNotices", shownHashes);
                    e.apply();
                }
            }
        }
    }

    public class Result {
        public int code = RES_OK;
        public Recovery recovery = null;
        public MultiROM multirom = null;
        public Kernel kernel = null;
        public Manifest manifest = null;
        public String statusText = null;
        public Device device = null;
        public String manifest_reset_status = null;
    }

    private WeakReference<View> m_layout;
    private StatusAsyncTaskListener m_listener;
    private Result m_res;
    private String m_progressText;
}
