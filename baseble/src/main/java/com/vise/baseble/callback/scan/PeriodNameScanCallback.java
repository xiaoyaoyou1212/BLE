package com.vise.baseble.callback.scan;

import android.bluetooth.BluetoothDevice;

import com.vise.baseble.common.State;
import com.vise.baseble.model.BluetoothLeDevice;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Description: 扫描指定名字设备回调
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/8/10 22:27.
 */
public abstract class PeriodNameScanCallback extends PeriodScanCallback {
    private String name;
    private AtomicBoolean hasFound = new AtomicBoolean(false);

    public PeriodNameScanCallback(String name) {
        super();
        this.name = name;
        if (name == null) {
            throw new IllegalArgumentException("start scan, name can not be null!");
        }
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (!hasFound.get()) {
            if (device != null && device.getName() != null && name.equalsIgnoreCase(device.getName().trim())) {
                hasFound.set(true);
                if (viseBluetooth != null) {
                    viseBluetooth.stopLeScan(PeriodNameScanCallback.this);
                    viseBluetooth.setState(State.SCAN_SUCCESS);
                }
                onDeviceFound(new BluetoothLeDevice(device, rssi, scanRecord, System.currentTimeMillis()));
            }
        }
    }
}
