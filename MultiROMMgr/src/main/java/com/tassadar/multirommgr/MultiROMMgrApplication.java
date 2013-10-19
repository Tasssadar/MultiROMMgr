package com.tassadar.multirommgr;

import android.content.Context;

import eu.chainfire.libsuperuser.Application;

public class MultiROMMgrApplication extends Application {
    private static Context m_context;

    public void onCreate(){
        super.onCreate();
        m_context = getApplicationContext();
    }

    public static Context getAppContext() {
        return m_context;
    }
}
