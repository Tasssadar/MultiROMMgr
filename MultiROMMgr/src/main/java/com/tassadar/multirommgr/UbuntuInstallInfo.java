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

import java.util.ArrayList;

public class UbuntuInstallInfo {

    public UbuntuInstallInfo() {
        addDefaultKeyrings();
    }

    private void addDefaultKeyrings() {
        keyrings.add(new UbuntuFile("/gpg/image-master.tar.xz", 0));
        keyrings.add(new UbuntuFile("/gpg/image-signing.tar.xz", 1));
    }

    public ArrayList<UbuntuFile> buildDownloadList() {
        ArrayList<UbuntuFile> res = new ArrayList<UbuntuFile>();
        res.addAll(installFiles);
        res.addAll(keyrings);
        return res;
    }

    public ArrayList<UbuntuFile> keyrings = new ArrayList<UbuntuFile>();
    public ArrayList<UbuntuFile> installFiles = new ArrayList<UbuntuFile>();
    public String channelName;
}
