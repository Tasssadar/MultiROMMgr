package com.tassadar.multirommgr;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Spinner;

import com.fima.cardsui.objects.Card;

import java.util.Date;

public class InstallCard extends Card implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {

    public InstallCard(Manifest manifest, boolean forceRecovery, StartInstallListener listener) {
        super();
        m_manifest = manifest;
        m_listener = listener;
        m_forceRecovery = forceRecovery;
    }

    @Override
    public View getCardContent(Context context) {
        m_view = LayoutInflater.from(context).inflate(R.layout.install_card, null);

        Resources res = m_view.getResources();

        Date rec_date = m_manifest.getRecoveryVersion();
        String recovery_ver = Recovery.DISPLAY_FMT.format(rec_date);

        CheckBox b = (CheckBox)m_view.findViewById(R.id.install_multirom);
        b.setText(res.getString(R.string.install_multirom, m_manifest.getMultiromVersion()));
        b.setChecked(m_manifest.hasMultiromUpdate());
        b.setOnCheckedChangeListener(this);

        b = (CheckBox)m_view.findViewById(R.id.install_recovery);
        b.setText(res.getString(R.string.install_recovery, recovery_ver));
        b.setChecked(m_manifest.hasRecoveryUpdate());
        b.setOnCheckedChangeListener(this);

        // Force user to install recovery if not yet installed - it is needed to flash ZIPs
        if(m_manifest.hasRecoveryUpdate() && m_forceRecovery) {
            b.append(Html.fromHtml(res.getString(R.string.required)));
            b.setClickable(false);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
                android.R.layout.simple_spinner_dropdown_item);
        adapter.addAll(m_manifest.getKernels().keySet());
        Spinner s = (Spinner)m_view.findViewById(R.id.kernel_options);
        s.setAdapter(adapter);
        s.setEnabled(false);

        b = (CheckBox)m_view.findViewById(R.id.install_kernel);
        b.setOnCheckedChangeListener(this);
        b.setEnabled(!adapter.isEmpty());

        Button install_btn = (Button)m_view.findViewById(R.id.install_btn);
        install_btn.setOnClickListener(this);

        ImageButton changelog_btn = (ImageButton)m_view.findViewById(R.id.changelog_btn);
        if(m_manifest.getChangelogs() == null || m_manifest.getChangelogs().length == 0)
            changelog_btn.setVisibility(View.GONE);
        else
            changelog_btn.setOnClickListener(this);

        enableInstallBtn();
        return m_view;
    }

    @Override
    public void onCheckedChanged(CompoundButton btn, boolean checked) {
        if(btn.getId() == R.id.install_kernel) {
            Spinner s = (Spinner)m_view.findViewById(R.id.kernel_options);
            s.setEnabled(checked);
        }
        enableInstallBtn();
    }

    private void enableInstallBtn() {
        Button install_btn = (Button)m_view.findViewById(R.id.install_btn);
        final int[] ids = { R.id.install_multirom, R.id.install_recovery, R.id.install_kernel };
        for(int i = 0; i < ids.length; ++i) {
            if(((CheckBox)m_view.findViewById(ids[i])).isChecked()) {
                install_btn.setEnabled(true);
                return;
            }
        }
        install_btn.setEnabled(false);
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.install_btn:
            {
                Bundle bundle = new Bundle();
                bundle.putString("installation_type", "multirom");

                CheckBox b = (CheckBox)m_view.findViewById(R.id.install_multirom);
                bundle.putBoolean("install_multirom", b.isChecked());

                b = (CheckBox)m_view.findViewById(R.id.install_recovery);
                bundle.putBoolean("install_recovery", b.isChecked());

                b = (CheckBox)m_view.findViewById(R.id.install_kernel);
                bundle.putBoolean("install_kernel", b.isChecked());

                if(b.isChecked()) {
                    Spinner s = (Spinner)m_view.findViewById(R.id.kernel_options);
                    String name = (String)s.getAdapter().getItem(s.getSelectedItemPosition());
                    bundle.putString("kernel_name", name);
                }

                m_listener.startActivity(bundle, MainActivity.ACT_INSTALL_MULTIROM, InstallActivity.class);
                break;
            }
            case R.id.changelog_btn:
            {
                Changelog[] logs = m_manifest.getChangelogs();
                String[] names = new String[logs.length];
                String[] urls = new String[logs.length];

                for(int i = 0; i < logs.length; ++i) {
                    names[i] = logs[i].name;
                    urls[i] = logs[i].url;
                }

                Bundle b = new Bundle();
                b.putStringArray("changelog_names", names);
                b.putStringArray("changelog_urls", urls);

                m_listener.startActivity(b, MainActivity.ACT_CHANGELOG, ChangelogActivity.class);
                break;
            }
        }

    }

    private Manifest m_manifest;
    private View m_view;
    private StartInstallListener m_listener;
    private boolean m_forceRecovery;
}

