package com.vise.bledemo.event;

import com.vise.baseble.core.BluetoothGattChannel;
import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.xsnow.event.IEvent;

public class NotifyDataEvent implements IEvent {
    private byte[] data;
    private BluetoothLeDevice bluetoothLeDevice;
    private BluetoothGattChannel bluetoothGattChannel;

    public byte[] getData() {
        return data;
    }

    public NotifyDataEvent setData(byte[] data) {
        this.data = data;
        return this;
    }

    public BluetoothLeDevice getBluetoothLeDevice() {
        return bluetoothLeDevice;
    }

    public NotifyDataEvent setBluetoothLeDevice(BluetoothLeDevice bluetoothLeDevice) {
        this.bluetoothLeDevice = bluetoothLeDevice;
        return this;
    }

    public BluetoothGattChannel getBluetoothGattChannel() {
        return bluetoothGattChannel;
    }

    public NotifyDataEvent setBluetoothGattChannel(BluetoothGattChannel bluetoothGattChannel) {
        this.bluetoothGattChannel = bluetoothGattChannel;
        return this;
    }
}
