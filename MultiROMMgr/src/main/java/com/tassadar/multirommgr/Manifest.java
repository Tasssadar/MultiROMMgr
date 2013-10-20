package com.tassadar.multirommgr;

import android.os.Build;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class Manifest {
    private static final String manifestUrl = "http://83.240.110.90/multirom_manifest.json";

    public boolean downloadAndParse(String dev) {
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
            if(!o.getString("status").equals("ok")) {
                Log.e("Manifest", "MultiROM manifest's status is \"" + o.getString("status") + "\"");
                return false;
            }

            JSONArray a = o.getJSONArray("devices");
            for(int i = 0; i < a.length(); ++i) {
                o = a.getJSONObject(i);
                if(o.getString("name").equals(dev)) {
                    setFromJSON(o);
                    return true;
                }
            }
            return false;
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void setFromJSON(JSONObject obj) throws JSONException, ParseException {
        m_multirom_ver = obj.getString("multirom_ver");
        m_multirom_url = obj.getString("multirom_url");
        m_recovery_ver = Recovery.RECOVERY_VER_FORMAT.parse(obj.getString("recovery_ver"));
        m_recovery_url = obj.getString("recovery_url");

        JSONArray k = obj.getJSONArray("kernels");
        for(int i = 0; i < k.length(); ++i) {
            obj = k.getJSONObject(i);
            m_kernels.put(obj.getString("name"), obj.getString("url"));
        }
    }

    public void compareVersions(MultiROM multirom, Recovery recovery, Kernel kernel) {
        if(multirom != null) {
            int[] my = getMultiromVersions(multirom.getVersion());
            int[] upd = getMultiromVersions(m_multirom_ver);
            m_multiromHasUpdate = (upd[0] > my[0]) || (upd[0] == my[0] && upd[1] > my[1]);
        } else
            m_multiromHasUpdate = true;

        if(recovery != null) {
            Date my = recovery.getVersion();
            m_recoveryHasUpdate = m_recovery_ver.after(my);
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
        return m_multirom_ver;
    }

    public Date getRecoveryVersion() {
        return m_recovery_ver;
    }

    public LinkedHashMap<String, String> getKernels() {
        return m_kernels;
    }

    private boolean m_multiromHasUpdate = false;
    private boolean m_recoveryHasUpdate = false;
    private boolean m_kernelHasUpdate = false;
    private String m_multirom_ver;
    private String m_multirom_url;
    private Date m_recovery_ver;
    private String m_recovery_url;
    private LinkedHashMap<String, String> m_kernels = new LinkedHashMap<String, String>();
}
