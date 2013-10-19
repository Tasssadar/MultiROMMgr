package com.tassadar.multirommgr;

import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class Kernel {
    public Kernel() {
        m_hasKexec = false;
    }

    public boolean findKexecHardboot(String busybox) {
        List<String> out = Shell.SU.run(
                "if [ -f /proc/config.gz ] && [ -e \"" + busybox + "\" ]; then" +
                "    " + busybox + " zcat /proc/config.gz | grep -q 'CONFIG_KEXEC_HARDBOOT=y' && echo has_kexec && exit 0;" +
                "fi;" +
                "if [ -f /proc/atags ] || [ \"$(grep mrom_kexecd=1 /proc/cmdline)\" ]; then" +
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
