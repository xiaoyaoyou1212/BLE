package com.vise.baseble.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/8/5 20:44.
 */
public class BleDeviceInfo implements Parcelable {

    protected BleDeviceInfo(Parcel in) {
    }

    public static final Creator<BleDeviceInfo> CREATOR = new Creator<BleDeviceInfo>() {
        @Override
        public BleDeviceInfo createFromParcel(Parcel in) {
            return new BleDeviceInfo(in);
        }

        @Override
        public BleDeviceInfo[] newArray(int size) {
            return new BleDeviceInfo[size];
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
