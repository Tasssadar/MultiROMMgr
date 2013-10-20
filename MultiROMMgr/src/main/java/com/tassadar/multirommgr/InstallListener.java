package com.tassadar.multirommgr;

public interface InstallListener {
    void onInstallLog(String str);
    void onInstallComplete();
    void onProgressUpdate(int val, int max, boolean indeterminate, String text);
}
