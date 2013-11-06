package com.tassadar.multirommgr;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.LinkedHashMap;

public class Manifest {
    private static final String manifestUrl = "http://83.240.110.90/multirom_manifest.json";

    public class InstallationFile {
        public String type;
        public String version;
        public String url;
        public String md5;
        public File destFile;
    }

    public boolean downloadAndParse(Device dev) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
        try {
            if(!Utils.downloadFile(manifestUrl, out, null))
                return false;
        } catch(IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                out.close();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }

        try {
            JSONObject o = (JSONObject)new JSONTokener(out.toString()).nextValue();
            m_status = o.getString("status");
            if(!m_status.equals("ok")) {
                Log.e("Manifest", "MultiROM manifest's status is \"" + m_status + "\"");
                return false;
            }

            JSONArray a = o.getJSONArray("devices");
            for(int i = 0; i < a.length(); ++i) {
                o = a.getJSONObject(i);
                if(o.getString("name").equals(dev.getName())) {
                    JSONObject utouch = o.optJSONObject("ubuntu_touch");
                    if(utouch != null) {
                        m_ubuntuReqMultiROM = utouch.getString("req_multirom");
                        m_ubuntuReqRecovery = utouch.getString("req_recovery");
                    }

                    getFileList(o.getJSONArray("files"));
                    m_dev = dev;
                    return true;
                }
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

            if(file.type.equals("multirom"))
                m_multirom = file;
            else if(file.type.equals("recovery")) {
                m_recovery = file;
            }
            else if(file.type.equals("kernel"))
                m_kernels.put(file.version, file);
        }
    }

    public void compareVersions(MultiROM multirom, Recovery recovery, Kernel kernel) {
        if(multirom != null) {
            int[] my = getMultiromVersions(multirom.getVersion());
            int[] upd = getMultiromVersions(m_multirom.version);
            m_multiromHasUpdate = (upd[0] > my[0]) || (upd[0] == my[0] && upd[1] > my[1]);
        } else
            m_multiromHasUpdate = true;

        if(recovery != null) {
            try {
                Date my = recovery.getVersion();
                Date upd = Recovery.VER_FMT.parse(m_recovery.version);
                m_recoveryHasUpdate = upd.after(my);
            } catch(ParseException e) {
                e.printStackTrace();
            }
        } else
            m_recoveryHasUpdate = true;

        m_kernelHasUpdate = kernel == null || !kernel.hasKexec();
    }

    private int[] getMultiromVersions(String ver) {
        int[] res = { 0, 0 };
        if(!Utils.isNumeric(ver.charAt(ver.length()-1))) {
            res[1] = (int)ver.charAt(ver.length()-1);
            ver = ver.substring(0, ver.length()-1);
        }
        try {
            res[0] = Integer.valueOf(ver);
        } catch(NumberFormatException e) {
            e.printStackTrace();
        }
        return res;
    }

    public boolean hasUbuntuReqMultiROM(MultiROM m) {
        if(m_ubuntuReqMultiROM == null)
            return true;

        int[] my = getMultiromVersions(m.getVersion());
        int[] req = getMultiromVersions(m_ubuntuReqMultiROM);
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

    public LinkedHashMap<String, InstallationFile> getKernels() {
        return m_kernels;
    }

    public InstallationFile getMultiromFile() { return m_multirom; }
    public InstallationFile getRecoveryFile() { return m_recovery; }
    public InstallationFile getKernelFile(String name) { return m_kernels.get(name); }

    public Device getDevice() { return m_dev; }
    public String getStatus() { return m_status; }

    private boolean m_multiromHasUpdate = false;
    private boolean m_recoveryHasUpdate = false;
    private boolean m_kernelHasUpdate = false;
    private InstallationFile m_multirom;
    private InstallationFile m_recovery;
    private LinkedHashMap<String, InstallationFile> m_kernels = new LinkedHashMap<String, InstallationFile>();
    private Device m_dev;
    private String m_status;
    private String m_ubuntuReqMultiROM;
    private String m_ubuntuReqRecovery;
}
