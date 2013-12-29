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

import com.tassadar.multirommgr.MainActivity;
import com.tassadar.multirommgr.MultiROM;
import com.tassadar.multirommgr.R;
import com.tassadar.multirommgr.StatusAsyncTask;
import com.tassadar.multirommgr.Utils;

public class RomEraseDialog extends DialogFragment implements View.OnClickListener {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity a = getActivity();
        Bundle args = getArguments();

        AlertDialog.Builder b = new AlertDialog.Builder(a);

        return b.setMessage(Utils.getString(R.string.erase_rom, args.getString("rom_name")))
                .setTitle(R.string.erase_rom_title)
                .setIcon(R.drawable.alerts_and_states_warning)
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(true)
                .setPositiveButton(R.string.erase, null)
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

        ProgressBar b = new ProgressBar(getActivity());
        b.setIndeterminate(true);

        d.setTitle(R.string.erasing_rom);
        d.setMessage("");
        d.addContentView(b, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));

        setCancelable(false);

        d.getButton(AlertDialog.BUTTON_NEGATIVE).setVisibility(View.GONE);
        d.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.GONE);

        new Thread(new RomEraseRunnable(args.getString("rom_name"))).start();
    }

    private class RomEraseRunnable implements Runnable {
        private String m_rom_name;
        public RomEraseRunnable(String rom_name) {
            m_rom_name = rom_name;
        }

        @Override
        public void run() {
            MultiROM m = StatusAsyncTask.instance().getMultiROM();
            m.eraseROM(m_rom_name);

            Activity a = getActivity();
            if(a == null)
                return;

            a.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dismiss();

                    MainActivity a = (MainActivity)getActivity();
                    if(a != null)
                        a.refresh();
                }
            });
        }
    }
}
