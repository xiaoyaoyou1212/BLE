package com.vise.baseble.callback;

import com.vise.baseble.exception.BleException;

/**
 * @Description: 操作数据回调
 * @author: <a href="http://xiaoyaoyou1212.360doc.com">DAWI</a>
 * @date: 2017/10/17 19:42
 */
public interface IBleCallback<T> {
    void onSuccess(T data, int code, byte[] bytes);
    void onFailure(BleException exception);
}
