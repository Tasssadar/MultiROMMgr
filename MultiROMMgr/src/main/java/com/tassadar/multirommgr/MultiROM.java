package com.tassadar.multirommgr;

import android.util.Log;

import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class MultiROM {

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

    public String getVersion() {
        return m_version;
    }
    public String getPath() { return m_path; }

    private String m_path;
    private String m_version;
}
