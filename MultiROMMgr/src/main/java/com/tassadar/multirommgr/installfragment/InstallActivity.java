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

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tassadar.multirommgr.Device;
import com.tassadar.multirommgr.Manifest;
import com.tassadar.multirommgr.MgrApp;
import com.tassadar.multirommgr.R;
import com.tassadar.multirommgr.SettingsFragment;
import com.tassadar.multirommgr.StatusAsyncTask;
import com.tassadar.multirommgr.Utils;

public class InstallActivity extends AppCompatActivity implements ServiceConnection, InstallListener {
    private static final String TAG = "MROMMgr::InstallActv";

    private static final int BTN_STATE_CANCEL    = 0;
    private static final int BTN_STATE_TRY_AGAIN = 1;
    private static final int BTN_STATE_DONE      = 2;

    private static final int WRITE_EXTERNAL_PERM_REQUEST = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_install);

        m_term = (TextView)findViewById(R.id.term);
        m_progressText = (TextView)findViewById(R.id.progress_text);
        m_progressBar = (ProgressBar)findViewById(R.id.progress_bar);
        m_isCancelEnabled = true;

        setButtonState(BTN_STATE_CANCEL);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        m_installInfo = getIntent().getBundleExtra("installation_info");

        if(savedInstanceState != null && savedInstanceState.getBoolean("completed", false)) {
            m_term.setText(Html.fromHtml(savedInstanceState.getString("log", "")));
            m_progressText.setText(savedInstanceState.getString("status"));

            m_progressBar.setIndeterminate(false);
            m_progressBar.setMax(100);
            m_progressBar.setProgress(100);

            setButtonState(BTN_STATE_DONE);
        }

        Intent i = new Intent(this, InstallService.class);
        startService(i);
        bindService(i, this, BIND_AUTO_CREATE);
    }

    protected void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);

        if(m_service != null && m_service.isInProgress())
            return;

        CharSequence s = m_term.getText();
        if(s instanceof Spanned)
            outState.putString("log", Html.toHtml((Spanned)m_term.getText()));
        else
            outState.putString("log", m_term.getText().toString());

        outState.putBoolean("completed", true);
        outState.putString("status", m_progressText.getText().toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(m_rebootDialog != null) {
            m_rebootDialog.dismiss();
            m_rebootDialog = null;
        }

        if(m_service != null)
            unbindService(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if(requestCode == WRITE_EXTERNAL_PERM_REQUEST) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startInstallation();
            } else {
                onInstallLog("MultiROM needs permission to write to external storage because it has to download some files, please allow it.<br>");
                setButtonState(BTN_STATE_TRY_AGAIN);
                m_progressBar.setIndeterminate(false);
                m_progressBar.setMax(100);
                m_progressBar.setProgress(100);
            }
        }
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

                setButtonState(m_service.wasSuccessful() ? BTN_STATE_DONE : BTN_STATE_TRY_AGAIN);

                int req = m_service.wasRecoveryRequested();
                if(req != 0)
                    requestRecovery(req == 2);
            }
        }
    }

    private void startInstallation() {
        if(m_installInfo == null || !StatusAsyncTask.initialized()) {
            Log.e(TAG, "No installation info!");
            m_term.append("No installation info!");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_PERM_REQUEST);
            return;
        }

        final String type = m_installInfo.getString("installation_type");
        if("multirom".equals(type)) {
            boolean multirom = m_installInfo.getBoolean("install_multirom", false);
            boolean recovery = m_installInfo.getBoolean("install_recovery", false);
            boolean kernel = m_installInfo.getBoolean("install_kernel", false);
            String kernel_name = m_installInfo.getString("kernel_name");

            Manifest man = StatusAsyncTask.instance().getManifest();
            Device dev = StatusAsyncTask.instance().getDevice();

            m_service.startMultiROMInstallation(man, dev, multirom, recovery, kernel, kernel_name);
        } else if("uninstall_multirom".equals(type)) {
            Manifest man = StatusAsyncTask.instance().getManifest();
            Device dev = StatusAsyncTask.instance().getDevice();
            m_service.startMultiROMUninstallation(man, dev);
        } else if("ubuntu".equals(type)) {
            m_service.startUbuntuInstallation(
                    UbuntuManifestAsyncTask.instance().getInstallInfo(),
                    StatusAsyncTask.instance().getMultiROM(),
                    StatusAsyncTask.instance().getDevice());
        } else {
            Log.e(TAG, "Unknown installation type: " + type);
            return;
        }

        m_term.setText("");
        m_isCancelEnabled = true;
        setButtonState(BTN_STATE_CANCEL);

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
                tryBack();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                tryBack();
                return true;
        }
        return false;
    }

    private void tryBack() {
        if(m_service != null) {
            if(m_service.isInProgress() && !m_isCancelEnabled) {
                Toast.makeText(this, R.string.uninterruptable, Toast.LENGTH_SHORT).show();
                return;
            }
            m_service.cancel();
        }
        this.finish();
    }

    public void onControlClicked(View v) {
        switch(m_btnState) {
            case BTN_STATE_CANCEL:
                tryBack();
                break;
            case BTN_STATE_TRY_AGAIN:
                if(m_service != null)
                    startInstallation();
                break;
            case BTN_STATE_DONE:
                if(m_service != null)
                    m_service.cancel();
                this.finish();
                break;
        }
    }

    private void setButtonState(int s) {
        final int[] texts = { R.string.cancel, R.string.try_again, R.string.done };

        Button b = (Button)findViewById(R.id.control_btn);
        b.setText(texts[s]);

        m_btnState = s;
    }

    @Override
    public void onInstallLog(String str) {
        runOnUiThread(new AppendLogRunnable(str));
    }

    @Override
    public void onProgressUpdate(int val, int max, boolean indeterminate, String text) {
        runOnUiThread(new UpdateProgressRunnable(val, max, indeterminate, Html.fromHtml(text), 0));
    }

    @Override
    public void onInstallComplete(boolean success) {
        Spanned text = null;
        if(success)
            text = new SpannedString(Utils.getString(R.string.install_success));
        else
            text = new SpannedString(Utils.getString(R.string.install_failed));

        setResult(RESULT_OK);
        m_isCancelEnabled = true;

        runOnUiThread(new UpdateProgressRunnable(100, 100, false, text, success ? 2 : 1));
    }

    @Override
    public void enableCancel(boolean enabled) {
        m_isCancelEnabled = enabled;
    }

    @Override
    public void requestRecovery(boolean force) {
        SharedPreferences pref = MgrApp.getPreferences();
        if(!pref.getBoolean(SettingsFragment.GENERAL_AUTO_REBOOT, false)) {
            runOnUiThread(new RecoveryDialogRunnable(force));
        } else {
            doReboot(force);
        }
    }

    public void doReboot(boolean force) {
        if(!force) {
            Device d = StatusAsyncTask.instance().getDevice();
            Utils.deployOpenRecoveryScript(d.getCacheDev());
        }
        Utils.reboot("recovery");
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
                    doReboot(m_force);
                }
             });

            if(!m_force) {
                b.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(m_service != null)
                            m_service.setRequestRecovery(0);

                        // Allow user to request recovery again
                        setButtonState(BTN_STATE_TRY_AGAIN);
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
        private int m_finished;

        public UpdateProgressRunnable(int val, int max, boolean indeterminate, Spanned text, int finished) {
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

            if(m_finished > 0) {
                if(m_finished == 2)
                    setButtonState(BTN_STATE_DONE);
                else
                    setButtonState(BTN_STATE_TRY_AGAIN);

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
    private int m_btnState;
    private Bundle m_installInfo;
}
