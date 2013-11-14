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

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

public class UbuntuChannel {

    public UbuntuChannel(String name, JSONObject c) throws JSONException {
        m_name = name;
        m_alias = c.optString("alias", null);

        JSONObject dev = c.getJSONObject("devices");
        Iterator itr = dev.keys();
        while(itr.hasNext()) {
            String code = (String)itr.next();
            JSONObject d = dev.getJSONObject(code);

            m_devices.put(code, d.getString("index"));
        }
    }

    public void addDuplicate(String name) {
        if(m_duplicates == null)
            m_duplicates = new ArrayList<String>();
        m_duplicates.add(name);
    }

    public boolean hasDevice(String name) {
        return m_devices.containsKey(name);
    }

    public String getDisplayName() {
        String res = m_name;
        if(m_duplicates != null) {
            res += " (";

            for(int i = 0; i < m_duplicates.size(); ++i)
                res += m_duplicates.get(i) + ", ";

            res = res.substring(0, res.length()-2) + ")";
        }
        return res;
    }

    public boolean loadDeviceImages(String device) throws Exception {
        String path = m_devices.get(device);
        if(path == null || path.isEmpty())
            throw new Exception("Device " + device + " was not found in this channel!");

        Log.d("UbuntuChannel", "Loading index " + path);

        ByteArrayOutputStream out = new ByteArrayOutputStream(32768);
        try {
            if(!Utils.downloadFile(UbuntuManifest.BASE_URL + path, out, null) || out.size() == 0)
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

        m_images = new TreeMap<Integer, UbuntuImage>();

        Object rawObject = new JSONTokener(out.toString()).nextValue();
        if(!(rawObject instanceof JSONObject)){
            Log.e("UbuntuChannel", "Malformed manifest format!");
            return false;
        }

        JSONArray images = ((JSONObject)rawObject).getJSONArray("images");
        for(int i = 0; i < images.length(); ++i) {
            JSONObject img = images.getJSONObject(i);

            // We only need full images because we do only clean install
            if(!img.getString("type").equals("full"))
                continue;

            UbuntuImage uimg = new UbuntuImage(img);
            m_images.put(uimg.version, uimg);
        }
        return true;
    }

    public void fillInstallFilesForVer(ArrayList<UbuntuFile> files, Integer version) {
        UbuntuImage img = m_images.get(version);
        files.addAll(img.files);
    }

    public Set<Integer> getImageVersions() {
        return m_images.keySet();
    }

    public String getRawName() { return m_name; }
    public String getAlias() { return m_alias; }
    public ArrayList<String> getDuplicates() { return m_duplicates; }

    private String m_name;
    private String m_alias;
    private HashMap<String, String> m_devices = new HashMap<String, String>();
    private ArrayList<String> m_duplicates = null;
    private TreeMap<Integer, UbuntuImage> m_images = null;
}
