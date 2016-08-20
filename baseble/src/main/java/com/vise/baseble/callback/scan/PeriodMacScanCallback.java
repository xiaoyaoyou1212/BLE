package com.vise.baseble.callback.scan;

import android.bluetooth.BluetoothDevice;

import com.vise.baseble.common.State;
import com.vise.baseble.model.BluetoothLeDevice;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Description: 扫描指定Mac设备回调
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/8/10 22:39.
 */
public abstract class PeriodMacScanCallback extends PeriodScanCallback {
    private String mac;
    private AtomicBoolean hasFound = new AtomicBoolean(false);

    public PeriodMacScanCallback(String mac) {
        super();
        this.mac = mac;
        if (mac == null) {
            throw new IllegalArgumentException("start scan, mac can not be null!");
        }
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (!hasFound.get()) {
            if (device != null && device.getAddress() != null && mac.equalsIgnoreCase(device.getAddress().trim())) {
                hasFound.set(true);
                if (viseBluetooth != null) {
                    viseBluetooth.stopLeScan(PeriodMacScanCallback.this);
                    viseBluetooth.setState(State.SCAN_SUCCESS);
                }
                onDeviceFound(new BluetoothLeDevice(device, rssi, scanRecord, System.currentTimeMillis()));
            }
        }
    }
}
