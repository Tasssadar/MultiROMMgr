package com.tassadar.multirommgr;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

public class InstallActivity extends Activity implements ServiceConnection, InstallListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_install);

        m_term = (TextView)findViewById(R.id.term);
        m_progressText = (TextView)findViewById(R.id.progress_text);
        m_progressBar = (ProgressBar)findViewById(R.id.progress_bar);

        Intent i = new Intent(this, InstallService.class);
        startService(i);
        bindService(i, this, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        m_service = ((InstallService.InstallServiceBinder)iBinder).get();
        m_service.setListener(this);

        if(!m_service.isInProgress()) {
            Intent i = getIntent();
            boolean multirom = i.getBooleanExtra("install_multirom", false);
            boolean recovery = i.getBooleanExtra("install_recovery", false);
            boolean kernel = i.getBooleanExtra("install_kernel", false);
            String kernel_name = i.getStringExtra("kernel_name");

            Manifest man = StatusAsyncTask.instance().getManifest();

            m_service.startInstallation(man, multirom, recovery, kernel, kernel_name);
        } else {
            m_term.setText(Html.fromHtml(m_service.getFullLog()));
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        m_service = null;
    }

    public void onCancelClicked(View v) {
        m_service.cancel();
        this.finish();
    }

    @Override
    public void onInstallLog(String str) {
        runOnUiThread(new AppendLogRunnable(str));
    }

    @Override
    public void onProgressUpdate(int val, int max, boolean indeterminate, String text) {
        runOnUiThread(new UpdateProgressRunnable(val, max, indeterminate, Html.fromHtml(text)));
    }

    @Override
    public void onInstallComplete() {

    }

    private class AppendLogRunnable implements Runnable {
        public Spanned m_str;
        public AppendLogRunnable(String str) {
            m_str = Html.fromHtml(str);
        }

        public void run() {
            m_term.append(m_str);
        }
    }

    private class UpdateProgressRunnable implements Runnable {
        private int m_val;
        private int m_max;
        private boolean m_indeterminate;
        private Spanned m_text;
        public UpdateProgressRunnable(int val, int max, boolean indeterminate, Spanned text) {
            m_val = val;
            m_max = max;
            m_indeterminate = indeterminate;
            m_text = text;
        }

        public void run() {
            m_progressBar.setIndeterminate(m_indeterminate);
            if(!m_indeterminate) {
                m_progressBar.setMax(m_max);
                m_progressBar.setProgress(m_val);
            }
            m_progressText.setText(m_text);
        }
    }

    private InstallService m_service;
    private TextView m_term;
    private TextView m_progressText;
    private ProgressBar m_progressBar;
}
