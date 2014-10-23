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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.PopupMenu;
import android.widget.Spinner;

import com.fima.cardsui.objects.Card;
import com.fima.cardsui.views.CardUI;
import com.tassadar.multirommgr.MainActivity;
import com.tassadar.multirommgr.MainFragment;
import com.tassadar.multirommgr.MgrApp;
import com.tassadar.multirommgr.MultiROMSwipeRefreshLayout;
import com.tassadar.multirommgr.R;
import com.tassadar.multirommgr.StatusAsyncTask;

public class InstallFragment extends MainFragment implements StatusAsyncTask.StatusAsyncTaskListener,
        UbuntuManifestAsyncTask.UbuntuManifestAsyncTaskListener, StartInstallListener,
        PopupMenu.OnMenuItemClickListener, MultiROMSwipeRefreshLayout.ScrollUpListener {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        m_view = inflater.inflate(R.layout.fragment_install, container, false);

        mCardView = (CardUI) findViewById(R.id.cardsview);
        mCardView.setSwipeable(false);
        mCardView.setSlideIn(!StatusAsyncTask.instance().isComplete());

        if(savedInstanceState != null)
            m_cardsSavedState = savedInstanceState.getBundle("cards_state");

        m_actListener.addScrollUpListener(this);
        m_actListener.onFragmentViewCreated();
        return m_view;
    }

    @Override
    public void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle cardsState = new Bundle();
        mCardView.saveInstanceState(cardsState);
        outState.putBundle("cards_state", cardsState);
    }

    @Override
    public void startRefresh() {
        super.startRefresh();

        // not the same data, something might have changed
        if(!StatusAsyncTask.instance().isComplete())
            m_cardsSavedState = null;

        mCardView.addCard(new StatusCard(this), true);
    }

    @Override
    public void refresh() {
        super.refresh();

        mCardView.clearCards();
        mCardView.setSlideIn(true);
    }

    @Override
    public void onStatusTaskFinished(StatusAsyncTask.Result res) {
        boolean hasUbuntu = false;

        if(res.manifest != null) {
            mCardView.addCard(new InstallCard(m_cardsSavedState, res.manifest, res.recovery == null, this));
            if(!res.device.supportsUbuntuTouch()) {
                showNotificationCard(R.layout.ubuntu_unsupported_card, "showUbuntuUnsupported");
            } else if(res.multirom != null && res.recovery != null) {
                UbuntuManifestAsyncTask.instance().setListener(this);
                mCardView.addCard(new UbuntuCard(m_cardsSavedState, this, res.manifest, res.multirom, res.recovery));
                hasUbuntu = true;
            }

            mCardView.refresh();
        } else if((res.code & StatusAsyncTask.RES_NO_MANIFEST) != 0) {
            showNotificationCard(R.layout.no_manifest_card, "showNoManifestCard");
        }

        if(!hasUbuntu)
            m_actListener.setRefreshComplete();

        // Saved state is not needed anymore
        m_cardsSavedState = null;
    }

    @Override
    public void onUbuntuTaskFinished(UbuntuManifestAsyncTask.Result res) {
        m_actListener.setRefreshComplete();
    }

    private void showNotificationCard(int layout, final String prefName) {
        final SharedPreferences p = MgrApp.getPreferences();
        if(!p.getBoolean(prefName, true))
            return;

        Card c = new StaticCard(layout);
        c.setOnCardSwipedListener(new Card.OnCardSwiped() {
            @Override
            public void onCardSwiped(Card card, View layout) {
                SharedPreferences.Editor e = MgrApp.getPreferences().edit();
                e.putBoolean(prefName, false);
                e.commit();
            }
        });
        mCardView.addSwipableCard(c, false, true);
    }

    @Override
    public void startActivity(Bundle data, int id, Class<?> cls) {
        Intent i = new Intent(getActivity(), cls);
        if(data != null)
            i.putExtras(data);
        startActivityForResult(i, id);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case MainActivity.ACT_INSTALL_MULTIROM:
            case MainActivity.ACT_UNINSTALL_MULTIROM:
                if(resultCode == Activity.RESULT_OK)
                    m_actListener.refresh();
                break;
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        if(menuItem.getItemId() != R.id.uninstall_multirom)
            return false;

        AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
        b.setTitle(R.string.uninstall_dialog_title)
         .setIcon(R.drawable.alerts_and_states_warning)
         .setMessage(R.string.uninstall_dialog_text)
         .setNegativeButton(R.string.cancel, null)
         .setCancelable(true)
         .setPositiveButton(R.string.continue_text, new DialogInterface.OnClickListener() {
             @Override
             public void onClick(DialogInterface dialogInterface, int i) {
                 Bundle bundle = new Bundle();
                 bundle.putString("installation_type", "uninstall_multirom");
                 Bundle extras = new Bundle();
                 extras.putBundle("installation_info", bundle);

                 startActivity(extras, MainActivity.ACT_UNINSTALL_MULTIROM, InstallActivity.class);
             }
         });
        b.create().show();
        return true;
    }

    @Override
    public boolean canChildScrollUp() {
        return canChildScrollUp(mCardView.getScrollView());
    }

    private CardUI mCardView;
    private Bundle m_cardsSavedState;
}
