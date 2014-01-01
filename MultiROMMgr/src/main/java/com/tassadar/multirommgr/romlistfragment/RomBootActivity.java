package com.tassadar.multirommgr.romlistfragment;


import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.app.FragmentManager;

import com.tassadar.multirommgr.Rom;
import com.tassadar.multirommgr.romlistwidget.RomListOpenHelper;

public class RomBootActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        Intent i = getIntent();
        if(i == null || !i.hasExtra(RomListOpenHelper.KEY_NAME) || !i.hasExtra(RomListOpenHelper.KEY_TYPE))
            return;

        Bundle b = new Bundle();
        b.putParcelable("rom", new Rom(i.getStringExtra(RomListOpenHelper.KEY_NAME),
                i.getIntExtra(RomListOpenHelper.KEY_TYPE, 0)));

        RomBootDialog f = new RomBootDialog();
        f.setArguments(b);

        FragmentManager mgr = getFragmentManager();
        FragmentTransaction t = mgr.beginTransaction();
        t.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        t.add(android.R.id.content, f)
         .commit();
    }
}
