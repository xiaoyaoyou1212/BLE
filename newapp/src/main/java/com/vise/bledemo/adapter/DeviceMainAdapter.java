package com.vise.bledemo.adapter;

import android.content.Context;
import android.widget.TextView;

import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.baseble.utils.HexUtil;
import com.vise.bledemo.R;
import com.vise.xsnow.ui.adapter.helper.HelperAdapter;
import com.vise.xsnow.ui.adapter.helper.HelperViewHolder;

public class DeviceMainAdapter extends HelperAdapter<BluetoothLeDevice> {

    private BluetoothLeDevice mBluetoothLeDevice;
    private byte[] mNotifyData;

    public DeviceMainAdapter(Context context) {
        super(context, R.layout.item_main_layout);
    }

    public DeviceMainAdapter setNotifyData(BluetoothLeDevice bluetoothLeDevice, byte[] notifyData) {
        this.mBluetoothLeDevice = bluetoothLeDevice;
        this.mNotifyData = notifyData;
        notifyDataSetChanged();
        return this;
    }

    @Override
    public void HelpConvert(HelperViewHolder viewHolder, int position, BluetoothLeDevice bluetoothLeDevice) {
        TextView deviceNameTv = viewHolder.getView(R.id.device_name);
        TextView deviceMacTv = viewHolder.getView(R.id.device_mac);
        TextView deviceNotifyDataTv = viewHolder.getView(R.id.device_notify_data);
        if (bluetoothLeDevice != null && bluetoothLeDevice.getDevice() != null) {
            String deviceName = bluetoothLeDevice.getDevice().getName();
            if (deviceName != null && !deviceName.isEmpty()) {
                deviceNameTv.setText(deviceName);
            } else {
                deviceNameTv.setText(mContext.getString(R.string.unknown_device));
            }
            deviceMacTv.setText(bluetoothLeDevice.getDevice().getAddress());
            if (mBluetoothLeDevice != null && mNotifyData != null && mNotifyData.length > 0 && bluetoothLeDevice != null
                    && mBluetoothLeDevice.getAddress() != null && mBluetoothLeDevice.getAddress().equals(bluetoothLeDevice.getAddress())) {
                deviceNotifyDataTv.setText(mContext.getString(R.string.label_notify_data) + HexUtil.encodeHexStr(mNotifyData));
            }
        }
    }
}
