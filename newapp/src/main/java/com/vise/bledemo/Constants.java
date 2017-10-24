package com.vise.bledemo;

/**
 * @Description:
 * @author: <a href="http://xiaoyaoyou1212.360doc.com">DAWI</a>
 * @date: 2017/10/21 14:39
 */
public class Constants {
    public static class UUID{
        /*Serial data Service*/
        public static final String SERIAL_DATA_SERVICE_UUID = "0000ff12-0000-1000-8000-00805f9b34fb";
        public static final String WRITE_DATA_CHARACTERISTIC_UUID = "0000ff01-0000-1000-8000-00805f9b34fb";
        public static final String NOTIFY_DATA_CHARACTERISTIC_UUID = "0000ff02-0000-1000-8000-00805f9b34fb";
        public static final String INDICATE_DATA_CHARACTERISTIC_UUID = "0000ff03-0000-1000-8000-00805f9b34fb";
        /*Battery Service*/
        public static final String BATTERY_SERVICE_UUID = "0000180f-0000-1000-8000-00805f9b34fb";
        public static final String BATTERY_LEVEL_CHARACTERISTIC_UUID = "00002a19-0000-1000-8000-00805f9b34fb";//当前设备电量信息

        public static final String BLE_SERVICE_UUID = "0000fff0-0000-1000-8000-00805f9b34fb";
        public static final String BLE_NOTIFY_UUID = "0000fff7-0000-1000-8000-00805f9b34fb";
        public static final String BLE_WRITE_UUID = "0000fff6-0000-1000-8000-00805f9b34fb";
    }
}
