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
import android.view.View;
import android.widget.ProgressBar;

import com.tassadar.multirommgr.MainActivity;
import com.tassadar.multirommgr.MultiROM;
import com.tassadar.multirommgr.R;
import com.tassadar.multirommgr.Rom;
import com.tassadar.multirommgr.StatusAsyncTask;
import com.tassadar.multirommgr.Utils;

public class RomEraseDialog extends DialogFragment implements View.OnClickListener {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity a = getActivity();
        Rom rom = getArguments().getParcelable("rom");

        AlertDialog.Builder b = new AlertDialog.Builder(a);

        return b.setMessage(Utils.getString(R.string.erase_rom, rom.name))
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
        AlertDialog d = (AlertDialog)getDialog();
        Rom rom = getArguments().getParcelable("rom");

        ProgressBar b = new ProgressBar(getActivity());
        b.setIndeterminate(true);

        d.setTitle(R.string.erasing_rom);
        d.setMessage("");
        d.setContentView(b);

        setCancelable(false);

        d.getButton(AlertDialog.BUTTON_NEGATIVE).setVisibility(View.GONE);
        d.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.GONE);

        new Thread(new RomEraseRunnable(rom)).start();
    }

    private class RomEraseRunnable implements Runnable {
        private Rom m_rom;
        public RomEraseRunnable(Rom rom) {
            m_rom = rom;
        }

        @Override
        public void run() {
            MultiROM m = StatusAsyncTask.instance().getMultiROM();
            m.eraseROM(m_rom);

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
