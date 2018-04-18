package com.vise.baseble.callback.scan;

import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.baseble.model.BluetoothLeDeviceStore;

/**
 * @Description: 扫描回调
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 17/9/10 18:14.
 */
public interface IScanCallback {
    //发现设备
    void onDeviceFound(BluetoothLeDevice bluetoothLeDevice);

    //扫描完成
    void onScanFinish(BluetoothLeDeviceStore bluetoothLeDeviceStore);

    //扫描超时
    void onScanTimeout();

}
