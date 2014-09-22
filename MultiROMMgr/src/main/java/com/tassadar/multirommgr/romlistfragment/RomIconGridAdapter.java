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

package com.tassadar.multirommgr.romlistfragment;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.fima.cardsui.Utils;
import com.tassadar.multirommgr.R;

public class RomIconGridAdapter extends BaseAdapter {
    private static final Integer[] ROM_ICONS = {
            R.drawable.romic_default,
            R.drawable.romic_android,
			R.drawable.romic_androidl,
            R.drawable.romic_cm1,
            R.drawable.romic_cm2,
            R.drawable.romic_pa1,
            R.drawable.romic_pa2,
            R.drawable.romic_omni,
            R.drawable.romic_aokp1,
            R.drawable.romic_aokp2,
            R.drawable.romic_ubuntu1,
            R.drawable.romic_ubuntu2,
            R.drawable.romic_ubuntu3,
            R.drawable.romic_linux,
            R.drawable.romic_firefox,
            R.drawable.romic_slimkat,
			R.drawable.romic_viperone
    };

    public RomIconGridAdapter(Context c) {
        m_context = c;
    }

    public int getCount() {
        return ROM_ICONS.length;
    }

    public Integer getItem(int position) {
        return ROM_ICONS[position];
    }

    public long getItemId(int position) {
        return position;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            int side = Utils.convertDpToPixelInt(m_context, 58);
            int padding = Utils.convertDpToPixelInt(m_context, 5);

            imageView = new ImageView(m_context);
            imageView.setLayoutParams(new GridView.LayoutParams(side, side));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(padding, padding, padding, padding);
            imageView.setBackgroundResource(R.drawable.transparent_btn);
        } else {
            imageView = (ImageView) convertView;
        }

        imageView.setImageResource(ROM_ICONS[position]);
        return imageView;
    }

    private Context m_context;
}
