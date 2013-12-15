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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Activity;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.fima.cardsui.objects.Card;
import com.fima.cardsui.views.CardUI;

import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

public class MainActivity extends Activity implements StatusAsyncTask.StatusAsyncTaskListener,
        UbuntuManifestAsyncTask.UbuntuManifestAsyncTaskListener, StartInstallListener, OnRefreshListener {

    public static final int ACT_INSTALL_MULTIROM = 1;
    public static final int ACT_INSTALL_UBUNTU   = 2;
    public static final int ACT_CHANGELOG        = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // This activity is using different background color, which would cause overdraw
        // of the whole area, so disable the default background
        getWindow().setBackgroundDrawable(null);

        PreferenceManager.setDefaultValues(this, R.xml.settings, false);

        Utils.installHttpCache(this);

        mCardView = (CardUI) findViewById(R.id.cardsview);
        mCardView.setSwipeable(false);
        mCardView.setSlideIn(!StatusAsyncTask.instance().isComplete());

        m_prtLayout = (PullToRefreshLayout)findViewById(R.id.ptr_layout);
        ActionBarPullToRefresh
                .from(this)
                .allChildrenArePullable()
                .listener(this)
                .setup(m_prtLayout);

        if(savedInstanceState != null)
            m_cardsSavedState = savedInstanceState.getBundle("cards_state");

        Intent i = getIntent();
        if(i == null || !i.getBooleanExtra("force_refresh", false)) {
            start();
        } else {
            i.removeExtra("force_refresh");
            refresh();
        }
    }

    protected void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle cardsState = new Bundle();
        mCardView.saveInstanceState(cardsState);
        outState.putBundle("cards_state", cardsState);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Utils.flushHttpCache();
    }

    private void start() {
        // not the same data, something might have changed
        if(!StatusAsyncTask.instance().isComplete())
            m_cardsSavedState = null;

        if(m_menu != null)
            m_menu.findItem(R.id.action_refresh).setEnabled(false);
        m_prtLayout.setRefreshing(true);

        mCardView.addCard(new StatusCard(), true);
        StatusAsyncTask.instance().setListener(this);
        StatusAsyncTask.instance().execute();
    }

    private void refresh() {
        StatusAsyncTask.destroy();
        UbuntuManifestAsyncTask.destroy();

        mCardView.clearCards();
        mCardView.setSlideIn(true);
        start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        m_menu = menu;

        if(!StatusAsyncTask.instance().isComplete())
            menu.findItem(R.id.action_refresh).setEnabled(false);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem it) {
        switch(it.getItemId()) {
            case R.id.action_refresh:
                refresh();
                return true;
            case R.id.action_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                return true;
            case R.id.action_reboot:
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
            default:
                return false;
        }
    }

    @Override
    public void onStatusTaskFinished(StatusAsyncTask.Result res) {
        boolean hasUbuntu = false;
        if(res.manifest != null) {
            mCardView.addCard(new InstallCard(m_cardsSavedState, res.manifest, res.recovery == null, this));

            if(!res.device.supportsUbuntuTouch()) {
                SharedPreferences p = MultiROMMgrApplication.getPreferences();
                if(p.getBoolean("showUbuntuUnsupported", true))
                    showUbuntuUnsupportedCard();
            } else if(res.multirom != null && res.recovery != null) {
                UbuntuManifestAsyncTask.instance().setListener(this);
                mCardView.addCard(new UbuntuCard(m_cardsSavedState, this, res.manifest, res.multirom, res.recovery));
                hasUbuntu = true;
            }

            mCardView.refresh();
        }

        if(!hasUbuntu)
            setRefreshComplete();

        // Saved state is not needed anymore
        m_cardsSavedState = null;
    }

    @Override
    public void onUbuntuTaskFinished(UbuntuManifestAsyncTask.Result res) {
        setRefreshComplete();
    }

    private void setRefreshComplete() {
        if(m_menu != null)
            m_menu.findItem(R.id.action_refresh).setEnabled(true);
        m_prtLayout.setRefreshComplete();
    }

    private void showUbuntuUnsupportedCard() {
        Card c = new StaticCard(R.layout.ubuntu_unsupported_card);
        c.setOnCardSwipedListener(new Card.OnCardSwiped() {
            @Override
            public void onCardSwiped(Card card, View layout) {
                SharedPreferences.Editor p = MultiROMMgrApplication.getPreferences().edit();
                p.putBoolean("showUbuntuUnsupported", false);
                p.commit();
            }
        });
        mCardView.addSwipableCard(c, false, true);
    }

    @Override
    public void startActivity(Bundle data, int id, Class<?> cls) {
        Intent i = new Intent(this, cls);
        if(data != null)
            i.putExtras(data);
        startActivityForResult(i, id);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case ACT_INSTALL_MULTIROM:
                if(resultCode == RESULT_OK)
                    refresh();
                break;
        }
    }

    @Override
    public void onRefreshStarted(View view) {
        refresh();
    }

    private CardUI mCardView;
    private Menu m_menu;
    private Bundle m_cardsSavedState;
    private PullToRefreshLayout m_prtLayout;
}
