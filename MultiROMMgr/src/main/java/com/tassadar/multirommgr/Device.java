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

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class Device {
    public static Device load(String name) {
        Context ctx = MgrApp.getAppContext();
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
            JSONArray names;
            for(int i = 0; i < devices.length(); ++i) {
                o = devices.getJSONObject(i);

                names = o.getJSONArray("names");
                for(int x = 0; x < names.length(); ++x)
                    if(names.getString(x).equals(name))
                        return new Device(name, names.getString(0), o);
            }
            return null;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Device(String name, String base_variant, JSONObject info) throws JSONException {
        m_name = name;
        m_base_variant_name = base_variant;

        JSONArray a = info.getJSONArray("devices");

        Log.d("Device", "Loading device name: " + m_name);
        for(int i = 0; i < a.length(); ++i) {
            JSONObject d = a.getJSONObject(i);
            m_devices.put(d.getString("name"), d.getString("path"));
            Log.d("Device", "Loading device " + d.getString("name") + " path " + d.getString("path"));
        }

        m_supportsUbuntuTouch = info.getBoolean("ubuntu_touch");
    }

    public String getBootDev() { return m_devices.get("boot"); }
    public String getRecoveryDev() { return m_devices.get("recovery"); }
    public String getCacheDev() { return m_devices.get("cache"); }
    public String getName() { return m_name; }
    public String getBaseVariantName() { return m_base_variant_name; }
    public boolean supportsUbuntuTouch() { return m_supportsUbuntuTouch; }

    private String m_name;
    private String m_base_variant_name;
    private HashMap<String, String> m_devices = new HashMap<String, String>();
    private boolean m_supportsUbuntuTouch;
}
