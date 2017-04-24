package com.vise.baseble.callback.data;

import com.vise.baseble.exception.BleException;

/**
 * @Description: 数据操作回调基类
 * @author: <a href="http://xiaoyaoyou1212.360doc.com">DAWI</a>
 * @date: 2017-04-24 11:05
 */
public interface IBleCallback {
    void onFailure(BleException exception);
}
