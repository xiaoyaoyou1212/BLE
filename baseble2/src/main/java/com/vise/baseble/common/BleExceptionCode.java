package com.vise.baseble.common;

/**
 * @Description: BLE异常Code
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/8/14 10:43.
 */
public enum BleExceptionCode {
    TIMEOUT,    //超时
    CONNECT_ERR,    //连接异常
    GATT_ERR,   //GATT异常
    INITIATED_ERR,  //初始化异常
    OTHER_ERR   //其他异常
}
