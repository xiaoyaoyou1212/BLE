package com.vise.baseble.callback;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/8/5 20:45.
 */
public interface IBleCallback<T> {
    void onSuccess(T t, int type);
    void onFail(String errMsg, int errCode);
}
