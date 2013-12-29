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

import org.json.JSONException;
import org.json.JSONObject;

public class UbuntuFile {

    public UbuntuFile(JSONObject file) throws JSONException {
        checksum = file.getString("checksum");
        order = file.getInt("order");
        path = file.getString("path");
        signature = file.getString("signature");
        size = file.getInt("size");
    }

    public UbuntuFile(String keyringPath,int order) {
        this.order = order;
        this.path = keyringPath;
        this.signature = keyringPath + ".asc";
    }

    public String checksum;
    public int order;
    public String path;
    public String signature;
    public int size;
}
