package com.tassadar.multirommgr;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;

import com.fima.cardsui.views.CardUI;


public class MainActivity extends Activity implements StatusAsyncTask.StatusAsyncTaskListener,
        UbuntuManifestAsyncTask.UbuntuManifestAsyncTaskListener, StartInstallListener {

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

        mCardView = (CardUI) findViewById(R.id.cardsview);
        mCardView.setSwipeable(false);
        mCardView.setSlideIn(!StatusAsyncTask.instance().isComplete());

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

    private void start() {
        // not the same data, something might have changed
        if(!StatusAsyncTask.instance().isComplete())
            m_cardsSavedState = null;

        mCardView.addCard(new StatusCard(), true);
        StatusAsyncTask.instance().setListener(this);
        StatusAsyncTask.instance().execute();

        if(m_menu != null)
            m_menu.findItem(R.id.action_refresh).setEnabled(false);
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
                             case 0:
                                 Utils.reboot("");
                                 break;
                             case 1:
                                 Utils.reboot("recovery");
                                 break;
                             case 2:
                                 Utils.reboot("bootloader");
                                 break;
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

            if(res.multirom != null && res.recovery != null && res.device.supportsUbuntuTouch()) {
                UbuntuManifestAsyncTask.instance().setListener(this);
                mCardView.addCard(new UbuntuCard(m_cardsSavedState, this, res.manifest, res.multirom, res.recovery));
                hasUbuntu = true;
            }

            mCardView.refresh();
        }

        if(m_menu != null && !hasUbuntu)
            m_menu.findItem(R.id.action_refresh).setEnabled(true);

        // Saved state is not needed anymore
        m_cardsSavedState = null;
    }

    @Override
    public void onUbuntuTaskFinished(UbuntuManifestAsyncTask.Result res) {
        if(m_menu != null)
            m_menu.findItem(R.id.action_refresh).setEnabled(true);
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

    private CardUI mCardView;
    private Menu m_menu;
    private Bundle m_cardsSavedState;
}
