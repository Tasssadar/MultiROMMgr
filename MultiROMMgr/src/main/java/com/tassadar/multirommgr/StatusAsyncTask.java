package com.tassadar.multirommgr;

import android.os.AsyncTask;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;

import eu.chainfire.libsuperuser.Shell;

public class StatusAsyncTask extends AsyncTask <Void, Void, StatusAsyncTask.Result> {

    static final int RES_OK                 = 0x00;
    static final int RES_NO_SU              = 0x01;
    static final int RES_NO_MULTIROM        = 0x02;
    static final int RES_FAIL_MROM_VER      = 0x04;
    static final int RES_UNSUPPORTED        = 0x08;
    static final int RES_NO_RECOVERY        = 0x10;

    private static StatusAsyncTask instance = null;
    public static StatusAsyncTask instance() {
        if(instance == null)
            instance = new StatusAsyncTask();
        return instance;
    }

    public static void destroy() {
        instance = null;
    }

    private StatusAsyncTask() {
        super();
        m_layout = new WeakReference<View>(null);
        m_res = null;
    }

    public void setStatusCardLayout(View layout) {
        m_layout = new WeakReference<View>(layout);
        applyResult();
    }

    public void execute() {
        if(this.getStatus() == Status.PENDING)
            this.execute((Void) null);
    }

    protected Result doInBackground(Void ...arg) {
        Result res = new Result();

        Device dev = Device.load(Build.BOARD);
        if(dev == null) {
            res.code = RES_UNSUPPORTED;
            return res;
        }

        if(!Shell.SU.available()) {
            res.code = RES_NO_SU;
            return res;
        }

        MultiROM m = new MultiROM();
        if(!m.findMultiROMDir()) {
            res.code |= RES_NO_MULTIROM;
        } else {
            if(!m.findVersion())
                res.code |= RES_FAIL_MROM_VER;
            else
                res.multirom = m;
        }

        Recovery rec = new Recovery();
        if(!rec.findRecoveryVersion(dev))
            res.code |= RES_NO_RECOVERY;
        else
            res.recovery = rec;

        res.kernel = new Kernel();
        res.kernel.findKexecHardboot(m != null ? m.getPath() + "busybox" : "");

        return res;
    }

    protected void onPostExecute(Result res) {
        m_res = res;
        applyResult();
    }

    protected void applyResult() {
        View l = m_layout.get();

        if(l == null || m_res == null)
            return;

        View v = l.findViewById(R.id.progress_bar);
        v.setVisibility(View.GONE);

        TextView t = (TextView) l.findViewById(R.id.error_text);
        t.setText("");
        switch (m_res.code) {
            case RES_NO_SU:
                t.setText(R.string.no_su);
                t.setVisibility(View.VISIBLE);
                return;
            case RES_UNSUPPORTED: {
                String s = t.getResources().getString(R.string.unsupported, Build.BOARD);
                t.setText(s);
                t.setVisibility(View.VISIBLE);
                return;
            }
        }

        if ((m_res.code & RES_FAIL_MROM_VER) != 0)
            t.append("\n" + t.getResources().getString(R.string.mrom_ver_fail));
        if ((m_res.code & RES_NO_MULTIROM) != 0)
            t.append("\n" + t.getResources().getString(R.string.no_multirom));
        if ((m_res.code & RES_NO_RECOVERY) != 0)
            t.append("\n" + t.getResources().getString(R.string.no_recovery));

        if (t.getText().length() != 0)
            t.setVisibility(View.VISIBLE);

        String recovery_date = null;
        if (m_res.recovery != null) {
            recovery_date = new SimpleDateFormat("yyyy-MM-dd (m)")
                    .format(m_res.recovery.getVersion());
        }

        String kexec_text = t.getResources().getString(
                m_res.kernel.hasKexec() ? R.string.has_kexec : R.string.no_kexec);

        t = (TextView) l.findViewById(R.id.info_text);
        Spanned s = Html.fromHtml(t.getResources().getString(R.string.status_text,
                m_res.multirom != null ? m_res.multirom.getVersion() : "N/A",
                "",
                recovery_date != null ? recovery_date : "N/A",
                "",
                kexec_text));
        t.setText(s);
        t.setVisibility(View.VISIBLE);
    }

    protected class Result {
        public int code = RES_OK;
        public Recovery recovery = null;
        public MultiROM multirom = null;
        public Kernel kernel = null;
    }

    private WeakReference<View> m_layout;
    private Result m_res;
}
