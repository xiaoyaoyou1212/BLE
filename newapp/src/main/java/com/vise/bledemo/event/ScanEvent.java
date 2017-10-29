package com.vise.bledemo.event;

import com.vise.baseble.model.BluetoothLeDeviceStore;
import com.vise.xsnow.event.IEvent;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 17/10/29 22:59.
 */
public class ScanEvent implements IEvent {
    private boolean isScanTimeout;
    private boolean isScanFinish;
    private BluetoothLeDeviceStore bluetoothLeDeviceStore;

    public ScanEvent() {
    }

    public boolean isScanTimeout() {
        return isScanTimeout;
    }

    public ScanEvent setScanTimeout(boolean scanTimeout) {
        isScanTimeout = scanTimeout;
        return this;
    }

    public boolean isScanFinish() {
        return isScanFinish;
    }

    public ScanEvent setScanFinish(boolean scanFinish) {
        isScanFinish = scanFinish;
        return this;
    }

    public BluetoothLeDeviceStore getBluetoothLeDeviceStore() {
        return bluetoothLeDeviceStore;
    }

    public ScanEvent setBluetoothLeDeviceStore(BluetoothLeDeviceStore bluetoothLeDeviceStore) {
        this.bluetoothLeDeviceStore = bluetoothLeDeviceStore;
        return this;
    }
}
