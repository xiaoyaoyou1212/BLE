package com.vise.baseble.callback;

import com.vise.baseble.exception.BleException;

/**
 * @Description: BLE操作回调
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/8/5 20:45.
 */
public interface IBleCallback<T> {
    void onSuccess(T t, int type);
    void onFailure(BleException exception);
}
