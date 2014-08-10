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

import android.content.SharedPreferences;
import android.util.Log;

import com.tassadar.multirommgr.installfragment.Changelog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.LinkedHashMap;

public class Manifest {
    public static final String DEFAULT_URL = "http://tasemnice.eu/multirom/manifest.json";

    public class InstallationFile {
        public String type;
        public String version;
        public String url;
        public String md5;
        public long size;
        public JSONObject extra;

        public File destFile;
    }

    public boolean downloadAndParse(Device dev, boolean check_gpg) {
        FileOutputStream out_man = null;
        FileOutputStream out_sign = null;
        String man_filename = MgrApp.getAppContext().getCacheDir() + "/manifest.json";
        String sign_filename = MgrApp.getAppContext().getCacheDir() + "/manifest.json.asc";
        try {
            out_man = new FileOutputStream(man_filename);
            out_sign = new FileOutputStream(sign_filename);
            SharedPreferences p = MgrApp.getPreferences();
            String url = p.getString(SettingsActivity.DEV_MANIFEST_URL, dev.getDefaultManifestUrl());
            if(!Utils.downloadFile(url, out_man, null, true))
                return false;
            if(dev.checkGpgSignatures() && !Utils.downloadFile(url + ".asc", out_sign, null, true))
                return false;
        } catch(IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            Utils.close(out_man);
            Utils.close(out_sign);
        }

        if(check_gpg && dev.checkGpgSignatures()) {
            try {
                Gpg gpg = new Gpg(Gpg.RING_MULTIROM);
                if (!gpg.verifyFile(man_filename, sign_filename)) {
                    Log.e("Manifest", "Manifest signature verification failed!");
                    return false;
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("Manifest", "Manifest signature verification failed!");
                return false;
            }
        }

        String manifest = Utils.readFile(man_filename);
        if(manifest == null)
            return false;

        try {
            Object rawObject = new JSONTokener(manifest).nextValue();
            if(!(rawObject instanceof JSONObject)){
                Log.e("Manifest", "Malformed manifest format!");
                return false;
            }

            JSONObject o = (JSONObject)rawObject;
            m_commands = o.optString("commands", "").split("\\|");
            m_status = o.getString("status");
            if(!m_status.equals("ok")) {
                Log.e("Manifest", "MultiROM manifest's status is \"" + m_status + "\"");
                return false;
            }

            if(dev.checkGpgSignatures())
                m_gpgData = o.optBoolean("gpg", false);

            JSONArray a = o.getJSONArray("devices");
            for(int i = 0; i < a.length(); ++i) {
                o = a.getJSONObject(i);

                if(!o.getString("name").equals(dev.getName()))
                    continue;

                JSONObject utouch = o.optJSONObject("ubuntu_touch");
                if(utouch != null) {
                    m_ubuntuReqMultiROM = utouch.getString("req_multirom");
                    m_ubuntuReqRecovery = utouch.getString("req_recovery");
                }

                JSONArray changelogs = o.optJSONArray("changelogs");
                if(changelogs != null) {
                    m_changelogs = new Changelog[changelogs.length()];
                    for(int x = 0; x < changelogs.length(); ++x)
                        m_changelogs[x] = new Changelog(changelogs.getJSONObject(x));
                }

                getFileList(o.getJSONArray("files"));
                return true;
            }
            return false;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void getFileList(JSONArray files) throws JSONException {
        for(int i = 0; i < files.length(); ++i) {
            InstallationFile file = new InstallationFile();
            JSONObject f = files.getJSONObject(i);

            file.type = f.getString("type");
            file.version = f.getString("version");
            file.url = f.getString("url");
            file.md5 = f.getString("md5");
            file.size = f.getLong("size");
            file.extra = f.optJSONObject("extra");

            if (file.type.equals("multirom")) {
                m_multirom = file;
            } else if(file.type.equals("recovery")) {
                m_recovery = file;
            } else if(file.type.equals("uninstaller")) {
                m_uninstaller = file;
            } else if(file.type.equals("kernel")) {
                m_kernels.put(file.version, file);
            }
        }
    }

    public void compareVersions(MultiROM m, Recovery r, Kernel kernel) {
        compareVersions(m != null ? m.getVersion() : null,
                r != null ? r.getVersionString() : null,
                kernel);
    }

    public void compareVersions(String multirom, String recovery, Kernel kernel) {
        if(multirom != null) {
            int[] my = MultiROM.parseMultiRomVersions(multirom);
            int[] upd = MultiROM.parseMultiRomVersions(m_multirom.version);
            m_multiromHasUpdate = (upd[0] > my[0]) || (upd[0] == my[0] && upd[1] > my[1]);
        } else
            m_multiromHasUpdate = true;

        if(recovery != null) {
            try {
                Date my = Recovery.VER_FMT.parse(recovery);
                Date upd = Recovery.VER_FMT.parse(m_recovery.version);
                m_recoveryHasUpdate = upd.after(my);
            } catch(ParseException e) {
                e.printStackTrace();
            }
        } else
            m_recoveryHasUpdate = true;

        m_kernelHasUpdate = kernel == null || !kernel.hasKexec();
    }

    public boolean hasUbuntuReqMultiROM(MultiROM m) {
        if(m_ubuntuReqMultiROM == null)
            return true;

        int[] my = MultiROM.parseMultiRomVersions(m.getVersion());
        int[] req = MultiROM.parseMultiRomVersions(m_ubuntuReqMultiROM);
        return (my[0] > req[0]) || (my[0] == req[0] && my[1] >= req[1]);
    }

    public boolean hasUbuntuReqRecovery(Recovery r) {
        if(m_ubuntuReqRecovery == null)
            return true;

        try {
            Date my = r.getVersion();
            Date req = Recovery.VER_FMT.parse(m_ubuntuReqRecovery);
            return my.compareTo(req) >= 0;
        } catch(ParseException e) {
            e.printStackTrace();
        }
        return true;
    }

    public String getUbuntuReqMultiROM() {
        return m_ubuntuReqMultiROM;
    }

    public Date getUbuntuReqRecovery() {
        try {
            return Recovery.VER_FMT.parse(m_ubuntuReqRecovery);
        } catch(ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean hasMultiromUpdate() {
        return m_multiromHasUpdate;
    }

    public boolean hasRecoveryUpdate() {
        return m_recoveryHasUpdate;
    }

    public boolean hasKernelUpdate() {
        return m_kernelHasUpdate;
    }

    public String getMultiromVersion() {
        return m_multirom.version;
    }

    public Date getRecoveryVersion() {
        try {
            return Recovery.VER_FMT.parse(m_recovery.version);
        } catch(ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean hasCommand(String cmd) {
        for(String c : m_commands) {
            if(c.equals(cmd) || c.startsWith(cmd + "="))
                return true;
        }
        return false;
    }

    public String getCommandArg(String cmd) {
        for(String c : m_commands) {
            if(c.startsWith(cmd + "="))
                return c.substring(cmd.length()+1);
        }
        return null;
    }

    public LinkedHashMap<String, InstallationFile> getKernels() {
        return m_kernels;
    }

    public InstallationFile getMultiromFile() { return m_multirom; }
    public InstallationFile getRecoveryFile() { return m_recovery; }
    public InstallationFile getUninstallerFile() { return m_uninstaller; }
    public InstallationFile getKernelFile(String name) { return m_kernels.get(name); }

    public String getStatus() { return m_status; }
    public String[] getCommands() { return m_commands; }
    public Changelog[] getChangelogs() { return m_changelogs; }

    public boolean checkDataGpg() { return m_gpgData; }

    private boolean m_multiromHasUpdate = false;
    private boolean m_recoveryHasUpdate = false;
    private boolean m_kernelHasUpdate = false;
    private InstallationFile m_multirom;
    private InstallationFile m_recovery;
    private InstallationFile m_uninstaller;
    private LinkedHashMap<String, InstallationFile> m_kernels = new LinkedHashMap<String, InstallationFile>();
    private String m_status;
    private String m_ubuntuReqMultiROM;
    private String m_ubuntuReqRecovery;
    private Changelog[] m_changelogs;
    private boolean m_gpgData = false;
    private String[] m_commands = new String[0];
}
