package com.vise.baseble.callback.data;

import android.bluetooth.BluetoothGattDescriptor;

/**
 * @Description: 描述值操作回调
 * @author: <a href="http://xiaoyaoyou1212.360doc.com">DAWI</a>
 * @date: 2017-04-24 11:11
 */
public interface IDescriptorCallback extends IBleCallback {
    void onSuccess(BluetoothGattDescriptor descriptor);
}
