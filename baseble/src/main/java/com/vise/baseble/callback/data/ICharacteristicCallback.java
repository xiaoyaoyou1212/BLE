package com.vise.baseble.callback.data;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * @Description: 特征值操作回调
 * @author: <a href="http://xiaoyaoyou1212.360doc.com">DAWI</a>
 * @date: 2017-04-24 11:07
 */
public interface ICharacteristicCallback extends IBleCallback {
    void onSuccess(BluetoothGattCharacteristic characteristic);
}
