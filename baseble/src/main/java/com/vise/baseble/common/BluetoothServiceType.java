package com.vise.baseble.common;

import android.bluetooth.BluetoothClass;

/**
 * @Description: BLE服务类型
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/8/7 22:07.
 */
public enum BluetoothServiceType {
    AUDIO(BluetoothClass.Service.AUDIO),    //音频服务
    CAPTURE(BluetoothClass.Service.CAPTURE),    //捕捉服务
    INFORMATION(BluetoothClass.Service.INFORMATION),    //信息服务
    LIMITED_DISCOVERABILITY(BluetoothClass.Service.LIMITED_DISCOVERABILITY),    //有限发现服务
    NETWORKING(BluetoothClass.Service.NETWORKING),  //网络服务
    OBJECT_TRANSFER(BluetoothClass.Service.OBJECT_TRANSFER),    //对象传输服务
    POSITIONING(BluetoothClass.Service.POSITIONING),    //定位服务
    RENDER(BluetoothClass.Service.RENDER),  //给予服务
    TELEPHONY(BluetoothClass.Service.TELEPHONY);    //电话服务

    private int code;

    BluetoothServiceType(int code) {
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }
}
