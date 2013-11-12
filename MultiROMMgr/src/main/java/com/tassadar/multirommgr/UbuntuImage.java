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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class UbuntuImage implements Comparator<UbuntuFile> {
    public UbuntuImage(JSONObject img) throws JSONException {
        version = img.getInt("version");
        description = img.getString("description");

        JSONArray f = img.getJSONArray("files");
        for(int i = 0; i < f.length(); ++i) {
            JSONObject file = f.getJSONObject(i);
            files.add(new UbuntuFile(file));
        }

        Collections.sort(files, this);
    }

    @Override
    public int compare(UbuntuFile f1, UbuntuFile f2) {
        return f1.order - f2.order;
    }

    public int version;
    public String description;
    public ArrayList<UbuntuFile> files = new ArrayList<UbuntuFile>();
}
