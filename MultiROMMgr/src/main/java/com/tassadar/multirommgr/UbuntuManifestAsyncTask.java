package com.tassadar.multirommgr;

import android.os.AsyncTask;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class UbuntuManifestAsyncTask extends AsyncTask<Device, Void, UbuntuManifestAsyncTask.Result> {

    static final int RES_OK                 = 0x00;
    static final int RES_CHANNELS_FAIL      = 0x01;

    public interface UbuntuManifestAsyncTaskListener {
        public void onTaskFinished(Result res);
    }

    private static UbuntuManifestAsyncTask instance = null;
    public static UbuntuManifestAsyncTask instance() {
        if(instance == null)
            instance = new UbuntuManifestAsyncTask();
        return instance;
    }

    public static void destroy() {
        instance = null;
    }

    private UbuntuManifestAsyncTask() {
        super();
        m_card = new WeakReference<UbuntuCard>(null);
        m_res = null;
        m_listener = null;
    }

    public void setListener(UbuntuManifestAsyncTaskListener listener) {
        m_listener = listener;
        if(m_listener != null && m_res != null)
            m_listener.onTaskFinished(m_res);
    }

    public void setCard(UbuntuCard card) {
        m_card = new WeakReference<UbuntuCard>(card);
        applyResult();
    }

    public void executeTask(Device dev) {
        if(this.getStatus() == Status.PENDING)
            this.execute(dev);
    }

    @Override
    protected UbuntuManifestAsyncTask.Result doInBackground(Device... dev) {
        Result res = new Result();

        UbuntuManifest man = new UbuntuManifest();
        if(!man.downloadAndParse(dev[0])) {
            res.code = RES_CHANNELS_FAIL;
            return res;
        }

        res.manifest = man;

        // TODO: code to find USB partitions
        return res;
    }

    protected void onPostExecute(Result res) {
        m_res = res;
        applyResult();
        if(m_listener != null && m_res != null)
            m_listener.onTaskFinished(res);
    }

    private void applyResult() {
        UbuntuCard card = m_card.get();

        if(card == null || m_res == null)
            return;

        card.applyResult(m_res);
    }

    public void putInstallInfo(UbuntuInstallInfo i) {
        m_installInfo = i;
    }

    public UbuntuInstallInfo getInstallInfo() {
        return m_installInfo;
    }

    public class Result {
        public int code = RES_OK;
        public UbuntuManifest manifest = null;
        public ArrayList<String> m_destinations = new ArrayList<String>();
    }

    private WeakReference<UbuntuCard> m_card;
    private UbuntuManifestAsyncTaskListener m_listener;
    private Result m_res;
    private UbuntuInstallInfo m_installInfo;
}
