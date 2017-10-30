package com.vise.baseble.callback.scan;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Looper;

import com.vise.baseble.ViseBle;
import com.vise.baseble.common.BleConstant;
import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.baseble.model.BluetoothLeDeviceStore;

/**
 * @Description: 扫描设备回调
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 17/8/1 22:58.
 */
public class ScanCallback implements BluetoothAdapter.LeScanCallback, IScanFilter {
    protected Handler handler = new Handler(Looper.getMainLooper());
    protected int scanTimeout = BleConstant.TIME_FOREVER; //表示一直扫描
    protected boolean isScan = true;//是否开始扫描
    protected boolean isScanning = false;//是否正在扫描
    protected BluetoothLeDeviceStore bluetoothLeDeviceStore;//用来存储扫描到的设备
    protected IScanCallback scanCallback;//扫描结果回调

    public ScanCallback(IScanCallback scanCallback) {
        this.scanCallback = scanCallback;
        if (scanCallback == null) {
            throw new NullPointerException("this scanCallback is null!");
        }
        bluetoothLeDeviceStore = new BluetoothLeDeviceStore();

    }

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
            bluetoothLeDeviceStore.clear();
            if (scanTimeout > 0) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        isScanning = false;
                        ViseBle.getInstance().stopLeScan(ScanCallback.this);

                        if (bluetoothLeDeviceStore.getDeviceMap() != null
                                && bluetoothLeDeviceStore.getDeviceMap().size() > 0) {
                            scanCallback.onScanFinish(bluetoothLeDeviceStore);
                        } else {
                            scanCallback.onScanTimeout();
                        }
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
        scanCallback.onDeviceFound(onFilter(new BluetoothLeDevice(bluetoothDevice, rssi, scanRecord, System.currentTimeMillis())));
    }

    @Override
    public BluetoothLeDeviceStore onFilter(BluetoothLeDevice bluetoothLeDevice) {
        bluetoothLeDeviceStore.addDevice(bluetoothLeDevice);
        return bluetoothLeDeviceStore;
    }
}
