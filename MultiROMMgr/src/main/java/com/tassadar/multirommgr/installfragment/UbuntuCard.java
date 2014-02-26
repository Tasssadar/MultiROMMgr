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

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.fima.cardsui.objects.Card;
import com.tassadar.multirommgr.MainActivity;
import com.tassadar.multirommgr.Manifest;
import com.tassadar.multirommgr.MultiROM;
import com.tassadar.multirommgr.R;
import com.tassadar.multirommgr.Recovery;
import com.tassadar.multirommgr.StatusAsyncTask;
import com.tassadar.multirommgr.Utils;

import java.util.ArrayList;

public class UbuntuCard extends Card implements AdapterView.OnItemSelectedListener, View.OnClickListener {

    private static final int MIN_FREE_SPACE = 2300;

    private static final int ERROR_NONE         = 0x00;
    private static final int ERROR_MULTIROM_VER = 0x01;
    private static final int ERROR_RECOVERY_VER = 0x02;

    public UbuntuCard(Bundle savedState, StartInstallListener listener, Manifest man, MultiROM multirom, Recovery recovery) {
        m_savedState = savedState;
        m_listener = listener;
        m_manifest = man;
        m_multirom = multirom;
        m_recovery = recovery;

        m_error = ERROR_NONE;

        if(!m_manifest.hasUbuntuReqMultiROM(m_multirom))
            m_error |= ERROR_MULTIROM_VER;

        if(!m_manifest.hasUbuntuReqRecovery(m_recovery))
            m_error |= ERROR_RECOVERY_VER;

        if(m_error == ERROR_NONE) {
            UbuntuManifestAsyncTask.instance()
                    .executeTask(StatusAsyncTask.instance().getDevice(), multirom);
        }
    }

    @Override
    public void saveInstanceState(Bundle outState) {
        super.saveInstanceState(outState);

        if(m_view == null)
            return;

        Spinner s = (Spinner) m_view.findViewById(R.id.channel);
        UbuntuChannel c = (UbuntuChannel)s.getSelectedItem();
        if(c != null)
            outState.putString("utouch_selected_chan", c.getRawName());

        s = (Spinner) m_view.findViewById(R.id.version);
        Integer i = (Integer)s.getSelectedItem();
        if(i != null)
            outState.putInt("utouch_selected_ver", i);

        View front = m_view.findViewById(R.id.ubuntu_card_front);
        outState.putBoolean("utouch_flipped", front.getVisibility() == View.GONE);
    }

    @Override
    public View getCardContent(Context context) {
        m_view = LayoutInflater.from(context).inflate(R.layout.ubuntu_card, null);

        Spinner s = (Spinner) m_view.findViewById(R.id.channel);
        s.setOnItemSelectedListener(this);

        Button b = (Button) m_view.findViewById(R.id.install_btn);
        b.setOnClickListener(this);
        b = (Button) m_view.findViewById(R.id.back_btn);
        b.setOnClickListener(this);

        ImageButton ib = (ImageButton)m_view.findViewById(R.id.info_btn);
        ib.setOnClickListener(this);

        TextView t = (TextView)m_view.findViewById(R.id.error_text);
        if((m_error & ERROR_MULTIROM_VER) != 0) {
            String f = t.getResources().getString(R.string.ubuntu_req_multirom);
            t.append(String.format(f, m_manifest.getUbuntuReqMultiROM()) + "\n");
        }

        if((m_error & ERROR_RECOVERY_VER) != 0) {
            String f = t.getResources().getString(R.string.ubuntu_req_recovery);
            String ver = Recovery.DISPLAY_FMT.format(m_manifest.getUbuntuReqRecovery());
            t.append(String.format(f, ver) + "\n");
        }

        if(m_error == ERROR_NONE) {
            UbuntuManifestAsyncTask.instance().setCard(this);
        } else {
            t.setVisibility(View.VISIBLE);
            m_view.findViewById(R.id.progress_bar).setVisibility(View.GONE);
        }

        if(m_savedState != null && m_savedState.getBoolean("utouch_flipped", false)) {
            m_view.findViewById(R.id.ubuntu_card_front).setVisibility(View.GONE);
            m_view.findViewById(R.id.ubuntu_card_back).setVisibility(View.VISIBLE);
        }

        return m_view;
    }

