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

import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class Kernel {
    public Kernel() {
        m_hasKexec = false;
    }

    public boolean findKexecHardboot() {
        String b = Utils.extractAsset("busybox");
        if(b == null) {
            Log.e("Kernel", "Failed to extract busybox!");
            return false;
        }
        return findKexecHardboot(b);
    }

    public boolean findKexecHardboot(String busybox) {
        List<String> out = Shell.SU.run(
                "if [ -f /proc/config.gz ] && [ -e \"" + busybox + "\" ]; then" +
                "    " + busybox + " zcat /proc/config.gz | grep -q 'CONFIG_KEXEC_HARDBOOT=y' && echo has_kexec && exit 0;" +
                "fi;" +
                "if [ -f /proc/atags ] || [ -d /proc/device-tree ] || [ \"$(grep mrom_kexecd=1 /proc/cmdline)\" ]; then" +
                "    echo has_kexec;" +
                "    exit 0;" +
                "fi;");

        if(out == null || out.isEmpty())
            return false;

        if(out.get(0).equals("has_kexec")) {
            m_hasKexec = true;
            return true;
        }
        return false;
    }

    public boolean hasKexec() {
        return m_hasKexec;
    }

    private boolean m_hasKexec;
}
