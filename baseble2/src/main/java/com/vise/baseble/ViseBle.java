package com.vise.baseble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.vise.baseble.callback.IBleCallback;
import com.vise.baseble.callback.IConnectCallback;
import com.vise.baseble.callback.IRssiCallback;
import com.vise.baseble.callback.scan.IScanCallback;
import com.vise.baseble.callback.scan.ScanCallback;
import com.vise.baseble.callback.scan.SingleFilterScanCallback;
import com.vise.baseble.common.BleConfig;
import com.vise.baseble.core.DeviceMirror;
import com.vise.baseble.core.DeviceMirrorPool;
import com.vise.baseble.exception.TimeoutException;
import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.baseble.model.BluetoothLeDeviceStore;
import com.vise.log.ViseLog;

/**
 * @Description: BLE设备操作入口
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 17/8/1 22:24.
 */
public class ViseBle {
    private Context context;//上下文
    private BluetoothManager bluetoothManager;//蓝牙管理
    private BluetoothAdapter bluetoothAdapter;//蓝牙适配器
    private DeviceMirrorPool deviceMirrorPool;//设备连接池

    private static ViseBle instance;//入口操作管理
    private static BleConfig bleConfig = BleConfig.getInstance();

    /**
     * 单例方式获取蓝牙通信入口
     *
     * @return 返回ViseBluetooth
     */
    public static ViseBle getInstance() {
        if (instance == null) {
            synchronized (ViseBle.class) {
                if (instance == null) {
                    instance = new ViseBle();
                }
            }
        }
        return instance;
    }

    private ViseBle() {
    }

    /**
     * 获取配置对象，可进行相关配置的修改
     * @return
     */
    public static BleConfig config() {
        return bleConfig;
    }

    /**
     * 初始化
     *
     * @param context 上下文
     */
    public void init(Context context) {
        if (this.context == null && context != null) {
            this.context = context.getApplicationContext();
            bluetoothManager = (BluetoothManager) this.context.getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
            deviceMirrorPool = new DeviceMirrorPool();
        }
    }

    /**
     * 开始扫描
     *
     * @param leScanCallback 回调
     */
    public void startLeScan(BluetoothAdapter.LeScanCallback leScanCallback) {
        if (bluetoothAdapter != null) {
            bluetoothAdapter.startLeScan(leScanCallback);
        }
    }

    /**
     * 停止扫描
     *
     * @param leScanCallback 回调
     */
    public void stopLeScan(BluetoothAdapter.LeScanCallback leScanCallback) {
        if (bluetoothAdapter != null) {
            bluetoothAdapter.stopLeScan(leScanCallback);
        }
    }

    /**
     * 开始扫描
     *
     * @param scanCallback 自定义回调
     */
    public void startScan(ScanCallback scanCallback) {
        if (scanCallback == null) {
            throw new IllegalArgumentException("this ScanCallback is Null!");
        }
        scanCallback.setScan(true).setScanTimeout(BleConfig.getInstance().getScanTimeout()).scan();
    }

    /**
     * 停止扫描
     *
     * @param scanCallback 自定义回调
     */
    public void stopScan(ScanCallback scanCallback) {
        if (scanCallback == null) {
            throw new IllegalArgumentException("this ScanCallback is Null!");
        }
        scanCallback.setScan(false).removeHandlerMsg().scan();
    }

    /**
     * 连接设备
     *
     * @param bluetoothLeDevice
     * @param connectCallback
     */
    public void connect(BluetoothLeDevice bluetoothLeDevice, IConnectCallback connectCallback) {
        if (bluetoothLeDevice == null || connectCallback == null) {
            ViseLog.e("This bluetoothLeDevice or connectCallback is null.");
            return;
        }
        if (!deviceMirrorPool.isContainDevice(bluetoothLeDevice)) {
            DeviceMirror deviceMirror = new DeviceMirror(bluetoothLeDevice);
            deviceMirror.connect(connectCallback);
        } else {
            ViseLog.i("This device is connected.");
        }
    }

    /**
     * 连接指定mac地址的设备
     *
     * @param mac             设备mac地址
     * @param connectCallback 连接回调
     */
    public void connectByMac(String mac, final IConnectCallback connectCallback) {
        if (mac == null || connectCallback == null) {
            ViseLog.e("This mac or connectCallback is null.");
            return;
        }
        startScan(new SingleFilterScanCallback(new IScanCallback() {
            @Override
            public void onDeviceFound(BluetoothLeDeviceStore bluetoothLeDeviceStore) {

            }

            @Override
            public void onScanFinish(final BluetoothLeDeviceStore bluetoothLeDeviceStore) {
                if (bluetoothLeDeviceStore.getDeviceList().size() > 0) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            connect(bluetoothLeDeviceStore.getDeviceList().get(0), connectCallback);
                        }
                    });
                } else {
                    connectCallback.onConnectFailure(new TimeoutException());
                }
            }

