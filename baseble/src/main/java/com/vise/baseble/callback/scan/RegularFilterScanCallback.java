package com.vise.baseble.callback.scan;

import android.text.TextUtils;

import com.vise.baseble.model.BluetoothLeDevice;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Description: 根据正则过滤扫描设备，这里设置的是根据一定信号范围内指定正则设备名称的过滤
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 17/9/12 22:19.
 */
public class RegularFilterScanCallback extends ScanCallback {
    private Pattern pattern;
    private Matcher matcher;
    private String regularDeviceName;//正则表达式表示的设备名称
    private int deviceRssi;//设备的信号

    public RegularFilterScanCallback(IScanCallback scanCallback) {
        super(scanCallback);
        pattern = Pattern.compile("^[\\x00-\\xff]*$");
    }

    public RegularFilterScanCallback setRegularDeviceName(String regularDeviceName) {
        this.regularDeviceName = regularDeviceName;
        if (!TextUtils.isEmpty(this.regularDeviceName)) {
            pattern = Pattern.compile(this.regularDeviceName);
        }
        return this;
    }

    public RegularFilterScanCallback setDeviceRssi(int deviceRssi) {
        this.deviceRssi = deviceRssi;
        return this;
    }

    @Override
    public BluetoothLeDevice onFilter(BluetoothLeDevice bluetoothLeDevice) {
        BluetoothLeDevice tempDevice = null;
        String tempName = bluetoothLeDevice.getName();
        int tempRssi = bluetoothLeDevice.getRssi();
        if (!TextUtils.isEmpty(tempName)) {
            matcher = pattern.matcher(tempName);
            if (this.deviceRssi < 0) {
                if (matcher.matches() && tempRssi >= this.deviceRssi) {
                    tempDevice = bluetoothLeDevice;
                }
            } else {
                if (matcher.matches()) {
                    tempDevice = bluetoothLeDevice;
                }
            }
        }
        return tempDevice;
    }
}
