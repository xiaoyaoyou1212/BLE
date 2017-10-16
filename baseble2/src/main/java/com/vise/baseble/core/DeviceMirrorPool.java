package com.vise.baseble.core;

import com.vise.baseble.common.BleConfig;
import com.vise.baseble.model.BluetoothLeDevice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @Description: 设备镜像池，用来管理多个设备连接后的操作
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 17/8/1 23:18.
 */
public class DeviceMirrorPool {
    private final LruHashMap<String, DeviceMirror> DEVICE_MIRROR_MAP;

    public DeviceMirrorPool() {
        DEVICE_MIRROR_MAP = new LruHashMap<>(BleConfig.getInstance().getMaxConnectCount());
    }

    public DeviceMirrorPool(int deviceMirrorSize) {
        DEVICE_MIRROR_MAP = new LruHashMap<>(deviceMirrorSize);
    }

    public void addDeviceMirror(DeviceMirror deviceMirror) {
        if (deviceMirror == null) {
            return;
        }
        if (!DEVICE_MIRROR_MAP.containsKey(deviceMirror.getUniqueSymbol())) {
            DEVICE_MIRROR_MAP.put(deviceMirror.getUniqueSymbol(), deviceMirror);
        }
    }

    public void removeDeviceMirror(DeviceMirror deviceMirror) {
        if (deviceMirror == null) {
            return;
        }
        if (DEVICE_MIRROR_MAP.containsKey(deviceMirror.getUniqueSymbol())) {
            DEVICE_MIRROR_MAP.remove(deviceMirror.getUniqueSymbol());
        }
    }

    public boolean isContainDevice(DeviceMirror deviceMirror) {
        if (deviceMirror != null && DEVICE_MIRROR_MAP.containsKey(deviceMirror.getUniqueSymbol())) {
            return true;
        }
        return false;
    }

    public boolean isContainDevice(BluetoothLeDevice bluetoothLeDevice) {
        if (bluetoothLeDevice != null && DEVICE_MIRROR_MAP.containsKey(bluetoothLeDevice.getAddress() + bluetoothLeDevice.getName())) {
            return true;
        }
        return false;
    }

    public void clear() {
        DEVICE_MIRROR_MAP.clear();
    }

    public Map<String, DeviceMirror> getDeviceMirrorMap() {
        return DEVICE_MIRROR_MAP;
    }

    public List<DeviceMirror> getDeviceMirrorList() {
        final List<DeviceMirror> deviceMirrors = new ArrayList<>(DEVICE_MIRROR_MAP.values());
        Collections.sort(deviceMirrors, new Comparator<DeviceMirror>() {
            @Override
            public int compare(final DeviceMirror lhs, final DeviceMirror rhs) {
                return lhs.getUniqueSymbol().compareToIgnoreCase(rhs.getUniqueSymbol());
            }
        });
        return deviceMirrors;
    }

}
