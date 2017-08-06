package com.vise.baseble.callback;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Looper;

import com.vise.baseble.ViseBle;
import com.vise.baseble.common.BleConstant;
import com.vise.baseble.model.BluetoothLeDevice;

/**
 * @Description: 扫描设备回调
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 17/8/1 22:58.
 */
public abstract class ScanCallback implements BluetoothAdapter.LeScanCallback {
    protected Handler handler = new Handler(Looper.getMainLooper());
    protected int scanTimeout = BleConstant.TIME_FOREVER; //表示一直扫描
    protected boolean isScan = true;
    protected boolean isScanning = false;

    public ScanCallback setScanTimeout(int scanTimeout) {
        this.scanTimeout = scanTimeout;
        return this;
    }

    public ScanCallback setScan(boolean scan) {
        isScan = scan;
        return this;
    }

    public boolean isScanning() {
        return isScanning;
    }

    public int getScanTimeout() {
        return scanTimeout;
    }

    public void scan() {
        if (isScan) {
            if (isScanning) {
                return;
            }
            if (scanTimeout > 0) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        isScanning = false;
                        ViseBle.getInstance().stopLeScan(ScanCallback.this);
                        scanTimeout();
                    }
                }, scanTimeout);
            }
            isScanning = true;
            ViseBle.getInstance().startLeScan(ScanCallback.this);
        } else {
            isScanning = false;
            ViseBle.getInstance().stopLeScan(ScanCallback.this);
        }
    }

    public ScanCallback removeHandlerMsg() {
        handler.removeCallbacksAndMessages(null);
        return this;
    }

    @Override
    public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {
        onDeviceFound(new BluetoothLeDevice(bluetoothDevice, rssi, scanRecord, System.currentTimeMillis()));
    }

    public abstract void scanTimeout();

    public abstract void onDeviceFound(BluetoothLeDevice bluetoothLeDevice);
}
