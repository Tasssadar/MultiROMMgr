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

import android.os.Parcel;
import android.os.Parcelable;

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
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeInt(this.type);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String name;
    public int type;
}
