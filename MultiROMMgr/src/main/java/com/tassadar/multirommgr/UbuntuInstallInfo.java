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
