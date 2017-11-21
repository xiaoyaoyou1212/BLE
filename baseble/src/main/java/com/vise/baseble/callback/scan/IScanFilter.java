package com.vise.baseble.callback.scan;

import com.vise.baseble.model.BluetoothLeDevice;

/**
 * @Description: 扫描过滤接口，根据需要实现过滤规则
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 17/9/10 18:19.
 */
public interface IScanFilter {
    BluetoothLeDevice onFilter(BluetoothLeDevice bluetoothLeDevice);
}
