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

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.util.Comparator;

public class Rom implements Parcelable {
    public static final int ROM_PRIMARY   = 0;
    public static final int ROM_SECONDARY = 1;

    public static class NameComparator implements Comparator<Rom> {
        @Override
        public int compare(Rom a, Rom b) {
            if(a.type == Rom.ROM_PRIMARY)
                return -1;

            if(b.type == Rom.ROM_PRIMARY)
                return 1;

            return a.name.compareToIgnoreCase(b.name);
        }
    }

    public static final Parcelable.Creator<Rom> CREATOR
            = new Parcelable.Creator<Rom>() {
        public Rom createFromParcel(Parcel in) {
            return new Rom(in);
        }

        public Rom[] newArray(int size) {
            return new Rom[size];
        }
    };

    public Rom(String name, int type) {
        this.name = name;
        this.type = type;
    }

    public Rom(Parcel in) {
        this.name = in.readString();
        this.type = in.readInt();
        this.icon_id = in.readInt();
        this.icon_hash = (String)in.readValue(String.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeInt(this.type);
        dest.writeInt(this.icon_id);
        dest.writeValue(this.icon_hash);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public Drawable getIcon() {
        if(m_icon == null)
            m_icon = loadIcon();
        return m_icon;
    }

    private Drawable loadIcon() {
        Drawable res = null;
        Resources r = MgrApp.getAppContext().getResources();

        if(this.icon_id == R.id.user_defined_icon) {
            File path = new File(MgrApp.getAppContext().getDir("icons", 0),
                    this.icon_hash + ".png");
            res = Drawable.createFromPath(path.getAbsolutePath());
        } else {
            try {
                res = r.getDrawable(this.icon_id);
            } catch(Resources.NotFoundException e) {
                // expected
            }
        }

        if(res == null)
            res = r.getDrawable(R.drawable.romic_default);

        return res;
    }

    public void resetIconDrawable() {
        m_icon = null;
    }

    public String name;
    public int type;
    public int icon_id = R.drawable.romic_default;
    public String icon_hash = null;

    private Drawable m_icon;
}