    public void applyResult(UbuntuManifestAsyncTask.Result res) {
        if(m_view == null)
            return;

        View v = m_view.findViewById(R.id.progress_bar);
        v.setVisibility(View.GONE);

        if(res.code == UbuntuManifestAsyncTask.RES_CHANNELS_FAIL) {
            TextView t = (TextView)m_view.findViewById(R.id.error_text);
            t.setVisibility(View.VISIBLE);
            t.setText(R.string.ubuntu_man_failed);

            m_savedState = null;
            return;
        }

        if(res.manifest.getChannels().isEmpty()) {
            TextView t = (TextView)m_view.findViewById(R.id.error_text);
            t.setVisibility(View.VISIBLE);
            t.setText(R.string.ubuntu_man_no_channels);

            m_savedState = null;
            return;
        }

        if(res.freeSpace != -1 && res.freeSpace < MIN_FREE_SPACE) {
            TextView t = (TextView)m_view.findViewById(R.id.error_text);
            t.setText(Utils.getString(R.string.ubuntu_free_space, res.freeSpace, MIN_FREE_SPACE));
            t.setVisibility(View.VISIBLE);
        }

        final int[] views = { R.id.channel_layout, R.id.version_layout, /* R.id.destination_layout,*/ R.id.install_btn };
        for(int i = 0; i < views.length; ++i) {
            v = m_view.findViewById(views[i]);
            v.setVisibility(View.VISIBLE);
        }

        Spinner chanSpinner = (Spinner) m_view.findViewById(R.id.channel);
        m_channelAdapter = new UbuntuChannelsAdapter(m_view.getContext(), res.manifest.getChannels());
        chanSpinner.setAdapter(m_channelAdapter);

        // TODO: support installation to USB drive
        /*m_destAdapter = new ArrayAdapter<String>(m_view.getContext(),
                    android.R.layout.simple_spinner_dropdown_item);
        m_destAdapter.add(m_view.getResources().getString(R.string.internal_memory));
        m_destAdapter.addAll(res.destinations);

        s = (Spinner) m_view.findViewById(R.id.destination);
        s.setAdapter(m_destAdapter);*/

        String preselected = "trusty";
        if(m_savedState != null && m_savedState.containsKey("utouch_selected_chan"))
            preselected = m_savedState.getString("utouch_selected_chan");

        for(int i = 0; i < m_channelAdapter.getCount(); ++i) {
            if(m_channelAdapter.getItem(i).getRawName().equals(preselected)) {
                chanSpinner.setSelection(i);
                break;
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        m_versionAdapter = new ArrayAdapter<Integer>(m_view.getContext(),
                android.R.layout.simple_spinner_dropdown_item);

        UbuntuChannel c = m_channelAdapter.getItem(position);
        m_versionAdapter.addAll(c.getImageVersions());

        Spinner s = (Spinner) m_view.findViewById(R.id.version);
        s.setAdapter(m_versionAdapter);
        s.setSelection(m_versionAdapter.getCount()-1);

        if(m_savedState != null) {
            Integer ver = m_savedState.getInt("utouch_selected_ver");
            if(ver != null && m_versionAdapter != null) {
                for(int i = 0; i < m_versionAdapter.getCount(); ++i) {
                    if(m_versionAdapter.getItem(i).equals(ver)) {
                        s.setSelection(i);
                        break;
                    }
                }
            }
            m_savedState = null;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Spinner s = (Spinner) m_view.findViewById(R.id.version);
        s.setAdapter(null);
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.install_btn:
            {
                Bundle extras = new Bundle();
                Bundle bundle = new Bundle();
                bundle.putString("installation_type", "ubuntu");

                UbuntuInstallInfo info = new UbuntuInstallInfo();

                Spinner s = (Spinner) m_view.findViewById(R.id.channel);
                UbuntuChannel chan = (UbuntuChannel)s.getSelectedItem();

                s = (Spinner) m_view.findViewById(R.id.version);
                Integer version = (Integer)s.getSelectedItem();

                chan.fillInstallFilesForVer(info.installFiles, version);
                info.channelName = chan.getRawName();

                UbuntuManifestAsyncTask.instance().putInstallInfo(info);

                extras.putBundle("installation_info", bundle);

                m_listener.startActivity(extras, MainActivity.ACT_INSTALL_UBUNTU, InstallActivity.class);
                break;
            }
            case R.id.info_btn:
            {
                rotateCard(true);
                break;
            }
            case R.id.back_btn:
            {
                rotateCard(false);
                break;
            }
        }
    }

    private void rotateCard(boolean frontToback) {
        AnimatorSet set = (AnimatorSet) AnimatorInflater.loadAnimator(m_view.getContext(),
                R.anim.card_flip);
        set.setTarget(mCardLayout);

        View front = m_view.findViewById(R.id.ubuntu_card_front);
        View back = m_view.findViewById(R.id.ubuntu_card_back);

        ArrayList<Animator> s = set.getChildAnimations();
        for(int i = 0; i < s.size(); ++i) {
            Animator a = s.get(i);
            if(a.getDuration() == 1) {
                a.addListener(new FlipAnimationListener(front, back, frontToback));
                break;
            }
        }
        set.start();
    }

    private static class FlipAnimationListener extends AnimatorListenerAdapter {
        private View m_front, m_back;
        private boolean m_frontToBack;

        public FlipAnimationListener(View front, View back, boolean frontToBack) {
            m_front = front;
            m_back = back;
            m_frontToBack = frontToBack;
        }

        @Override
        public void onAnimationStart(Animator animation) {
            super.onAnimationStart(animation);
            m_front.setVisibility(m_frontToBack ? View.GONE : View.VISIBLE);
            m_back.setVisibility(m_frontToBack ? View.VISIBLE : View.GONE);
        }
    }

    private View m_view;
    private UbuntuChannelsAdapter m_channelAdapter;
    private ArrayAdapter<Integer> m_versionAdapter;
    private ArrayAdapter<String> m_destAdapter;
    private StartInstallListener m_listener;
    private Manifest m_manifest;
    private MultiROM m_multirom;
    private Recovery m_recovery;
    private Bundle m_savedState;
    private int m_error;
}
