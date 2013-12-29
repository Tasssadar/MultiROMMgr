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
import android.view.LayoutInflater;
import android.view.View;

import com.fima.cardsui.objects.Card;

public class StaticCard extends Card {

    public StaticCard(String title, int layoutResourceId) {
        super(title);
        m_layoutId = layoutResourceId;
    }

    public StaticCard(int layoutResourceId) {
        super();
        m_layoutId = layoutResourceId;
    }

    @Override
    public View getCardContent(Context context) {
        View view = LayoutInflater.from(context).inflate(m_layoutId, null);
        return view;
    }

    protected int m_layoutId;
}
