package com.tassadar.multirommgr;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity {
    public static final String GENERAL_AUTO_REBOOT = "general_auto_reboot";
    public static final String UTOUCH_SHOW_HIDDEN = "utouch_show_hidden";
    public static final String UTOUCH_DELETE_FILES = "utouch_delete_files";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        getActionBar().setDisplayHomeAsUpEnabled(true);
    }
}