            @Override
            public void onScanTimeout() {
                connectCallback.onConnectFailure(new TimeoutException());
            }
        }).setDeviceMac(mac));
    }

    /**
     * 连接指定设备名称的设备
     *
     * @param name            设备名称
     * @param connectCallback 连接回调
     */
    public void connectByName(String name, final IConnectCallback connectCallback) {
        if (name == null || connectCallback == null) {
            ViseLog.e("This name or connectCallback is null.");
            return;
        }
        startScan(new SingleFilterScanCallback(new IScanCallback() {
            @Override
            public void onDeviceFound(BluetoothLeDeviceStore bluetoothLeDeviceStore) {

            }

            @Override
            public void onScanFinish(final BluetoothLeDeviceStore bluetoothLeDeviceStore) {
                if (bluetoothLeDeviceStore.getDeviceList().size() > 0) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            connect(bluetoothLeDeviceStore.getDeviceList().get(0), connectCallback);
                        }
                    });
                } else {
                    connectCallback.onConnectFailure(new TimeoutException());
                }
            }

            @Override
            public void onScanTimeout() {
                connectCallback.onConnectFailure(new TimeoutException());
            }
        }).setDeviceName(name));
    }

    /**
     * 写入数据
     *
     * @param deviceMirror 设备镜像
     * @param data         待发送数据（不能超过20字节）
     */
    public void writeData(DeviceMirror deviceMirror, byte[] data) {
        if (deviceMirror != null && deviceMirror.isConnected() && data != null) {
            deviceMirror.writeData(data);
        }
    }

    /**
     * 读取数据
     *
     * @param deviceMirror 设备镜像
     */
    public void readData(DeviceMirror deviceMirror) {
        if (deviceMirror != null && deviceMirror.isConnected()) {
            deviceMirror.readData();
        }
    }

    /**
     * 读取设备信号值
     *
     * @param deviceMirror 设备镜像
     * @param rssiCallback 信号回调
     */
    public void readRemoteRssi(DeviceMirror deviceMirror, IRssiCallback rssiCallback) {
        if (deviceMirror != null && deviceMirror.isConnected() && rssiCallback != null) {
            deviceMirror.readRemoteRssi(rssiCallback);
        }
    }

    /**
     * 注册获取数据通知
     *
     * @param deviceMirror 设备镜像
     * @param isIndication 是否是指示器方式
     */
    public void registerNotify(DeviceMirror deviceMirror, boolean isIndication) {
        if (deviceMirror != null && deviceMirror.isConnected()) {
            deviceMirror.registerNotify(isIndication);
        }
    }

    /**
     * 取消获取数据通知
     *
     * @param deviceMirror 设备镜像
     * @param isIndication 是否是指示器方式
     */
    public void unregisterNotify(DeviceMirror deviceMirror, boolean isIndication) {
        if (deviceMirror != null && deviceMirror.isConnected()) {
            deviceMirror.unregisterNotify(isIndication);
        }
    }

    /**
     * 设置接收数据监听
     *
     * @param deviceMirror    设备镜像
     * @param key             接收数据回调key，由serviceUUID+characteristicUUID+descriptorUUID组成
     * @param receiveCallback 接收数据回调
     */
    public void setNotifyListener(DeviceMirror deviceMirror, String key, IBleCallback receiveCallback) {
        if (deviceMirror != null && deviceMirror.isConnected()) {
            deviceMirror.setNotifyListener(key, receiveCallback);
        }
    }

    /**
     * 清除资源，在退出应用时调用
     */
    public void clear() {
        if (deviceMirrorPool != null) {
            deviceMirrorPool.clear();
        }
    }

    /**
     * 获取Context
     *
     * @return 返回Context
     */
    public Context getContext() {
        return context;
    }

    /**
     * 获取蓝牙管理
     *
     * @return 返回蓝牙管理
     */
    public BluetoothManager getBluetoothManager() {
        return bluetoothManager;
    }

    /**
     * 获取蓝牙适配器
     *
     * @return 返回蓝牙适配器
     */
    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    /**
     * 获取设备镜像池
     *
     * @return
     */
    public DeviceMirrorPool getDeviceMirrorPool() {
        return deviceMirrorPool;
    }
}
