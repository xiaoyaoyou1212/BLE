package com.vise.baseble.common;

/**
 * @Description: BLE常量
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/8/20 20:31.
 */
public class BleConstant {
    public static final String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    public static final int TIME_FOREVER = -1;

    public static final int DEFAULT_SCAN_TIME = 20000;
    public static final int DEFAULT_CONN_TIME = 10000;
    public static final int DEFAULT_OPERATE_TIME = 5000;

    public static final int MSG_WRITE_CHA = 1;
    public static final int MSG_WRITE_DES = 2;
    public static final int MSG_READ_CHA = 3;
    public static final int MSG_READ_DES = 4;
    public static final int MSG_READ_RSSI = 5;
    public static final int MSG_CONNECT_TIMEOUT = 6;
}
