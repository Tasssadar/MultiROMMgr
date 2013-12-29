package com.tassadar.multirommgr.romlistfragment;


import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
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
    }

    private String m_rom;
    private OnRomActionListener m_listener;
}
