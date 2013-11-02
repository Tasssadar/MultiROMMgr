package com.tassadar.multirommgr;

public interface InstallListener {
    void onInstallLog(String str);
    void onInstallComplete(boolean success);
    void onProgressUpdate(int val, int max, boolean indeterminate, String text);
    void enableCancel(boolean enabled);
    void requestRecovery();
}
