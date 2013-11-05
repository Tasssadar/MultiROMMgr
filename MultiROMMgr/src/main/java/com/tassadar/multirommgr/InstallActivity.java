package com.tassadar.multirommgr;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class InstallActivity extends Activity implements ServiceConnection, InstallListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_install);

        m_term = (TextView)findViewById(R.id.term);
        m_progressText = (TextView)findViewById(R.id.progress_text);
        m_progressBar = (ProgressBar)findViewById(R.id.progress_bar);
        m_isCancelEnabled = true;

        Intent i = new Intent(this, InstallService.class);
        startService(i);
        bindService(i, this, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(m_rebootDialog != null) {
            m_rebootDialog.dismiss();
            m_rebootDialog = null;
        }

        unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        m_service = ((InstallService.InstallServiceBinder)iBinder).get();
        m_service.setListener(this);

        if(!m_service.isInProgress() && !m_service.wasCompleted()) {
            startInstallation();
        } else {
            m_term.setText(Html.fromHtml(m_service.getFullLog()));
            m_isCancelEnabled = m_service.getEnableCancel();
            if(m_service.wasCompleted()) {
                m_progressBar.setIndeterminate(false);
                m_progressBar.setMax(100);
                m_progressBar.setProgress(100);

                Button b = (Button)findViewById(R.id.control_btn);
                b.setText(R.string.try_again);

                int req = m_service.wasRecoveryRequested();
                if(req != 0)
                    requestRecovery(req == 2);
            }
        }
    }

    private void startInstallation() {
        Intent i = getIntent();
        final String type = i.getStringExtra("installation_type");
        if(type.equals("multirom")) {
            boolean multirom = i.getBooleanExtra("install_multirom", false);
            boolean recovery = i.getBooleanExtra("install_recovery", false);
            boolean kernel = i.getBooleanExtra("install_kernel", false);
            String kernel_name = i.getStringExtra("kernel_name");

            Manifest man = StatusAsyncTask.instance().getManifest();

            m_service.startMultiROMInstallation(man, multirom, recovery, kernel, kernel_name);
        } else if(type.equals("ubuntu")) {
            m_service.startUbuntuInstallation(
                    UbuntuManifestAsyncTask.instance().getInstallInfo(),
                    StatusAsyncTask.instance().getMultiROM());
        } else {
            Log.e("InstallActivity", "Unknown installation type: " + type);
            return;
        }

        m_term.setText("");
        m_isCancelEnabled = true;

        setResult(RESULT_CANCELED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        m_service = null;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch(keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if(m_service != null) {
                    if(m_service.isInProgress() && !m_isCancelEnabled) {
                        Toast.makeText(this, R.string.uninterruptable, Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    m_service.cancel();
                }
                this.finish();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void onControlClicked(View v) {
        if(m_service == null)
            return;

        if(m_service.isInProgress()) {
            if(m_isCancelEnabled) {
                m_service.cancel();
                this.finish();
            } else {
                Toast.makeText(this, R.string.uninterruptable, Toast.LENGTH_SHORT).show();
            }
        } else {
            startInstallation();
            ((Button)v).setText(R.string.cancel);
        }
    }

    @Override
    public void onInstallLog(String str) {
        runOnUiThread(new AppendLogRunnable(str));
    }

    @Override
    public void onProgressUpdate(int val, int max, boolean indeterminate, String text) {
        runOnUiThread(new UpdateProgressRunnable(val, max, indeterminate, Html.fromHtml(text), false));
    }

    @Override
    public void onInstallComplete(boolean success) {
        Spanned text = null;
        if(success)
            text = new SpannedString("Installation was successful.");
        else
            text = new SpannedString("Installation FAILED!");

        setResult(RESULT_OK);

        runOnUiThread(new UpdateProgressRunnable(100, 100, false, text, true));
    }

    @Override
    public void enableCancel(boolean enabled) {
        m_isCancelEnabled = enabled;
    }

    @Override
    public void requestRecovery(boolean force) {
        runOnUiThread(new RecoveryDialogRunnable(force));
    }

    private class RecoveryDialogRunnable implements Runnable {
        private boolean m_force;
        public RecoveryDialogRunnable(boolean force) {
            m_force = force;
        }

        @Override
        public void run() {
            AlertDialog.Builder b = new AlertDialog.Builder(InstallActivity.this);
            b.setTitle(R.string.reboot)
             .setMessage(R.string.reboot_message)
             .setCancelable(!m_force)
             .setPositiveButton(R.string.reboot, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if(!m_force) {
                        Device d = StatusAsyncTask.instance().getManifest().getDevice();
                        Utils.deployOpenRecoveryScript(d.getCacheDev());
                    }
                    Utils.reboot("recovery");
                }
             });

            if(!m_force) {
                b.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(m_service != null)
                            m_service.setRequestRecovery(0);
                    }
                });
            }
            m_rebootDialog = b.create();
            m_rebootDialog.show();
        }
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
        private boolean m_finished;

        public UpdateProgressRunnable(int val, int max, boolean indeterminate, Spanned text, boolean finished) {
            m_val = val;
            m_max = max;
            m_indeterminate = indeterminate;
            m_text = text;
            m_finished = finished;
        }

        public void run() {
            m_progressBar.setIndeterminate(m_indeterminate);
            if(!m_indeterminate) {
                m_progressBar.setMax(m_max);
                m_progressBar.setProgress(m_val);
            }
            m_progressText.setText(m_text);

            if(m_finished) {
                Button b = (Button)findViewById(R.id.control_btn);
                b.setText(R.string.try_again);

                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }

        }
    }

    private InstallService m_service;
    private TextView m_term;
    private TextView m_progressText;
    private ProgressBar m_progressBar;
    private boolean m_isCancelEnabled;
    private AlertDialog m_rebootDialog;
}
