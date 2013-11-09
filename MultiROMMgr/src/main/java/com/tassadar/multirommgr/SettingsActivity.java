package com.tassadar.multirommgr;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.MenuItem;

public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String GENERAL_UPDATE_CHECK =  "general_update_check";
    public static final String GENERAL_AUTO_REBOOT = "general_auto_reboot";
    public static final String GENERAL_DOWNLOAD_DIR = "general_download_dir";
    public static final String UTOUCH_SHOW_HIDDEN = "utouch_show_hidden";
    public static final String UTOUCH_DELETE_FILES = "utouch_delete_files";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        SharedPreferences p = MultiROMMgrApplication.getPreferences();
        if(p.getString(GENERAL_DOWNLOAD_DIR, null) == null)
            Utils.setDownloadDir(Utils.getDefaultDownloadDir());

        addPreferencesFromResource(R.xml.settings);

        p.registerOnSharedPreferenceChangeListener(this);
        updatePrefText();
    }

    public void onDestroy() {
        super.onDestroy();

        SharedPreferences p = MultiROMMgrApplication.getPreferences();
        p.unregisterOnSharedPreferenceChangeListener(this);
    }

    private void updatePrefText() {
        Preference pref = findPreference(UTOUCH_DELETE_FILES);
        pref.setSummary(Utils.getString(R.string.pref_delete_utouch_files_summ,
                Utils.getDownloadDir(), UbuntuInstallTask.DOWN_DIR));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences p, String key) {
        if(key.equals(GENERAL_UPDATE_CHECK)) {
            UpdateChecker.updateAlarmStatus();
        } else if(key.equals(GENERAL_DOWNLOAD_DIR)) {
            Utils.setDownloadDir(p.getString(key, Utils.getDefaultDownloadDir()));
            updatePrefText();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return false;
    }
}
