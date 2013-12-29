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

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tassadar.multirommgr.R;

public class RomListItem extends LinearLayout implements View.OnClickListener {

    public interface OnRomActionListener {
        public void onRenameClicked(String rom);
        public void onEraseClicked(String rom);
    }

    public RomListItem(Context ctx) {
        super(ctx);
    }

    public RomListItem(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
    }

    public RomListItem(Context ctx, AttributeSet attrs, int defStyleAttr) {
        super(ctx, attrs, defStyleAttr);
    }

    public void initializeListeners(OnRomActionListener listener) {
        m_listener = listener;

        ImageButton b = (ImageButton)findViewById(R.id.rename_btn);
        b.setOnClickListener(this);
        b = (ImageButton)findViewById(R.id.erase_btn);
        b.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.rename_btn:
                m_listener.onRenameClicked(m_rom);
                break;
            case R.id.erase_btn:
                m_listener.onEraseClicked(m_rom);
                break;
        }
    }

    public void setRom(String rom) {
        m_rom = rom;

        TextView t = (TextView)findViewById(R.id.rom_name);
        t.setText(rom);

        boolean internal = rom.equals("Internal");
        View v = findViewById(R.id.rename_btn);
        v.setVisibility(internal ? View.GONE : View.VISIBLE);
        v = findViewById(R.id.erase_btn);
        v.setVisibility(internal ? View.GONE : View.VISIBLE);
        v = findViewById(R.id.separator);
        v.setVisibility(internal ? View.GONE : View.VISIBLE);
    }

    private String m_rom;
    private OnRomActionListener m_listener;
}
