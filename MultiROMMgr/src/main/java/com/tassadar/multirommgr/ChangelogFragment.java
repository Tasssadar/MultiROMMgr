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

package com.tassadar.multirommgr;


import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ChangelogFragment extends Fragment {

    public static ChangelogFragment newInstance(String url) {
        ChangelogFragment f = new ChangelogFragment();

        Bundle b = new Bundle();
        b.putString("url", url);

        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_url = getArguments() != null ? getArguments().getString("url") : null;

        if(savedInstanceState != null)
            m_text = savedInstanceState.getString("changelog");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_changelog, container, false);

        if(m_text != null)
            setChangelog(m_text, v);
        else
            new DownloadTask().execute(m_url);

        m_text = null;
        return v;
    }

    public void onDestroyView() {
        super.onDestroyView();

        TextView t = (TextView)getView().findViewById(R.id.changelog);
        m_text = t.getText().toString();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if(m_text == null && getView() != null) {
            TextView t = (TextView)getView().findViewById(R.id.changelog);
            m_text = t.getText().toString();
        }

        if(m_text != null)
            outState.putString("changelog", m_text);
    }

    private void setChangelog(String c, View view) {
        View v = view.findViewById(R.id.progress_bar);
        v.setVisibility(View.GONE);
        v = view.findViewById(R.id.scrollview);
        v.setVisibility(View.VISIBLE);

        TextView t = (TextView)view.findViewById(R.id.changelog);
        if(c != null)
            t.setText(c);
        else
        {
            t.setText(R.string.changelog_failed);
            t.setGravity(Gravity.CENTER);
        }
    }

    private class DownloadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... url) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(8192);
            try {
                if(!Utils.downloadFile(url[0], out, null))
                    return null;
            } catch(IOException e) {
                e.printStackTrace();
                return null;
            }

            try { out.close(); } catch (IOException e) { }

            return out.toString();
        }

        @Override
        protected void onPostExecute(String res) {
            View view = getView();
            if(view == null)
                return;

            setChangelog(res, view);
        }
    }

    private String m_url;
    private String m_text;
}
