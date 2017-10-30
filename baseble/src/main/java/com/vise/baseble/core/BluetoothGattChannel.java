package com.vise.baseble.core;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import com.vise.baseble.common.PropertyType;

import java.util.UUID;

/**
 * @Description: BluetoothGatt 相关信息
 * @author: <a href="http://xiaoyaoyou1212.360doc.com">DAWI</a>
 * @date: 2017/10/17 16:25
 */
public class BluetoothGattChannel {

    private BluetoothGatt bluetoothGatt;
    private BluetoothGattService service;
    private BluetoothGattCharacteristic characteristic;
    private BluetoothGattDescriptor descriptor;
    private String gattInfoKey;
    private PropertyType propertyType;
    private UUID serviceUUID;
    private UUID characteristicUUID;
    private UUID descriptorUUID;

    private BluetoothGattChannel(BluetoothGatt bluetoothGatt, PropertyType propertyType, UUID serviceUUID, UUID characteristicUUID, UUID descriptorUUID) {
        this.bluetoothGatt = bluetoothGatt;
        this.propertyType = propertyType;
        this.serviceUUID = serviceUUID;
        this.characteristicUUID = characteristicUUID;
        this.descriptorUUID = descriptorUUID;
        StringBuilder stringBuilder = new StringBuilder();
        if (propertyType != null) {
            stringBuilder.append(propertyType.getPropertyValue());
        }
        if (serviceUUID != null && bluetoothGatt != null) {
            service = bluetoothGatt.getService(serviceUUID);
            stringBuilder.append(serviceUUID.toString());
        }
        if (service != null && characteristicUUID != null) {
            characteristic = service.getCharacteristic(characteristicUUID);
            stringBuilder.append(characteristicUUID.toString());
        }
        if (characteristic != null && descriptorUUID != null) {
            descriptor = characteristic.getDescriptor(descriptorUUID);
            stringBuilder.append(descriptorUUID.toString());
        }
        gattInfoKey = stringBuilder.toString();
    }

    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
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

    public BluetoothGattChannel setDescriptor(BluetoothGattDescriptor descriptor) {
        this.descriptor = descriptor;
        return this;
    }

    public String getGattInfoKey() {
        return gattInfoKey;
    }

    public UUID getCharacteristicUUID() {
        return characteristicUUID;
    }

    public UUID getDescriptorUUID() {
        return descriptorUUID;
    }

    public PropertyType getPropertyType() {
        return propertyType;
    }

    public UUID getServiceUUID() {
        return serviceUUID;
    }

    public static class Builder {
        private BluetoothGatt bluetoothGatt;
        private PropertyType propertyType;
        private UUID serviceUUID;
        private UUID characteristicUUID;
        private UUID descriptorUUID;

        public Builder() {
        }

        public Builder setBluetoothGatt(BluetoothGatt bluetoothGatt) {
            this.bluetoothGatt = bluetoothGatt;
            return this;
        }

        public Builder setCharacteristicUUID(UUID characteristicUUID) {
            this.characteristicUUID = characteristicUUID;
            return this;
        }

        public Builder setDescriptorUUID(UUID descriptorUUID) {
            this.descriptorUUID = descriptorUUID;
            return this;
        }

        public Builder setPropertyType(PropertyType propertyType) {
            this.propertyType = propertyType;
            return this;
        }

        public Builder setServiceUUID(UUID serviceUUID) {
            this.serviceUUID = serviceUUID;
            return this;
        }

        public BluetoothGattChannel builder() {
            return new BluetoothGattChannel(bluetoothGatt, propertyType, serviceUUID, characteristicUUID, descriptorUUID);
        }
    }
}
