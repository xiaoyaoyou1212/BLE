package com.vise.baseble.callback;

import com.vise.baseble.core.BluetoothGattChannel;
import com.vise.baseble.exception.BleException;
import com.vise.baseble.model.BluetoothLeDevice;

/**
 * @Description: 操作数据回调
 * @author: <a href="http://xiaoyaoyou1212.360doc.com">DAWI</a>
 * @date: 2017/10/17 19:42
 */
public interface IBleCallback {
    void onSuccess(byte[] data, BluetoothGattChannel bluetoothGattChannel, BluetoothLeDevice bluetoothLeDevice);

    void onFailure(BleException exception);
}
