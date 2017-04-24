package com.vise.baseble.callback.data;

/**
 * @Description: 信号值操作回调
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 2017-04-24 11:11
 */
public interface IRssiCallback extends IBleCallback {
    void onSuccess(int rssi);
}
