package com.vise.baseble.callback;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.vise.baseble.ViseBle;
import com.vise.baseble.common.BleConstant;
import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.baseble.model.BluetoothLeDeviceStore;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Description: 扫描设备回调
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 17/8/1 22:58.
 */
public abstract class ScanCallback implements BluetoothAdapter.LeScanCallback {
    private Handler handler = new Handler(Looper.getMainLooper());
    private AtomicBoolean hasFound = new AtomicBoolean(false);
    private BluetoothLeDeviceStore bluetoothLeDeviceStore;//用来存储扫描到的设备
    private Pattern pattern;
    private Matcher matcher;

    private int scanTimeout = BleConstant.TIME_FOREVER; //表示一直扫描
    private String filterDeviceName;//过滤设备的名称
    private int filterDeviceRssi;//过滤设备的信号
    private boolean isScan = true;//是否开始扫描
    private boolean isScanning = false;//是否正在扫描
    private String deviceName;//扫描指定名称的设备
    private String deviceMac;//扫描指定Mac地址的设备

    public ScanCallback() {
        bluetoothLeDeviceStore = new BluetoothLeDeviceStore();
        pattern = Pattern.compile("^[\\x00-\\xff]*$");
    }

    public ScanCallback setScanTimeout(int scanTimeout) {
        this.scanTimeout = scanTimeout;
        return this;
    }

    public ScanCallback setScan(boolean scan) {
        isScan = scan;
        return this;
    }

    public ScanCallback setFilterDeviceName(String filterDeviceName) {
        this.filterDeviceName = filterDeviceName;
        if (!TextUtils.isEmpty(this.filterDeviceName)) {
            pattern = Pattern.compile(this.filterDeviceName);
        }
        bluetoothLeDeviceStore.clear();
        return this;
    }

    public ScanCallback setFilterDeviceRssi(int filterDeviceRssi) {
        this.filterDeviceRssi = filterDeviceRssi;
        bluetoothLeDeviceStore.clear();
        return this;
    }

    public ScanCallback setDeviceName(String deviceName) {
        this.deviceName = deviceName;
        bluetoothLeDeviceStore.clear();
        return this;
    }

    public ScanCallback setDeviceMac(String deviceMac) {
        this.deviceMac = deviceMac;
        bluetoothLeDeviceStore.clear();
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
        if (TextUtils.isEmpty(deviceName) && TextUtils.isEmpty(deviceMac)) {
            onDeviceFound(new BluetoothLeDevice(bluetoothDevice, rssi, scanRecord, System.currentTimeMillis()));
        } else {
            if (!hasFound.get()) {
                if (bluetoothDevice != null && bluetoothDevice.getAddress() != null && deviceMac != null
                        && deviceMac.equalsIgnoreCase(bluetoothDevice.getAddress().trim())) {
                    hasFound.set(true);
                    ViseBle.getInstance().stopLeScan(ScanCallback.this);
                    onDeviceFound(new BluetoothLeDevice(bluetoothDevice, rssi, scanRecord, System.currentTimeMillis()));
                } else if (bluetoothDevice != null && bluetoothDevice.getName() != null && deviceName != null
                        && deviceName.equalsIgnoreCase(bluetoothDevice.getName().trim())) {
                    hasFound.set(true);
                    ViseBle.getInstance().stopLeScan(ScanCallback.this);
                    onDeviceFound(new BluetoothLeDevice(bluetoothDevice, rssi, scanRecord, System.currentTimeMillis()));
                }
            }
        }
    }

    protected void onDeviceFound(BluetoothLeDevice bluetoothLeDevice) {
        String tempName = bluetoothLeDevice.getName();
        int tempRssi = bluetoothLeDevice.getRssi();
        if (!TextUtils.isEmpty(tempName)) {
            addDevice(bluetoothLeDevice, tempName, tempRssi);
        } else {
            addDevice(bluetoothLeDevice, "", tempRssi);
        }
        onDeviceFound(bluetoothLeDeviceStore);
    }

    private void addDevice(BluetoothLeDevice bluetoothLeDevice, String tempName, int tempRssi) {
        matcher = pattern.matcher(tempName);
        if (this.filterDeviceRssi < 0) {
            if (matcher.matches() && tempRssi >= this.filterDeviceRssi) {
                bluetoothLeDeviceStore.addDevice(bluetoothLeDevice);
            } else if (matcher.matches() && tempRssi < this.filterDeviceRssi) {
                bluetoothLeDeviceStore.removeDevice(bluetoothLeDevice);
            }
        } else {
            if (matcher.matches()) {
                bluetoothLeDeviceStore.addDevice(bluetoothLeDevice);
            }
        }
    }

    public abstract void onDeviceFound(BluetoothLeDeviceStore bluetoothLeDeviceStore);

    public abstract void scanTimeout();

}
