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

package com.tassadar.multirommgr;

import android.app.Activity;
import android.app.Fragment;
import android.view.View;
import android.widget.ListView;

import com.tassadar.multirommgr.installfragment.InstallFragment;
import com.tassadar.multirommgr.romlistfragment.RomListFragment;

public class MainFragment extends Fragment {

    public static final int MAIN_FRAG_INSTALL   = 0;
    public static final int MAIN_FRAG_ROM_LIST  = 1;
    public static final int MAIN_FRAG_CNT       = 2;

    static public MainFragment newFragment(int type) {
        Class cls = getFragmentClass(type);
        if(cls == null)
            return null;

        try {
            return (MainFragment)cls.newInstance();
        } catch (java.lang.InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    static public Class getFragmentClass(int type) {
        switch(type) {
            case MAIN_FRAG_INSTALL:
                return InstallFragment.class;
            case MAIN_FRAG_ROM_LIST:
                return RomListFragment.class;
            default:
                return null;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            m_actListener = (MainActivityListener)activity;
        } catch(ClassCastException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        m_actListener.onFragmentViewDestroyed();
    }

    protected View findViewById(int id) {
        assert m_view != null;
        return m_view.findViewById(id);
    }

    protected boolean canChildScrollUp(ListView listview) {
        if(this.isHidden())
            return false;

        if(listview.getFirstVisiblePosition() > 0)
            return true;

        final View v = listview.getChildAt(0);
        return v != null && v.getTop() < 0;
    }

    public void startRefresh() { }
    public void refresh() { }
    public void onStatusTaskFinished(StatusAsyncTask.Result res) { }
    public void setRefreshComplete() { }

    protected MainActivityListener m_actListener;
    protected View m_view;
}
