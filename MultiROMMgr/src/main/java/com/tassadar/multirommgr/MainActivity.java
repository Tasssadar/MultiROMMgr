package com.tassadar.multirommgr;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;

import com.fima.cardsui.views.CardUI;


public class MainActivity extends Activity implements StatusAsyncTask.StatusAsyncTaskListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCardView = (CardUI) findViewById(R.id.cardsview);
        mCardView.setSwipeable(false);

        start();
    }

    private void start() {
        mCardView.addCard(new StatusCard("Status"), true);
        StatusAsyncTask.instance().setListener(this);
        StatusAsyncTask.instance().execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem it) {
        switch(it.getItemId()) {
            case R.id.action_refresh:
                StatusAsyncTask.destroy();
                mCardView.clearCards();
                start();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onTaskFinished(StatusAsyncTask.Result res) {
        if(res.manifest != null)
            mCardView.addCard(new InstallCard("Install/update", res.manifest), true);
    }

    private CardUI mCardView;
}
