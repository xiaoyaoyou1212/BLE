package com.vise.baseble.callback;

import android.bluetooth.BluetoothGatt;

import com.vise.baseble.exception.BleException;

/**
 * @Description: 连接设备回调
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/8/20 16:21.
 */
public interface IConnectCallback {
    void onConnectSuccess(BluetoothGatt gatt, int status);

    void onConnectFailure(BleException exception);

    void onDisconnect();
}
