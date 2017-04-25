package com.vise.baseble.callback.scan;

import android.text.TextUtils;

import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.baseble.model.BluetoothLeDeviceStore;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Description: 根据特定条件过滤设备（设备名字根据正则表达式过滤）
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 2017-04-25 15:30
 */
public abstract class PeriodFilterScanCallback extends PeriodScanCallback {
    protected BluetoothLeDeviceStore bluetoothLeDeviceStore;
    protected String filterName;
    protected int filterRssi;
    protected Pattern pattern;
    protected Matcher matcher;

    public PeriodFilterScanCallback() {
        bluetoothLeDeviceStore = new BluetoothLeDeviceStore();
        pattern = Pattern.compile("^[\\x00-\\xff]*$");
    }

    public PeriodFilterScanCallback setFilterName(String filterName) {
        this.filterName = filterName;
        if (!TextUtils.isEmpty(this.filterName)) {
            pattern = Pattern.compile(this.filterName);
        }
        bluetoothLeDeviceStore.clear();
        return this;
    }

    public PeriodFilterScanCallback setFilterRssi(int filterRssi) {
        this.filterRssi = filterRssi;
        bluetoothLeDeviceStore.clear();
        return this;
    }

    @Override
    public void onDeviceFound(BluetoothLeDevice bluetoothLeDevice) {
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
        if (this.filterRssi < 0) {
            if (matcher.matches() && tempRssi >= this.filterRssi) {
                bluetoothLeDeviceStore.addDevice(bluetoothLeDevice);
            } else if (matcher.matches() && tempRssi < this.filterRssi) {
                bluetoothLeDeviceStore.removeDevice(bluetoothLeDevice);
            }
        } else {
            if (matcher.matches()) {
                bluetoothLeDeviceStore.addDevice(bluetoothLeDevice);
            }
        }
    }

    public abstract void onDeviceFound(BluetoothLeDeviceStore bluetoothLeDeviceStore);
}
