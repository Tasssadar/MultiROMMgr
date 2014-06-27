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
    public static final DateFormat VER_FMT = new SimpleDateFormat("'mrom'yyyyMMdd-mm");
    public static final DateFormat DISPLAY_FMT = new SimpleDateFormat("yyyy-MM-dd (m)");

    public boolean findRecoveryVersion(Device dev) {
        String p = Utils.extractAsset("bbootimg");
        if(p == null) {
            Log.e("Recovery", "Failed to extract bbootimg!");
            return false;
        }

        if(Utils.isSELinuxEnforcing())
            Utils.chcon(Utils.CHCON_BLOCK_ACCESS, p);

        List<String> out = Shell.SU.run(p + " -j " + dev.getRecoveryDev());

        if(Utils.isSELinuxEnforcing())
            Utils.chcon(Utils.CHCON_ORIGINAL, p);

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

            m_version = VER_FMT.parse(name);

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
        return VER_FMT.format(m_version);
    }

    public Date getVersion() {
        return m_version;
    }

    private Date m_version;
}
