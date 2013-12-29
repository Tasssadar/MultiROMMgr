package com.tassadar.multirommgr;


import android.app.Activity;
import android.app.Fragment;
import android.view.View;

import com.tassadar.multirommgr.installfragment.InstallFragment;
import com.tassadar.multirommgr.romlistfragment.RomListFragment;

import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

public class MainFragment extends Fragment implements OnRefreshListener {

    public static final int MAIN_FRAG_INSTALL   = 0;
    public static final int MAIN_FRAG_ROM_LIST  = 1;

    static public MainFragment newFragment(int type) {
        switch(type) {
            case MAIN_FRAG_INSTALL:
                return new InstallFragment();
            case MAIN_FRAG_ROM_LIST:
                return new RomListFragment();
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

    @Override
    public void onRefreshStarted(View view) {
        m_actListener.refresh();
    }

    protected void setPtrLayout(int id) {
        m_ptrLayout = (PullToRefreshLayout)findViewById(id);
        ActionBarPullToRefresh
                .from(getActivity())
                .allChildrenArePullable()
                .listener(this)
                .setup(m_ptrLayout);
    }

    public void startRefresh() {
        m_ptrLayout.setRefreshing(true);
    }

    public void refresh() { }
    public void onStatusTaskFinished(StatusAsyncTask.Result res) { }

    public void setRefreshComplete() {
        m_ptrLayout.setRefreshComplete();
    }

    protected MainActivityListener m_actListener;
    protected View m_view;
    protected PullToRefreshLayout m_ptrLayout;
}
