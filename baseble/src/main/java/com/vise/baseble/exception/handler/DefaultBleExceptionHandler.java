package com.vise.baseble.exception.handler;

import com.vise.baseble.exception.ConnectException;
import com.vise.baseble.exception.GattException;
import com.vise.baseble.exception.InitiatedException;
import com.vise.baseble.exception.OtherException;
import com.vise.baseble.exception.TimeoutException;
import com.vise.baseble.utils.BleLog;

/**
 * @Description: 异常默认处理
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/8/14 10:35.
 */
public class DefaultBleExceptionHandler extends BleExceptionHandler {
    @Override
    protected void onConnectException(ConnectException e) {
        BleLog.e(e.getDescription());
    }

    @Override
    protected void onGattException(GattException e) {
        BleLog.e(e.getDescription());
    }

    @Override
    protected void onTimeoutException(TimeoutException e) {
        BleLog.e(e.getDescription());
    }

    @Override
    protected void onInitiatedException(InitiatedException e) {
        BleLog.e(e.getDescription());
    }

    @Override
    protected void onOtherException(OtherException e) {
        BleLog.e(e.getDescription());
    }
}
