/*
 * This file is part of MultiROM Manager.
 *
 * MultiROM Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MultiROM Manager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MultiROM Manager. If not, see <http://www.gnu.org/licenses/>.
 */

package com.tassadar.multirommgr.installfragment;

import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import com.tassadar.multirommgr.MultiROM;
import com.tassadar.multirommgr.MultiROMMgrApplication;
import com.tassadar.multirommgr.R;
import com.tassadar.multirommgr.SettingsActivity;
import com.tassadar.multirommgr.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class UbuntuInstallTask extends InstallAsyncTask  {

    public static final String DOWN_DIR = "UbuntuTouch";

    public UbuntuInstallTask(UbuntuInstallInfo info, MultiROM multirom) {
        m_info = info;
        m_multirom = multirom;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        m_listener.onProgressUpdate(0, 0, true, Utils.getString(R.string.preparing_downloads, ""));
        m_listener.onInstallLog(Utils.getString(R.string.preparing_downloads, "<br>"));

        File destDir = new File(Utils.getDownloadDir(), DOWN_DIR);
        destDir.mkdirs();
        String dest = destDir.toString();

        String suDestDir = findSUDestDir(destDir);

        if(suDestDir == null) {
            m_listener.onInstallLog(Utils.getString(R.string.su_failed_find_dir));
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

        m_listener.onProgressUpdate(0, 0, true, Utils.getString(R.string.installing_utouch));
        m_listener.enableCancel(false);

        String romPath = m_multirom.getNewRomFolder("utouch_" + m_info.channelName);
        if(romPath == null) {
            m_listener.onInstallLog(Utils.getString(R.string.failed_create_rom));
            m_listener.onInstallComplete(false);
            return null;
        }
        m_listener.onInstallLog(Utils.getString(R.string.installing_rom, Utils.getFilenameFromUrl(romPath)));

        if(!m_multirom.initUbuntuDir(romPath)) {
            m_listener.onInstallLog(Utils.getString(R.string.failed_rom_init));
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
            m_listener.onInstallLog(Utils.getString(R.string.invalid_url, url));
            m_listener.onInstallComplete(false);
            return false;
        }

        File destFile = new File(destDir, filename);
        if(destFile.exists() && checksum != null) {
            m_listener.onInstallLog(Utils.getString(R.string.checking_file, Utils.trim(filename, 40)));
            String sha256 = Utils.calculateSHA256(destFile);
            if(checksum.equals(sha256)) {
                m_listener.onInstallLog(Utils.getString(R.string.ok_skippping));
                return true;
            } else {
                m_listener.onInstallLog(Utils.getString(R.string.failed_redownload));
            }
        }

        if(!downloadFile(url, destFile)) {
            if(!m_canceled)
                m_listener.onInstallComplete(false);
            return false;
        }

        if(checksum != null && !checksum.isEmpty()) {
            m_listener.onInstallLog(Utils.getString(R.string.checking_file, m_downFilename));
            String sha256 = Utils.calculateSHA256(destFile);
            if(checksum.equals(sha256))
                m_listener.onInstallLog(Utils.getString(R.string.ok));
            else {
                m_listener.onInstallLog(Utils.getString(R.string.failed));
                m_listener.onInstallComplete(false);
                return false;
            }
        }
        return true;
    }

    private boolean copyFiles(String src, String dest, ArrayList<UbuntuFile> files) {
        String bb = Utils.extractAsset("busybox");
        if(bb == null) {
            m_listener.onInstallLog(Utils.getString(R.string.failed_busybox));
            return false;
        }

        for(int i = 0; i < files.size(); ++i) {
            UbuntuFile f = files.get(i);
            String filename = Utils.getFilenameFromUrl(f.path);
            m_listener.onInstallLog(Utils.getString(R.string.copying_file, Utils.trim(filename, 40)));

            if(!copyFile(src + "/" + filename, dest, bb)) {
                m_listener.onInstallLog(Utils.getString(R.string.failed_file_copy, filename));
                return false;
            }

            filename = Utils.getFilenameFromUrl(f.signature);
            if(!copyFile(src + "/" + filename, dest, bb)) {
                m_listener.onInstallLog(Utils.getString(R.string.failed_file_copy, filename));
                return false;
            }
            m_listener.onInstallLog(Utils.getString(R.string.ok));
        }

        SharedPreferences pref = MultiROMMgrApplication.getPreferences();
        if(pref.getBoolean(SettingsActivity.UTOUCH_DELETE_FILES, false)) {
            m_listener.onInstallLog(Utils.getString(R.string.deleting_used_files));
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

        String tail = destDir.getAbsolutePath();
        if(tail.startsWith(Environment.getExternalStorageDirectory().getAbsolutePath()))
            tail = tail.substring(Environment.getExternalStorageDirectory().getAbsolutePath().length()+1);
        else if(tail.startsWith("/sdcard/"))
            tail = tail.substring(("/sdcard/").length());

        StringBuilder b = new StringBuilder();
        appendSUDirCheck(b, destDir.getAbsolutePath(), "");

        final String[] paths = {
                "/sdcard/", "/storage/emulated/0/", "/storage/emulated/legacy/",
                "/data/media/0/", "/data/media/"
        };

        for(String p : paths)
            appendSUDirCheck(b, p, tail);

        b.append("se exit 0; fi;");

        List<String> out = Shell.SU.run(b.toString());

        if(out == null || out.isEmpty())
            return null;
        return out.get(0);
    }

    private static void appendSUDirCheck(StringBuilder b, String path, String path2) {
        b.append("if [ -f \"")
         .append(path).append(path2)
         .append("/ut_test_file\" ]; then echo \"")
         .append(path).append(path2)
         .append("\"; exit 0; el");
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
