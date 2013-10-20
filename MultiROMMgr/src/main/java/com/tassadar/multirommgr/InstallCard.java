package com.tassadar.multirommgr;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;

import com.fima.cardsui.objects.Card;

import java.text.SimpleDateFormat;
import java.util.Date;

public class InstallCard extends Card implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {

    public InstallCard(String title, Manifest manifest) {
        super(title);
        m_manifest = manifest;
    }

    @Override
    public View getCardContent(Context context) {
        m_view = LayoutInflater.from(context).inflate(R.layout.install_card, null);

        Resources res = m_view.getResources();

        Date rec_date = m_manifest.getRecoveryVersion();
        String recovery_ver = new SimpleDateFormat("yyyy-MM-dd (m)").format(rec_date);

        CheckBox b = (CheckBox)m_view.findViewById(R.id.install_multirom);
        b.setText(res.getString(R.string.install_multirom, m_manifest.getMultiromVersion()));
        b.setChecked(m_manifest.hasMultiromUpdate());
        b.setOnCheckedChangeListener(this);

        b = (CheckBox)m_view.findViewById(R.id.install_recovery);
        b.setText(res.getString(R.string.install_recovery, recovery_ver));
        b.setChecked(m_manifest.hasRecoveryUpdate());
        b.setOnCheckedChangeListener(this);

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
        Intent i = new Intent(m_view.getContext(), InstallActivity.class);

        CheckBox b = (CheckBox)m_view.findViewById(R.id.install_multirom);
        i.putExtra("install_multirom", b.isChecked());

        b = (CheckBox)m_view.findViewById(R.id.install_recovery);
        i.putExtra("install_recovery", b.isChecked());

        b = (CheckBox)m_view.findViewById(R.id.install_kernel);
        i.putExtra("install_kernel", b.isChecked());

        if(b.isChecked()) {
            Spinner s = (Spinner)m_view.findViewById(R.id.kernel_options);
            String name = (String)s.getAdapter().getItem(s.getSelectedItemPosition());
            i.putExtra("kernel_name", name);
        }

        m_view.getContext().startActivity(i);
    }

    private Manifest m_manifest;
    private View m_view;
}

