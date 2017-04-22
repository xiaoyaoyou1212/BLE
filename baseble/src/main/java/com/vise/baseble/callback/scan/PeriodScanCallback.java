package com.vise.baseble.callback.scan;

import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Looper;

import com.vise.baseble.ViseBluetooth;
import com.vise.baseble.common.BleConstant;
import com.vise.baseble.common.State;
import com.vise.baseble.model.BluetoothLeDevice;

/**
 * @Description: 扫描回调
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/8/8 21:47.
 */
public abstract class PeriodScanCallback implements LeScanCallback {

    protected Handler handler = new Handler(Looper.getMainLooper());
    protected ViseBluetooth viseBluetooth;
    protected int scanTimeout = BleConstant.TIME_FOREVER; //表示一直扫描
    protected boolean isScan = true;
    protected boolean isScanning = false;

    public ViseBluetooth getViseBluetooth() {
        return viseBluetooth;
    }

    public PeriodScanCallback setViseBluetooth(ViseBluetooth viseBluetooth) {
        this.viseBluetooth = viseBluetooth;
        return this;
    }

    public PeriodScanCallback setScanTimeout(int scanTimeout) {
        this.scanTimeout = scanTimeout;
        return this;
    }

    public PeriodScanCallback setScan(boolean scan) {
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
                        if (viseBluetooth != null) {
                            viseBluetooth.setState(State.SCAN_TIMEOUT);
                            viseBluetooth.stopLeScan(PeriodScanCallback.this);
                        }
                        scanTimeout();
                    }
                }, scanTimeout);
            }
            isScanning = true;
            if (viseBluetooth != null) {
                viseBluetooth.startLeScan(PeriodScanCallback.this);
            }
        } else {
            isScanning = false;
            if (viseBluetooth != null) {
                viseBluetooth.stopLeScan(PeriodScanCallback.this);
            }
        }
    }

    public PeriodScanCallback removeHandlerMsg() {
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
