package com.tassadar.multirommgr;

public interface MainActivityListener {
    public void startRefresh();
    public void refresh();
    public void setRefreshComplete();
    public void onFragmentViewCreated();
    public void onFragmentViewDestroyed();
}
