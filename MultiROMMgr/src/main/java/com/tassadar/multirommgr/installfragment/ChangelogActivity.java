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

package com.tassadar.multirommgr.installfragment;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;

import com.tassadar.multirommgr.R;

public class ChangelogActivity extends FragmentActivity implements ActionBar.TabListener, ViewPager.OnPageChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_changelog);

        m_urls = getIntent().getStringArrayExtra("changelog_urls");
        m_names = getIntent().getStringArrayExtra("changelog_names");

        m_viewPager = (ViewPager)findViewById(R.id.pager);
        m_viewPager.setAdapter(new ChangelogAdapter(getSupportFragmentManager()));
        m_viewPager.setOnPageChangeListener(this);

        ActionBar bar = getActionBar();
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        for(int i = 0; i < m_names.length; ++i) {
            ActionBar.Tab t = bar.newTab();
            t.setText(m_names[i]);
            t.setTabListener(this);

            bar.addTab(t);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return false;
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        m_viewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) { }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) { }

    @Override
    public void onPageSelected(int i) {
        getActionBar().setSelectedNavigationItem(i);
    }

    @Override
    public void onPageScrolled(int i, float v, int i2) { }

    @Override
    public void onPageScrollStateChanged(int i) { }

    private class ChangelogAdapter extends FragmentPagerAdapter {
        public ChangelogAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            return ChangelogFragment.newInstance(m_urls[i]);
        }

        @Override
        public int getCount() {
            return m_urls.length;
        }
    }

    private ViewPager m_viewPager;
    private String[] m_urls;
    private String[] m_names;
}
