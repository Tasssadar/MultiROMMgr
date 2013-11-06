package com.tassadar.multirommgr;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import eu.chainfire.libsuperuser.Application;

public class MultiROMMgrApplication extends Application {
    private static Context m_context;

    public void onCreate(){
        super.onCreate();
        m_context = getApplicationContext();
    }

    public static SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(m_context);
    }

    public static Context getAppContext() {
        return m_context;
    }
}
