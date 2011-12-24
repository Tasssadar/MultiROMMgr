package com.tassadar.multirommgr;

import java.io.File;

public class Storage
{
    private static final String MOUNT_LOC[] = new String[] { "/sd-ext", "/system/sd" }; 

    public static String getSDExt()
    {
        String mount = MultiROMMgrActivity.runRootCommand("mount");
        if(mount == null)
            return null;
        mount = mount.replaceAll(" on ", " ");
        String mount_sp[] = mount.split("\n");
        for(int i = 0; i < mount_sp.length; ++i)
        {
            if(mount_sp[i].startsWith(("/dev/block/mmcblk0p2")) || mount_sp[i].contains("/sd-ext") ||    
                mount_sp[i].contains("/sdroot"))
            {
                return mount_sp[i].split(" ")[1];
            }
        }
        return MountSD();
    }
    
    private static String MountSD()
    {
        String res = MultiROMMgrActivity.runRootCommand("mknod /dev/block/mmcblk0p97 b 179 2");
        if(res == null || !res.equals(""))
            return null;

        String folder = null;
        for(int i = 0; i < MOUNT_LOC.length; ++i)
        {
            File f = new File(MOUNT_LOC[i]);
            if(f.exists() && f.isDirectory())
                folder = MOUNT_LOC[i];
        }
        if(folder == null)
            return null;
        
        res = MultiROMMgrActivity.runRootCommand("mount /dev/block/mmcblk0p97 " + folder + " -t auto");
        if(res == null || !res.equals(""))
            return null;
        return folder;
    }
}