package com.vise.baseble.core;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.util.UUID;

/**
 * @Description: BluetoothGatt 相关信息
 * @author: <a href="http://xiaoyaoyou1212.360doc.com">DAWI</a>
 * @date: 2017/10/17 16:25
 */
public class BluetoothGattInfo {
    private BluetoothGattService service;
    private BluetoothGattCharacteristic characteristic;
    private BluetoothGattDescriptor descriptor;

    public BluetoothGattInfo(BluetoothGatt bluetoothGatt, UUID serviceUUID, UUID characteristicUUID, UUID descriptorUUID) {
        if (serviceUUID != null && bluetoothGatt != null) {
            service = bluetoothGatt.getService(serviceUUID);
        }
        if (service != null && characteristicUUID != null) {
            characteristic = service.getCharacteristic(characteristicUUID);
        }
        if (characteristic != null && descriptorUUID != null) {
            descriptor = characteristic.getDescriptor(descriptorUUID);
        }
    }

    public BluetoothGattCharacteristic getCharacteristic() {
        return characteristic;
    }

    public BluetoothGattDescriptor getDescriptor() {
        return descriptor;
    }

    public BluetoothGattService getService() {
        return service;
    }
}
