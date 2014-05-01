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
import com.tassadar.multirommgr.Utils;

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

    public UbuntuChannel(String channelName, String fullName, JSONObject c) throws JSONException {
        m_name = channelName;
        m_fullName = fullName;
        m_alias = c.optString("alias", null);

        JSONObject dev = c.getJSONObject("devices");
        Iterator itr = dev.keys();
        while(itr.hasNext()) {
            String code = (String)itr.next();
            JSONObject d = dev.getJSONObject(code);

            m_devices.put(code, d.getString("index"));
        }
    }

    public boolean hasDevice(String name) {
        return m_devices.containsKey(name);
    }

    public String getDisplayName() {
        StringBuilder b = new StringBuilder(m_name);
        if(m_alias != null && !m_alias.equals(m_fullName)) {
            final String flavour = getFlavour();
            b.append("<br><small><font color=\"#585858\">(alias of ");
            if(flavour.equals(getFlavour(m_alias)) && !flavour.equals(UbuntuManifest.NO_FLAVOUR)) {
                b.append(m_alias.substring(m_alias.indexOf('/') + 1));
            } else {
                b.append(m_alias);
            }
            b.append(")</font></small>");
        }
        return b.toString();
    }

    public boolean loadDeviceImages(String device_name, Device dev) throws Exception {
        String path = m_devices.get(device_name);
        if(path == null || path.isEmpty())
            throw new Exception("Device " + device_name + " was not found in this channel!");

        Log.d("UbuntuChannel", "Loading index " + path);

        ByteArrayOutputStream out = new ByteArrayOutputStream(32768);
        try {
            if(!Utils.downloadFile(dev.getUbuntuBaseUrl() + path, out, null, true) || out.size() == 0)
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

    public String getFlavour() {
        return UbuntuChannel.getFlavour(m_fullName);
    }

    public static String getFlavour(String channelName) {
        int idx = channelName.indexOf('/');
        if(idx != -1) {
            return channelName.substring(0, idx);
        } else {
            return UbuntuManifest.NO_FLAVOUR;
        }
    }

    public Set<Integer> getImageVersions() {
        return m_images.keySet();
    }

    public String getRawName() { return m_name; }
    public String getFullName() { return m_fullName; }
    public String getAlias() { return m_alias; }
    public boolean hasImages() { return !m_images.isEmpty(); }

    private String m_name;
    private String m_fullName;
    private String m_alias;
    private HashMap<String, String> m_devices = new HashMap<String, String>();
    private ArrayList<String> m_duplicates = null;
    private TreeMap<Integer, UbuntuImage> m_images = null;
}
