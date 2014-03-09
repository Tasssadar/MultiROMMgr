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

import android.content.Context;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;

import com.tassadar.multirommgr.R;
import com.tassadar.multirommgr.StatusAsyncTask;

public class StatusCard extends StaticCard implements View.OnClickListener {

    public StatusCard(PopupMenu.OnMenuItemClickListener listener) {
        super(R.layout.status_card);
        m_uninstallListener = listener;
    }

    @Override
    public View getCardContent(Context context) {
        View view = super.getCardContent(context);

        ImageButton b = (ImageButton)view.findViewById(R.id.uninstall_btn);
        b.setOnClickListener(this);

        StatusAsyncTask.instance().setStatusCardLayout(view);
        return view;
    }

    @Override
    public void onClick(View view) {
        PopupMenu popup = new PopupMenu(view.getContext(), view);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.uninstall_multirom, popup.getMenu());
        popup.setOnMenuItemClickListener(m_uninstallListener);
        popup.show();
    }

    private PopupMenu.OnMenuItemClickListener m_uninstallListener;
}
