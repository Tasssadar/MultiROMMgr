package com.tassadar.multirommgr;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class Recovery {
    public static final DateFormat RECOVERY_VER_FORMAT = new SimpleDateFormat("'mrom'yyyyMMdd-mm");

    public boolean findRecoveryVersion(Device dev) {
        String p = Utils.extractAsset("bbootimg");
        if(p == null) {
            Log.e("Recovery", "Failed to extract bbootimg!");
            return false;
        }

        List<String> out = Shell.SU.run(p + " -j " + dev.getRecoveryDev());
        if(out == null || out.isEmpty())
            return false;

        StringBuilder b = new StringBuilder();
        for(int i = 0; i < out.size(); ++i)
            b.append(out.get(i));

        try {
            JSONObject o = (JSONObject)new JSONTokener(b.toString()).nextValue();
            if(o == null)
                return false;

            o = o.getJSONObject("boot_img_hdr");
            String name = o.getString("name");

            m_version = RECOVERY_VER_FORMAT.parse(name);

            Log.d("Recovery", "Got recovery version " + m_version.toString());
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getVersionString() {
        return RECOVERY_VER_FORMAT.format(m_version);
    }

    public Date getVersion() {
        return m_version;
    }

    private Date m_version;
}
