package com.tassadar.multirommgr;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;

import com.fima.cardsui.views.CardUI;


public class MainActivity extends Activity {

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

    private CardUI mCardView;
}
