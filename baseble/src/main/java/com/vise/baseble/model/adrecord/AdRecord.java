package com.vise.baseble.model.adrecord;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/8/7 21:53.
 */
public class AdRecord implements Parcelable {
    protected AdRecord(Parcel in) {
    }

    public static final Creator<AdRecord> CREATOR = new Creator<AdRecord>() {
        @Override
        public AdRecord createFromParcel(Parcel in) {
            return new AdRecord(in);
        }

        @Override
        public AdRecord[] newArray(int size) {
            return new AdRecord[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
    }
}
