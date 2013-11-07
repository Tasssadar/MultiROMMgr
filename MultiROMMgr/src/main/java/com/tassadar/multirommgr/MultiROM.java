package com.tassadar.multirommgr;

import android.util.Log;

import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class MultiROM {

    private static final int MAX_ROM_NAME = 26;
    private static final String UTOUCH_ROM_INFO = "ubuntu_touch.txt";

    public boolean findMultiROMDir() {
        List<String> out = Shell.SU.run(
                "folders=\"/data/media/0/multirom/ /data/media/multirom/\";" +
                "for f in $folders; do" +
                "    if [ -d \"$f\" ]; then" +
                "        echo \"$f\";" +
                "        exit 0;" +
                "    fi;" +
                "done;");

        if (out == null || out.isEmpty())
            return false;

        m_path = out.get(0);
        Log.d("MultiROM", "Found in path " + m_path);
        return true;
    }

    public boolean findVersion() {
        List<String> out = Shell.SU.run(m_path + "multirom -v");
        if (out == null || out.isEmpty())
            return false;

        m_version = out.get(0);
        Log.d("MultiROM", "MultiROM version: " + m_version);
        return true;
    }

    public String getNewRomFolder(String base) {
        if(base.length() > MAX_ROM_NAME)
            base = base.substring(0, MAX_ROM_NAME);

        List<String> out = Shell.SU.run(String.format(
                "cd \"%s/roms\"; " +
                "rom=\"%s\"; c=0; " +
                "while [ $c -lt 10 ]; do" +
                "    if [ ! -d \"$rom\" ]; then" +
                "        echo $(pwd)/$rom;" +
                "        exit 0;" +
                "    fi;" +
                "    c=$(($c+1));" +
                "    rom=\"${rom%%?}$c\";" +
                "done",
                m_path, base));

        if (out == null || out.isEmpty())
            return null;
        return out.get(0);
    }

    public boolean initUbuntuDir(String path) {
        List<String> out = Shell.SU.run(String.format(
                "mkdir -p \"%s\" && cd \"%s\" && " +
                "mkdir system data cache && " +
                "mkdir cache/recovery && " +
                "cat ../../infos/%s > rom_info.txt &&" +
                "echo success",
                path, path, UTOUCH_ROM_INFO));

        if (out == null || out.isEmpty() || !out.get(0).equals("success"))
            return false;
        return true;
    }

    public int getFreeSpaceMB() {
        String bb = Utils.extractAsset("busybox");

        List<String> out = Shell.SU.run("\"%s\" df -Pm \"%s\"", bb, m_path);
        if (out == null || out.size() < 2 || !out.get(0).startsWith("Filesystem"))
            return -1;

        String l = out.get(1);
        if(!l.startsWith("/dev"))
            return -1;

        String[] tokens = l.split("[ \t]+");
        if(tokens.length != 6)
            return -1;

        try {
            return Integer.parseInt(tokens[3]);
        } catch(NumberFormatException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public String getVersion() {
        return m_version;
    }
    public String getPath() { return m_path; }

    private String m_path;
    private String m_version;
}
