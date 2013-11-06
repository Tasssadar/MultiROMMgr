package com.tassadar.multirommgr;

import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class UbuntuInstallTask extends InstallAsyncTask  {

    private static final String DOWN_DIR = "/Download/UbuntuTouch";

    public UbuntuInstallTask(UbuntuInstallInfo info, MultiROM multirom) {
        m_info = info;
        m_multirom = multirom;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        m_listener.onProgressUpdate(0, 0, true, "Preparing downloads...");
        m_listener.onInstallLog("Preparing downloads...<br>");

        File destDir = new File(Environment.getExternalStorageDirectory(), DOWN_DIR);
        destDir.mkdirs();
        String dest = destDir.toString();

       String suDestDir = findSUDestDir(destDir);

        if(suDestDir == null) {
            m_listener.onInstallLog("Failed to find download directory as SU.<br>");
            m_listener.onInstallComplete(false);
            return null;
        }

        Log.d("UbuntuInstallTask", "Using download directory: " + dest);
        Log.d("UbuntuInstallTask", "Using SU download directory: " + suDestDir);

        ArrayList<UbuntuFile> files = m_info.buildDownloadList();

        for(int i = 0; i < files.size(); ++i) {
            UbuntuFile f = files.get(i);

            if(!downloadFile(destDir, UbuntuManifest.BASE_URL + f.path, f.checksum))
                return null;

            if(f.signature != null && !downloadFile(destDir, UbuntuManifest.BASE_URL + f.signature, null))
                return null;
        }

        m_listener.onProgressUpdate(0, 0, true, "Installing Ubuntu Touch...");
        m_listener.enableCancel(false);

        String romPath = m_multirom.getNewRomFolder("utouch_" + m_info.channelName);
        if(romPath == null) {
            m_listener.onInstallLog("Failed to create ROM folder!<br>");
            m_listener.onInstallComplete(false);
            return null;
        }
        m_listener.onInstallLog("<br>Installing as ROM <font color=\"yellow\">" +
                Utils.getFilenameFromUrl(romPath) + "</font><br>");

        if(!m_multirom.initUbuntuDir(romPath)) {
            m_listener.onInstallLog("Failed to initialize ROM folder!<br>");
            Shell.SU.run("rm -r \"%s\"", romPath);
            m_listener.onInstallComplete(false);
            return null;
        }

        if(!buildCommandFile(romPath + "/cache/recovery/ubuntu_command")) {
            Shell.SU.run("rm -r \"%s\"", romPath);
            m_listener.onInstallComplete(false);
            return null;
        }

        if(!copyFiles(suDestDir, romPath + "/cache/recovery", files)) {
            Shell.SU.run("rm -r \"%s\"", romPath);
            m_listener.onInstallComplete(false);
            return null;
        }

        m_listener.requestRecovery(true);
        m_listener.onInstallComplete(true);
        return null;
    }

    private boolean downloadFile(File destDir, String url, String checksum) {
        String filename = Utils.getFilenameFromUrl(url);
        if(filename == null || filename.isEmpty()) {
            m_listener.onInstallLog("Invalid url " + url);
            m_listener.onInstallComplete(false);
            return false;
        }

        File destFile = new File(destDir, filename);
        if(destFile.exists() && checksum != null) {
            m_listener.onInstallLog("Checking file " + Utils.trim(filename, 40) + "... ");
            String sha256 = Utils.calculateSHA256(destFile);
            if(checksum.equals(sha256)) {
                m_listener.onInstallLog("<font color=\"green\">ok, skipping.</font><br>");
                return true;
            } else {
                m_listener.onInstallLog("<font color=\"#FF9900\">failed, re-downloading.</font><br>");
            }
        }

        if(!downloadFile(url, destFile)) {
            if(!m_canceled)
                m_listener.onInstallComplete(false);
            return false;
        }

        if(checksum != null && !checksum.isEmpty()) {
            m_listener.onInstallLog("Checking file " + m_downFilename + "... ");
            String sha256 = Utils.calculateSHA256(destFile);
            if(checksum.equals(sha256))
                m_listener.onInstallLog("<font color=\"green\">ok</font><br>");
            else {
                m_listener.onInstallLog("<font color=\"red\">FAILED!</font><br>");
                m_listener.onInstallComplete(false);
                return false;
            }
        }
        return true;
    }

    private boolean copyFiles(String src, String dest, ArrayList<UbuntuFile> files) {
        String bb = Utils.extractAsset("busybox");
        if(bb == null) {
            m_listener.onInstallLog("Failed to extract busybox!");
            return false;
        }

        for(int i = 0; i < files.size(); ++i) {
            UbuntuFile f = files.get(i);
            String filename = Utils.getFilenameFromUrl(f.path);
            m_listener.onInstallLog("Copying file " + Utils.trim(filename, 40) + "...");

            if(!copyFile(src + "/" + filename, dest, bb)) {
                m_listener.onInstallLog("<br>Failed to copy file " + filename + "<br>");
                return false;
            }

            filename = Utils.getFilenameFromUrl(f.signature);
            if(!copyFile(src + "/" + filename, dest, bb)) {
                m_listener.onInstallLog("<br>Failed to copy file " + filename + "<br>");
                return false;
            }
            m_listener.onInstallLog("<font color=\"green\">OK</font><br>");
        }

        SharedPreferences pref = MultiROMMgrApplication.getPreferences();
        if(pref.getBoolean(SettingsActivity.UTOUCH_DELETE_FILES, false)) {
            m_listener.onInstallLog("Deleting used files...<br>");
            for(int i = 0; i < files.size(); ++i) {
                UbuntuFile f = files.get(i);

                File file = new File(src, Utils.getFilenameFromUrl(f.path));
                file.delete();
                file = new File(src, Utils.getFilenameFromUrl(f.signature));
                file.delete();
            }
        }

        return true;
    }

    private boolean copyFile(final String src, final String dst, final String bb) {
        List<String> out = Shell.SU.run("%s cp \"%s\" \"%s/\" && echo success",
                bb, src, dst);
        return out != null && !out.isEmpty() && out.get(0).equals("success");
    }

    private String findSUDestDir(File destDir) {
        File tmp = new File(destDir, "ut_test_file");
        try {
            tmp.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        List<String> out = Shell.SU.run(
            "paths=\"" +
                "/sdcard/%s /storage/emulated/0/%s /storage/emulated/legacy/%s" +
                "/data/media/0/%s /data/media/%s\";" +
            "for d in $paths; do" +
            "    if [ -f \"$d/ut_test_file\" ]; then " +
            "        echo $d; exit 0;" +
            "    fi;" +
            "done;",
            DOWN_DIR, DOWN_DIR, DOWN_DIR, DOWN_DIR, DOWN_DIR);

        if(out == null || out.isEmpty())
            return null;
        return out.get(0);
    }

    private boolean buildCommandFile(String dest) {
        ArrayList<String> cmd = new ArrayList<String>();
        cmd.add("echo \"format data\" > \"" + dest + "\"");
        cmd.add("echo \"format system\" >> \"" + dest + "\"");

        for(int i = 0; i < m_info.keyrings.size(); ++i)  {
            UbuntuFile f = m_info.keyrings.get(i);
            cmd.add(String.format(
                    "echo \"load_keyring %s %s\" >> \"%s\"",
                    Utils.getFilenameFromUrl(f.path), Utils.getFilenameFromUrl(f.signature), dest));
        }

        cmd.add("echo \"mount system\" >> \"" + dest + "\"");

        for(int i = 0; i < m_info.installFiles.size(); ++i)  {
            UbuntuFile f = m_info.installFiles.get(i);
            cmd.add(String.format(
                    "echo \"update %s %s\" >> \"%s\"",
                    Utils.getFilenameFromUrl(f.path), Utils.getFilenameFromUrl(f.signature), dest));
        }

        cmd.add("echo \"unmount system\" >> \"" + dest + "\"");
        cmd.add("echo success");

        List<String> out = Shell.SU.run(cmd);
        return out != null && !out.isEmpty() && out.get(0).equals("success");
    }

    private UbuntuInstallInfo m_info;
    private MultiROM m_multirom;
}
