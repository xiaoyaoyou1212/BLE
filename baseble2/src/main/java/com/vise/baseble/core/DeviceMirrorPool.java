package com.vise.baseble.core;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description: 设备镜像池，用来管理多个设备连接后的操作
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 17/8/1 23:18.
 */
public class DeviceMirrorPool {
    private final Map<String, DeviceMirror> mDeviceMirrorMap;

    public DeviceMirrorPool() {
        mDeviceMirrorMap = new HashMap<>();
    }

    public void addDevice(DeviceMirror deviceMirror) {
        if (deviceMirror == null) {
            return;
        }
    }

    public void removeDevice(DeviceMirror deviceMirror) {
        if (deviceMirror == null) {
            return;
        }
    }

    public void clear() {
        mDeviceMirrorMap.clear();
    }

    public Map<String, DeviceMirror> getDeviceMirrorMap() {
        return mDeviceMirrorMap;
    }

}
