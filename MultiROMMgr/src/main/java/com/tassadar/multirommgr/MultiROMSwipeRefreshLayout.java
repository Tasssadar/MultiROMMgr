package com.tassadar.multirommgr;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;

import java.util.HashSet;

public class MultiROMSwipeRefreshLayout extends SwipeRefreshLayout {
    public static interface ScrollUpListener {
        public boolean canChildScrollUp();
    }

    public MultiROMSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean canChildScrollUp() {
        for(ScrollUpListener l : m_listeners) {
            if(l.canChildScrollUp())
                return true;
        }
        return false;
    }

    public void addScrollUpListener(ScrollUpListener l) {
        m_listeners.add(l);
    }

    public void rmScrollUpListener(ScrollUpListener l) {
        m_listeners.remove(l);
    }

    private HashSet<ScrollUpListener> m_listeners = new HashSet<ScrollUpListener>();
}
