package com.tassadar.multirommgr;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;

public class Device {
    public static Device load(String name) {
        Context ctx = MultiROMMgrApplication.getAppContext();
        StringBuilder b = new StringBuilder();

        name = name.toLowerCase();

        try {
            InputStream in = ctx.getAssets().open("devices.json");
            byte[] buff = new byte[1024];
            for(int len; (len = in.read(buff)) != -1; )
                b.append(new String(buff, 0, len));
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        try {
            JSONObject o = (JSONObject)new JSONTokener(b.toString()).nextValue();
            JSONArray devices = o.getJSONArray("devices");
            for(int i = 0; i < devices.length(); ++i) {
                o = devices.getJSONObject(i);
                if(o.getString("name").equals(name))
                    return new Device(o);
            }
            return null;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Device(JSONObject info) throws JSONException {
        name = info.getString("name");
        m_dev_boot = info.getString("dev_boot");
        m_dev_recovery = info.getString("dev_recovery");

        Log.d("Device", "Loaded: " + name + " " + m_dev_boot + " " + m_dev_recovery);
    }

    public String getBootDev() {
        return m_dev_boot;
    }

    public String getRecoveryDev() {
        return m_dev_recovery;
    }

    private String name;
    private String m_dev_boot;
    private String m_dev_recovery;
}
