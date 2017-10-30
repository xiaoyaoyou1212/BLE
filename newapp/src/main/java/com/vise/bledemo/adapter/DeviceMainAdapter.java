package com.vise.bledemo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.baseble.utils.HexUtil;
import com.vise.bledemo.R;

import java.util.List;

public class DeviceMainAdapter extends BaseAdapter {

    private Context context;
    private List<BluetoothLeDevice> deviceList;
    private BluetoothLeDevice bluetoothLeDevice;
    private byte[] notifyData;

    public DeviceMainAdapter(Context context) {
        this.context = context;
    }

    public DeviceMainAdapter setDeviceList(List<BluetoothLeDevice> deviceList) {
        this.deviceList = deviceList;
        notifyDataSetChanged();
        return this;
    }

    public DeviceMainAdapter setNotifyData(BluetoothLeDevice bluetoothLeDevice, byte[] notifyData) {
        this.bluetoothLeDevice = bluetoothLeDevice;
        this.notifyData = notifyData;
        notifyDataSetChanged();
        return this;
    }

    @Override
    public int getCount() {
        return deviceList != null ? deviceList.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return deviceList != null ? deviceList.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(context).inflate(R.layout.item_main_layout, null);
            viewHolder.deviceName = (TextView) convertView.findViewById(R.id.device_name);
            viewHolder.deviceMac = (TextView) convertView.findViewById(R.id.device_mac);
            viewHolder.deviceNotifyData = (TextView) convertView.findViewById(R.id.device_notify_data);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        if (deviceList != null && deviceList.get(position) != null && deviceList.get(position).getDevice() != null) {
            String deviceName = deviceList.get(position).getDevice().getName();
            if (deviceName != null && !deviceName.isEmpty()) {
                viewHolder.deviceName.setText(deviceName);
            } else {
                viewHolder.deviceName.setText(context.getString(R.string.unknown_device));
            }
            viewHolder.deviceMac.setText(deviceList.get(position).getDevice().getAddress());
            if (bluetoothLeDevice != null && notifyData != null && notifyData.length > 0 && deviceList.get(position) != null
                    && bluetoothLeDevice.getAddress() != null && bluetoothLeDevice.getAddress().equals(deviceList.get(position).getAddress())) {
                viewHolder.deviceNotifyData.setText(context.getString(R.string.label_notify_data) + HexUtil.encodeHexStr(notifyData));
            }
        }
        return convertView;
    }

    class ViewHolder {
        TextView deviceName;
        TextView deviceMac;
        TextView deviceNotifyData;
    }
}
