package com.vise.bledemo.event;

import com.vise.baseble.core.DeviceMirror;
import com.vise.xsnow.event.IEvent;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 17/10/29 23:07.
 */
public class ConnectEvent implements IEvent {
    private boolean isSuccess;
    private boolean isDisconnected;
    private DeviceMirror deviceMirror;

    public boolean isSuccess() {
        return isSuccess;
    }

    public ConnectEvent setSuccess(boolean success) {
        isSuccess = success;
        return this;
    }

    public boolean isDisconnected() {
        return isDisconnected;
    }

    public ConnectEvent setDisconnected(boolean disconnected) {
        isDisconnected = disconnected;
        return this;
    }

    public DeviceMirror getDeviceMirror() {
        return deviceMirror;
    }

    public ConnectEvent setDeviceMirror(DeviceMirror deviceMirror) {
        this.deviceMirror = deviceMirror;
        return this;
    }
}
