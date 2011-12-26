package com.tassadar.multirommgr;

import android.content.Context;
import android.os.Handler;
import android.preference.Preference;
import android.util.AttributeSet;

public class TetrisMaxPreference extends Preference
{
    private Handler m_handler;
    public TetrisMaxPreference(Context context)
    {
        super(context);
    }
    public TetrisMaxPreference(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    @Override
    protected void onClick()
    {
        m_handler.sendEmptyMessage(1);
    }
    
    public void setHandler(Handler h)
    {
        m_handler = h;
    }
}