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

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.app.FragmentManager;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.tassadar.multirommgr.installfragment.UbuntuManifestAsyncTask;

public class MainActivity extends AppCompatActivity implements StatusAsyncTask.StatusAsyncTaskListener, MainActivityListener, SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "MROMMgr::MainActivity";

    public static final int ACT_INSTALL_MULTIROM   = 1;
    public static final int ACT_INSTALL_UBUNTU     = 2;
    public static final int ACT_CHANGELOG          = 3;
    public static final int ACT_SELECT_ICON        = 4;
    public static final int ACT_UNINSTALL_MULTIROM = 5;

    public static final String INTENT_EXTRA_SHOW_ROM_LIST = "show_rom_list";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(Build.VERSION.SDK_INT == 20) {
            showDeprecatedLAlert();
            return;
        }

        setContentView(R.layout.activity_main);

        // This activity is using different background color, which would cause overdraw
        // of the whole area, so disable the default background
        getWindow().setBackgroundDrawable(null);

        Utils.installHttpCache(this);
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);

        m_srLayout = (MultiROMSwipeRefreshLayout)findViewById(R.id.refresh_layout);
        m_srLayout.setOnRefreshListener(this);

        m_curFragment = -1;

        m_fragmentTitles = getResources().getStringArray(R.array.main_fragment_titles);
        m_drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        m_drawerList = (ListView) findViewById(R.id.left_drawer);

        String[] fragmentClsNames = new String[MainFragment.MAIN_FRAG_CNT];
        for(int i = 0; i < fragmentClsNames.length; ++i)
            fragmentClsNames[i] = MainFragment.getFragmentClass(i).getName();

        m_fragments = new MainFragment[MainFragment.MAIN_FRAG_CNT];
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction t = fragmentManager.beginTransaction();
        for(int i = 0; i < m_fragments.length; ++i) {
            m_fragments[i] = (MainFragment)fragmentManager.findFragmentByTag(fragmentClsNames[i]);
            if(m_fragments[i] == null) {
                m_fragments[i] = MainFragment.newFragment(i);
                t.add(R.id.content_frame, m_fragments[i], fragmentClsNames[i]);
            }
            t.hide(m_fragments[i]);
        }
        t.commit();

        // Set the adapter for the list view
        m_drawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, m_fragmentTitles));
        // Set the list's click listener
        m_drawerList.setOnItemClickListener(new DrawerItemClickListener());

        m_drawerTitle = getText(R.string.app_name);
        m_drawerToggle = new ActionBarDrawerToggle(
                this, m_drawerLayout, R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerClosed(View view) {
                getSupportActionBar().setTitle(m_title);
            }

            public void onDrawerOpened(View drawerView) {
                getSupportActionBar().setTitle(m_drawerTitle);
            }
        };
        m_drawerLayout.setDrawerListener(m_drawerToggle);

        final ActionBar bar = getSupportActionBar();
        if(bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setHomeButtonEnabled(true);
        }

        if (getIntent().hasExtra(INTENT_EXTRA_SHOW_ROM_LIST) &&
            getIntent().getBooleanExtra(INTENT_EXTRA_SHOW_ROM_LIST, false)) {
            getIntent().removeExtra(INTENT_EXTRA_SHOW_ROM_LIST);
            selectItem(1);
        } else if(savedInstanceState != null) {
            selectItem(savedInstanceState.getInt("curFragment", 0));
        } else {
            selectItem(0);
        }
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        if (i.hasExtra(INTENT_EXTRA_SHOW_ROM_LIST) &&
            i.getBooleanExtra(INTENT_EXTRA_SHOW_ROM_LIST, false)) {
            selectItem(1);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Utils.flushHttpCache();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("curFragment", m_curFragment);
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu) {

        getMenuInflater().inflate(R.menu.main, menu);

        m_refreshItem = menu.findItem(R.id.action_refresh);
        if(!StatusAsyncTask.instance().isComplete())
            m_refreshItem.setEnabled(false);
        return true;
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    /** Swaps fragments in the main content view */
    private void selectItem(int position) {
        if(position < 0 || position >= m_fragments.length) {
            Log.e(TAG, "Invalid fragment index " + position);
            return;
        }

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction t = fragmentManager.beginTransaction();

        if(m_curFragment != -1)
            t.hide(m_fragments[m_curFragment]);
        t.show(m_fragments[position]);
        t.commit();

        m_curFragment = position;

        // Highlight the selected item, update the title, and close the drawer
        m_drawerList.setItemChecked(position, true);
        setTitle(m_fragmentTitles[position]);
        m_drawerLayout.closeDrawer(m_drawerList);
    }

    @Override
    public void setTitle(CharSequence title) {
        m_title = title;
        getSupportActionBar().setTitle(m_title);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if(m_drawerToggle != null)
            m_drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(m_drawerToggle != null)
            m_drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem it) {
        if (m_drawerToggle.onOptionsItemSelected(it))
            return true;

        switch(it.getItemId()) {
            case R.id.action_refresh:
                refresh();
                return true;
            case R.id.action_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                return true;
            case R.id.action_reboot:
            {
                AlertDialog.Builder b = new AlertDialog.Builder(this);
                b.setTitle(R.string.reboot)
                        .setCancelable(true)
                        .setNegativeButton(R.string.cancel, null)
                        .setItems(R.array.reboot_options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                switch (i) {
                                    case 0: Utils.reboot(""); break;
                                    case 1: Utils.reboot("recovery"); break;
                                    case 2: Utils.reboot("bootloader"); break;
                                }
                            }
                        })
                        .create().show();
                return true;
            }
            default:
                return false;
        }
    }

    public void startRefresh(boolean notifyRefreshLayout) {
        if(notifyRefreshLayout)
            m_srLayout.setRefreshing(true);

        if(m_refreshItem != null)
            m_refreshItem.setEnabled(false);

        for(int i = 0; i < m_fragments.length; ++i)
            m_fragments[i].startRefresh();

        StatusAsyncTask.instance().setListener(this);
        StatusAsyncTask.instance().execute();
    }

    public void refresh(boolean notifyRefreshLayout) {
        StatusAsyncTask.destroy();
        UbuntuManifestAsyncTask.destroy();

        for(int i = 0; i < m_fragments.length; ++i)
            m_fragments[i].refresh();

        startRefresh(notifyRefreshLayout);
    }

    @Override
    public void refresh() {
        refresh(true);
    }

    @Override
    public void setRefreshComplete() {
        m_srLayout.setRefreshing(false);

        if(m_refreshItem != null)
            m_refreshItem.setEnabled(true);

        for(int i = 0; i < m_fragments.length; ++i)
            m_fragments[i].setRefreshComplete();
    }

    @Override
    public void onFragmentViewCreated() {
        if(++m_fragmentViewsCreated == m_fragments.length) {
            // postDelayed because SwipeRefresher view ignores
            // setRefreshing call otherwise
            m_srLayout.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent i = getIntent();
                    if(i == null || !i.getBooleanExtra("force_refresh", false)) {
                        startRefresh(true);
                    } else {
                        i.removeExtra("force_refresh");
                        refresh();
                    }
                }
            }, 1);
        }
    }

    @Override
    public void onFragmentViewDestroyed() {
        --m_fragmentViewsCreated;
    }

    @Override
    public void addScrollUpListener(MultiROMSwipeRefreshLayout.ScrollUpListener l) {
        m_srLayout.addScrollUpListener(l);
    }

    @Override
    public void onStatusTaskFinished(StatusAsyncTask.Result res) {
        for(int i = 0; i < m_fragments.length; ++i)
            m_fragments[i].onStatusTaskFinished(res);
    }

    @Override
    public void onRefresh() {
        refresh(false);
    }

    @TargetApi(20)
    private void showDeprecatedLAlert() {
        SpannableString msg = new SpannableString(getString(R.string.deprecated_l_text));
        Linkify.addLinks(msg, Linkify.ALL);

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(R.string.deprecated_l_title)
         .setCancelable(false)
         .setMessage(msg)
         .setNegativeButton(R.string.deprecated_l_btn, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
          });

        AlertDialog d = b.create();
        d.show();

        TextView msgView = (TextView)d.findViewById(android.R.id.message);
        msgView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private DrawerLayout m_drawerLayout;
    private ListView m_drawerList;
    private String[] m_fragmentTitles;
    private MainFragment[] m_fragments;
    private int m_curFragment;
    private CharSequence m_title;
    private ActionBarDrawerToggle m_drawerToggle;
    private CharSequence m_drawerTitle;
    private MenuItem m_refreshItem;
    private int m_fragmentViewsCreated;
    private MultiROMSwipeRefreshLayout m_srLayout;
}
