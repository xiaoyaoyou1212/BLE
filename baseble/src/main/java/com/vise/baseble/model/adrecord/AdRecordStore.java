package com.vise.baseble.model.adrecord;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/8/7 21:54.
 */
public class AdRecordStore implements Parcelable {
    protected AdRecordStore(Parcel in) {
    }

    public static final Creator<AdRecordStore> CREATOR = new Creator<AdRecordStore>() {
        @Override
        public AdRecordStore createFromParcel(Parcel in) {
            return new AdRecordStore(in);
        }

        @Override
        public AdRecordStore[] newArray(int size) {
            return new AdRecordStore[size];
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
