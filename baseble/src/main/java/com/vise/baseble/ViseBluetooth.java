package com.vise.baseble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.vise.baseble.callback.scan.PeriodScanCallback;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/8/5 20:42.
 */
public class ViseBluetooth {

    public static final int DEFAULT_SCAN_TIME = 20000;
    public static final int DEFAULT_CONN_TIME = 10000;

    private Context context;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Set<BluetoothGattCallback> callbackList = new LinkedHashSet<BluetoothGattCallback>();
    private int scanTimeout = DEFAULT_SCAN_TIME;
    private int connectTimeout = DEFAULT_CONN_TIME;

    public ViseBluetooth(Context context) {
        this.context = context;
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
    }

    /*==================scan========================*/
    public void startLeScan(BluetoothAdapter.LeScanCallback leScanCallback){
        bluetoothAdapter.startLeScan(leScanCallback);
    }

    public void stopLeScan(BluetoothAdapter.LeScanCallback leScanCallback){
        bluetoothAdapter.stopLeScan(leScanCallback);
    }

    public void startLeScan(PeriodScanCallback periodScanCallback){
        if (periodScanCallback == null) {
            throw new NullPointerException("this PeriodScanCallback is Null!");
        }
        periodScanCallback.setViseBluetooth(this).setScan(true).setScanTimeout(scanTimeout).scan();
    }

    public void stopLeScan(PeriodScanCallback periodScanCallback){
        if (periodScanCallback == null) {
            throw new NullPointerException("this PeriodScanCallback is Null!");
        }
        periodScanCallback.setViseBluetooth(this).removeHandlerMsg().setScan(false).scan();
    }

    /*==================get and set========================*/
    public BluetoothManager getBluetoothManager() {
        return bluetoothManager;
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }

    public Set<BluetoothGattCallback> getCallbackList() {
        return callbackList;
    }

    public void setScanTimeout(int scanTimeout) {
        this.scanTimeout = scanTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getScanTimeout() {
        return scanTimeout;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }
}
