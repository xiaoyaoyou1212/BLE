package com.vise.bledemo.common;

import android.content.Context;
import android.widget.Toast;

import com.vise.baseble.ViseBle;
import com.vise.baseble.callback.IBleCallback;
import com.vise.baseble.callback.IConnectCallback;
import com.vise.baseble.callback.scan.IScanCallback;
import com.vise.baseble.callback.scan.ScanCallback;
import com.vise.baseble.common.BleConstant;
import com.vise.baseble.core.BluetoothGattChannel;
import com.vise.baseble.core.DeviceMirror;
import com.vise.baseble.core.DeviceMirrorPool;
import com.vise.baseble.exception.BleException;
import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.baseble.model.BluetoothLeDeviceStore;
import com.vise.baseble.utils.HexUtil;
import com.vise.bledemo.activity.DeviceControlActivity;
import com.vise.log.ViseLog;

/**
 * @Description: 蓝牙设备管理
 * @author: <a href="http://xiaoyaoyou1212.360doc.com">DAWI</a>
 * @date: 2017/10/27 17:09
 */
public class BluetoothDeviceManager {

    private static BluetoothDeviceManager instance;
    private DeviceMirrorPool mDeviceMirrorPool;
    private BluetoothLeDeviceStore mBluetoothLeDeviceStore;

    /**
     * 扫描回调
     */
    private ScanCallback periodScanCallback = new ScanCallback(new IScanCallback() {
        @Override
        public void onDeviceFound(BluetoothLeDeviceStore bluetoothLeDeviceStore) {
            ViseLog.i("Founded Scan Device:" + bluetoothLeDeviceStore);
            mBluetoothLeDeviceStore = bluetoothLeDeviceStore;
        }

        @Override
        public void onScanFinish(BluetoothLeDeviceStore bluetoothLeDeviceStore) {
            ViseLog.i("scan finish " + bluetoothLeDeviceStore);
        }

        @Override
        public void onScanTimeout() {
            ViseLog.i("scan timeout");
        }
    });

    /**
     * 连接回调
     */
    private IConnectCallback connectCallback = new IConnectCallback() {

        @Override
        public void onConnectSuccess(final DeviceMirror deviceMirror) {
            ViseLog.i("Connect Success!");
        }

        @Override
        public void onConnectFailure(BleException exception) {
            ViseLog.i("Connect Failure!");
        }

        @Override
        public void onDisconnect(boolean isActive) {
            ViseLog.i("Disconnect!");
        }
    };

    /**
     * 接收数据回调
     */
    private IBleCallback receiveCallback = new IBleCallback() {
        @Override
        public void onSuccess(final byte[] data, BluetoothGattChannel bluetoothGattInfo) {
            if (data == null) {
                return;
            }
            ViseLog.i("notify success:" + HexUtil.encodeHexStr(data));
        }

        @Override
        public void onFailure(BleException exception) {
            if (exception == null) {
                return;
            }
            ViseLog.i("notify fail:" + exception.getDescription());
        }
    };

    /**
     * 写入数据回调
     */
    private IBleCallback writeCallback = new IBleCallback() {
        @Override
        public void onSuccess(final byte[] data, BluetoothGattChannel bluetoothGattInfo) {
            if (data == null) {
                return;
            }
            ViseLog.i("write success:" + HexUtil.encodeHexStr(data));
        }

        @Override
        public void onFailure(BleException exception) {
            if (exception == null) {
                return;
            }
            ViseLog.i("write fail:" + exception.getDescription());
        }
    };

    /**
     * 读取数据回调
     */
    private IBleCallback readCallback = new IBleCallback() {
        @Override
        public void onSuccess(final byte[] data, BluetoothGattChannel bluetoothGattInfo) {
            if (data == null) {
                return;
            }
            ViseLog.i("read success:" + HexUtil.encodeHexStr(data));
        }

        @Override
        public void onFailure(BleException exception) {
            if (exception == null) {
                return;
            }
            ViseLog.i("read fail:" + exception.getDescription());
        }
    };

    /**
     * 使能回调
     */
    private IBleCallback enableCallback = new IBleCallback() {
        @Override
        public void onSuccess(final byte[] data, BluetoothGattChannel bluetoothGattInfo) {
            if (data == null) {
                return;
            }
            ViseLog.i("enable success:" + HexUtil.encodeHexStr(data));
        }

        @Override
        public void onFailure(BleException exception) {
            if (exception == null) {
                return;
            }
            ViseLog.i("enable fail:" + exception.getDescription());
        }
    };

    private BluetoothDeviceManager() {

    }

    public static BluetoothDeviceManager getInstance() {
        if (instance == null) {
            synchronized (BluetoothDeviceManager.class) {
                if (instance == null) {
                    instance = new BluetoothDeviceManager();
                }
            }
        }
        return instance;
    }

    public void init(Context context) {
        if (context == null) {
            return;
        }
        //蓝牙相关配置修改
        ViseBle.config()
                .setScanTimeout(BleConstant.TIME_FOREVER)
                .setMaxConnectCount(3);
        //蓝牙信息初始化，全局唯一，必须在应用初始化时调用
        ViseBle.getInstance().init(context.getApplicationContext());
        mDeviceMirrorPool = ViseBle.getInstance().getDeviceMirrorPool();
    }

    public void scan() {

    }

    public void connect(BluetoothLeDevice bluetoothLeDevice) {

    }

    public void write(BluetoothLeDevice bluetoothLeDevice, byte[] data) {

    }

    public void read(BluetoothLeDevice bluetoothLeDevice) {

    }

    public void notify(BluetoothLeDevice bluetoothLeDevice) {

    }

}
