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
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.tassadar.multirommgr.MainActivity;
import com.tassadar.multirommgr.MultiROM;
import com.tassadar.multirommgr.R;
import com.tassadar.multirommgr.Rom;
import com.tassadar.multirommgr.StatusAsyncTask;
import com.tassadar.multirommgr.Utils;

public class RomIconDialog extends DialogFragment implements AdapterView.OnItemClickListener, View.OnClickListener {

    private static final int WRITE_EXTERNAL_PERM_REQUEST = 0;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity a = getActivity();

        LinearLayout view = (LinearLayout)a.getLayoutInflater()
                .inflate(R.layout.dialog_rom_icon, null, false);

        GridView grid = (GridView)view.findViewById(R.id.icon_grid);
        m_adapter = new RomIconGridAdapter(a);
        grid.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        grid.setAdapter(m_adapter);
        grid.setOnItemClickListener(this);

        Button btn = (Button)view.findViewById(R.id.browse_icons);
        btn.setOnClickListener(this);

        AlertDialog.Builder b = new AlertDialog.Builder(a);
        return b.setView(view)
                .setCancelable(true)
                .create();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
        setDialogInProgress();

        Rom rom = getArguments().getParcelable("rom");
        new Thread(new SetIconRunnable(rom, m_adapter.getItem(pos))).start();
    }

    @Override
    public void onClick(View view) {
        final Activity a = getActivity();
        if(a == null)
            return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                a.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_PERM_REQUEST);
            return;
        }

        Intent intent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        if(Utils.isIntentAvailable(intent)) {
            startActivityForResult(intent, MainActivity.ACT_SELECT_ICON);
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(
                    Intent.createChooser(intent,  Utils.getString(R.string.select_icon)),
                    MainActivity.ACT_SELECT_ICON);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if(requestCode == WRITE_EXTERNAL_PERM_REQUEST && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            final View root = getView();
            if(root != null) {
                onClick(root.findViewById(R.id.browse_icons));
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode != MainActivity.ACT_SELECT_ICON || resultCode != Activity.RESULT_OK) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        Uri selectedImage = data.getData();

        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Cursor cursor = getActivity().getContentResolver().query(
                selectedImage, filePathColumn, null, null, null);

        if(!cursor.moveToFirst())
        {
            cursor.close();
            return;
        }

        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String path = cursor.getString(columnIndex);
        cursor.close();
        if(path == null || path.isEmpty())
            return;

        setDialogInProgress();

        Rom rom = getArguments().getParcelable("rom");
        new Thread(new SetIconRunnable(rom, path)).start();
    }

    private void setDialogInProgress() {
        AlertDialog d = (AlertDialog)getDialog();

        setCancelable(false);

        View v = d.findViewById(R.id.icon_grid);
        v.setEnabled(false);
        v = d.findViewById(R.id.browse_icons);
        v.setClickable(false);

        ProgressBar p = (ProgressBar)d.findViewById(R.id.progress_bar);
        p.setIndeterminate(true);
    }

    private class SetIconRunnable implements Runnable {
        private Rom m_rom;
        private int m_drawable_id;
        private String m_path;

        public SetIconRunnable(Rom rom, int drawable_id) {
            m_rom = rom;
            m_drawable_id = drawable_id;
            m_path = null;
        }

        public SetIconRunnable(Rom rom, String path) {
            m_rom = rom;
            m_path = path;
        }

        @Override
        public void run() {
            MultiROM m = StatusAsyncTask.instance().getMultiROM();

            if(m_path != null)
                m.setRomIcon(m_rom, m_path);
            else
                m.setRomIcon(m_rom, m_drawable_id);

            Activity a = getActivity();
            if(a == null)
                return;

            a.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dismiss();

                    Activity a = getActivity();
                    if(a == null)
                        return;

                    Fragment f = a.getFragmentManager().findFragmentById(R.id.content_frame);
                    if(f instanceof RomListFragment) {
                        ((RomListFragment)f).invalidateAdapter();
                    }
                }
            });
        }
    }

    private RomIconGridAdapter m_adapter;
}
