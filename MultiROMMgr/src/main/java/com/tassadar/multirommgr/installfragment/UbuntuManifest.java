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

import android.content.SharedPreferences;
import android.util.Log;

import com.tassadar.multirommgr.Device;
import com.tassadar.multirommgr.MgrApp;
import com.tassadar.multirommgr.SettingsActivity;
import com.tassadar.multirommgr.SettingsFragment;
import com.tassadar.multirommgr.Utils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class UbuntuManifest {
    private static final String TAG = "MROMMgr::UbuntuManifest";

    public static final String DEFAULT_BASE_URL = "https://system-image.ubports.com";
    public static final String CHANNELS_PATH = "/channels.json";
    public static final String NO_FLAVOUR = "*none*";

    public boolean downloadAndParse(Device dev) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(8192);
        try {
            final String url = dev.getUbuntuBaseUrl() + CHANNELS_PATH;
            if(!Utils.downloadFile(url, out, null, true) || out.size() == 0)
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
            Object rawObject = new JSONTokener(out.toString()).nextValue();
            if(!(rawObject instanceof JSONObject)){
                Log.e(TAG, "Malformed manifest format!");
                return false;
            }

            JSONObject o = (JSONObject)rawObject;
            SharedPreferences pref = MgrApp.getPreferences();

            Iterator itr = o.keys();
            while(itr.hasNext()) {
                String name = (String)itr.next();
                JSONObject c = o.getJSONObject(name);

                // Skip hidden channels
                if(c.optBoolean("hidden", false) &&
                   !pref.getBoolean(SettingsFragment.UTOUCH_SHOW_HIDDEN, false)) {
                    continue;
                }

                addChannel(name, c);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        // Remove channels without the device we're currently running on and load images
        Iterator<Map.Entry<String, TreeMap<String,UbuntuChannel>>> f_itr = m_flavours.entrySet().iterator();
        while(f_itr.hasNext()) {
            Map<String, UbuntuChannel> channelMap = f_itr.next().getValue();
            Iterator<Map.Entry<String,UbuntuChannel>> c_itr = channelMap.entrySet().iterator();
            while(c_itr.hasNext()) {
                UbuntuChannel c = c_itr.next().getValue();

                // Devices like deb or tilapia won't be in
                // Ubuntu Touch manifests, yet the versions
                // for flo/grouper work fine - select those.
                String dev_name = dev.getName();
                if(dev.getUbuntuDevice() != ""){
                    dev_name = dev.getUbuntuDevice();
                    if (!c.hasDevice(dev_name)) {
                        c_itr.remove();
                        continue;
                    }
                }else {
                    if (!c.hasDevice(dev_name)) {
                        dev_name = dev.getBaseVariantName();

                        if (!c.hasDevice(dev_name)) {
                            c_itr.remove();
                            continue;
                        }
                    }
                }

                try {
                    if(!c.loadDeviceImages(dev_name, dev))
                        return false;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }

                // Remove channels with no images for our device
                if(!c.hasImages()) {
                    c_itr.remove();
                    continue;
                }

                Log.d(TAG, "Got channel: " + c.getDisplayName());
            }

            if(channelMap.isEmpty())
                f_itr.remove();
        }
        return true;
    }

    private void addChannel(String full_name, JSONObject channelObject) throws JSONException {
        String flavour_name, channel_name;
        int idx = full_name.indexOf('/');
        if(idx == -1) {
            flavour_name = NO_FLAVOUR;
            channel_name = full_name;
            full_name = NO_FLAVOUR + "/" + full_name;
        } else {
            flavour_name = full_name.substring(0, idx);
            channel_name = full_name.substring(idx+1);
        }

        UbuntuChannel channel = new UbuntuChannel(channel_name, full_name, channelObject);
        TreeMap<String, UbuntuChannel> channelMap = m_flavours.get(flavour_name);
        if(channelMap == null) {
            channelMap = new TreeMap<String, UbuntuChannel>();
            m_flavours.put(flavour_name, channelMap);
        }
        channelMap.put(channel_name, channel);
    }

    public UbuntuChannel findChannel(String full_name) {
        String flavour_name, channel_name;
        int idx = full_name.indexOf('/');
        if(idx == -1) {
            flavour_name = NO_FLAVOUR;
            channel_name = full_name;
        } else {
            flavour_name = full_name.substring(0, idx);
            channel_name = full_name.substring(idx+1);
        }

        TreeMap<String, UbuntuChannel> channelMap = m_flavours.get(flavour_name);
        if(channelMap != null)
            return channelMap.get(channel_name);
        return null;
    }

    public TreeMap<String, TreeMap<String, UbuntuChannel>> getFlavours() { return m_flavours; }

    private TreeMap<String, TreeMap<String, UbuntuChannel>> m_flavours =
            new TreeMap<String, TreeMap<String, UbuntuChannel>>();
}
