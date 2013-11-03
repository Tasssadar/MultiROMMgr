package com.tassadar.multirommgr;

import android.os.AsyncTask;

public class UbuntuInstallAsyncTask extends AsyncTask<Void, Void, Void> implements Utils.DownloadProgressListener {
    @Override
    protected Void doInBackground(Void... voids) {
        return null;
    }

    @Override
    public void onProgressChanged(int downloaded, int total) {

    }

    @Override
    public boolean isCanceled() {
        return false;
    }
}
