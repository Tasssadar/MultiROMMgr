package com.tassadar.multirommgr;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Utils {

    static String extractAsset(String name) {
        Context ctx = MultiROMMgrApplication.getAppContext();

        String path = "/data/data/" + ctx.getPackageName() + "/" + name;
        File f = new File(path);
        if(f.exists())
            return path;

        try {
            InputStream in = ctx.getAssets().open(name);
            FileOutputStream out = new FileOutputStream(path);

            byte[] buff = new byte[4096];
            for(int len; (len = in.read(buff)) != -1; )
                out.write(buff, 0, len);

            out.close();
            in.close();

            f = new File(path);
            f.setExecutable(true);
            return path;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
