package com.vise.baseble.callback.scan;

import com.vise.baseble.model.BluetoothLeDevice;

import java.util.List;

/**
 * @Description: 指定设备集合进行过滤，一般用设备名称和Mac地址集合
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 17/9/12 22:50.
 */
public class ListFilterScanCallback extends ScanCallback {
    private List<String> deviceNameList;//指定设备名称集合
    private List<String> deviceMacList;//指定设备Mac地址集合

    public ListFilterScanCallback(IScanCallback scanCallback) {
        super(scanCallback);
    }

    public ListFilterScanCallback setDeviceNameList(List<String> deviceNameList) {
        this.deviceNameList = deviceNameList;
        return this;
    }

    public ListFilterScanCallback setDeviceMacList(List<String> deviceMacList) {
        this.deviceMacList = deviceMacList;
        return this;
    }

    @Override
    public BluetoothLeDevice onFilter(BluetoothLeDevice bluetoothLeDevice) {
        BluetoothLeDevice tempDevice = null;
        if (deviceNameList != null && deviceNameList.size() > 0) {
            for (String deviceName : deviceNameList) {
                if (bluetoothLeDevice != null && bluetoothLeDevice.getName() != null && deviceName != null
                        && deviceName.equalsIgnoreCase(bluetoothLeDevice.getName().trim())) {
                    tempDevice = bluetoothLeDevice;
                }
            }
        } else if (deviceMacList != null && deviceMacList.size() > 0) {
            for (String deviceMac : deviceMacList) {
                if (bluetoothLeDevice != null && bluetoothLeDevice.getAddress() != null && deviceMac != null
                        && deviceMac.equalsIgnoreCase(bluetoothLeDevice.getAddress().trim())) {
                    tempDevice = bluetoothLeDevice;
                }
            }
        }
        return tempDevice;
    }
}
