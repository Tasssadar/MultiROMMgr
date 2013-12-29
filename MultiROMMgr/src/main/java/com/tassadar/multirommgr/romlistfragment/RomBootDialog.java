package com.tassadar.multirommgr.romlistfragment;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.tassadar.multirommgr.MultiROM;
import com.tassadar.multirommgr.R;
import com.tassadar.multirommgr.StatusAsyncTask;
import com.tassadar.multirommgr.Utils;

public class RomBootDialog extends DialogFragment implements View.OnClickListener {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity a = getActivity();
        Bundle args = getArguments();

        AlertDialog.Builder b = new AlertDialog.Builder(a);

        return b.setMessage(Utils.getString(R.string.boot_rom, args.getString("rom_name")))
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(true)
                .setPositiveButton(R.string.boot, null)
                .setTitle(R.string.boot_rom_title)
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();

        AlertDialog d = (AlertDialog)getDialog();
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        Bundle args = getArguments();
        AlertDialog d = (AlertDialog)getDialog();

        setCancelable(false);
        d.setMessage(getString(R.string.booting));

        d.getButton(AlertDialog.BUTTON_NEGATIVE).setVisibility(View.GONE);
        d.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.GONE);

        new Thread(new RomBootRunnable(args.getString("rom_name"))).start();
    }

    private class RomBootRunnable implements Runnable {
        private String m_rom_name;
        public RomBootRunnable(String rom_name) {
            m_rom_name = rom_name;
        }

        @Override
        public void run() {
            MultiROM m = StatusAsyncTask.instance().getMultiROM();
            boolean has_kexec = StatusAsyncTask.instance().hasKexecKernel();
            Activity a = getActivity();
            if(a == null)
                return;

            if(!has_kexec && m.isKexecNeededFor(m_rom_name)) {
                a.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog d = (AlertDialog)getDialog();
                        if(d == null)
                            return;

                        d.getButton(AlertDialog.BUTTON_NEGATIVE).setVisibility(View.VISIBLE);
                        d.setMessage(getString(R.string.rom_boot_kexec));
                        setCancelable(true);
                    }
                });

                return;
            }

            // this won't return unless it fails
            m.bootRom(m_rom_name);

            a.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog d = (AlertDialog)getDialog();
                    if(d == null)
                        return;

                    d.getButton(AlertDialog.BUTTON_NEGATIVE).setVisibility(View.VISIBLE);
                    d.setMessage(getString(R.string.rom_boot_failed));
                    setCancelable(true);
                }
            });
        }
    }
}
