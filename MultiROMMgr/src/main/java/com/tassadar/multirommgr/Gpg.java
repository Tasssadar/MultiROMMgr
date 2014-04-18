package com.tassadar.multirommgr;

import android.content.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class Gpg {
    public static final String RING_MULTIROM = "multirom";

    public Gpg(String keyring) throws IOException {
        m_gpg_bin = Utils.extractAsset("gpg");
        if(m_gpg_bin == null)
            throw new FileNotFoundException("Couldn't extract gpg binary!");

        m_gpg_home = extractKeyring(keyring);
    }

    private String extractKeyring(String name) throws IOException {
        Context ctx = MgrApp.getAppContext();

        File gpg_home = new File(ctx.getCacheDir() + "/gpg_keyrings/" + name);
        File f = new File(gpg_home, "pubring.gpg");
        if(f.exists())
            return gpg_home.getAbsolutePath();

        InputStream in = null;
        FileOutputStream out = null;
        try {
            gpg_home.mkdirs();

            in = ctx.getAssets().open(name + ".gpg");
            out = new FileOutputStream(f);

            byte[] buff = new byte[4096];
            for(int len; (len = in.read(buff)) != -1; )
                out.write(buff, 0, len);

            return gpg_home.getAbsolutePath();
        } catch (IOException e) {
            throw e;
        } finally {
            if(in != null)
                try { in.close(); } catch(IOException e) { }
            if(out != null)
                try { out.close(); } catch(IOException e) { }
        }
    }

    public boolean verifyFile(String path) {
        return verifyFile(path, path + ".asc");
    }

    public boolean verifyFile(String path, String signature) {
        List<String> out = Shell.SU.run("GNUPGHOME='%s' %s --verify %s %s >/dev/null 2>&1 && echo \'success\'",
                m_gpg_home, m_gpg_bin, signature, path);
        return out.size() >= 1 && out.get(0).equals("success");
    }

    private String m_gpg_bin;
    private String m_gpg_home;
}
