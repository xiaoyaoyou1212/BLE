package com.vise.baseble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import com.vise.baseble.callback.IConnectCallback;
import com.vise.baseble.callback.scan.IScanCallback;
import com.vise.baseble.callback.scan.ScanCallback;
import com.vise.baseble.callback.scan.SingleFilterScanCallback;
import com.vise.baseble.core.DeviceMirror;
import com.vise.baseble.core.DeviceMirrorPool;
import com.vise.baseble.exception.TimeoutException;
import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.baseble.model.BluetoothLeDeviceStore;

import java.util.UUID;

import static com.vise.baseble.common.BleConstant.DEFAULT_CONN_TIME;
import static com.vise.baseble.common.BleConstant.DEFAULT_OPERATE_TIME;
import static com.vise.baseble.common.BleConstant.DEFAULT_SCAN_TIME;

/**
 * @Description: BLE设备操作入口
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 17/8/1 22:24.
 */
public class ViseBle {
    private Context context;//上下文
    private BluetoothManager bluetoothManager;//蓝牙管理
    private BluetoothAdapter bluetoothAdapter;//蓝牙适配器
    private int scanTimeout = DEFAULT_SCAN_TIME;//扫描超时时间
    private int connectTimeout = DEFAULT_CONN_TIME;//连接超时时间
    private int operateTimeout = DEFAULT_OPERATE_TIME;//数据操作超时时间
    private DeviceMirrorPool deviceMirrorPool;//设备连接池
    private DeviceMirror deviceMirror;//连接的设备

    private static ViseBle instance;//入口操作管理

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
     * 初始化
     * @param context 上下文
     */
    public void init(Context context) {
        if (this.context == null) {
            this.context = context.getApplicationContext();
            bluetoothManager = (BluetoothManager) this.context.getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
            deviceMirrorPool = new DeviceMirrorPool();
        }
    }

    /**
     * 开始扫描
     * @param leScanCallback 回调
     */
    public void startLeScan(BluetoothAdapter.LeScanCallback leScanCallback) {
        if (bluetoothAdapter != null) {
            bluetoothAdapter.startLeScan(leScanCallback);
        }
    }

    /**
     * 停止扫描
     * @param leScanCallback 回调
     */
    public void stopLeScan(BluetoothAdapter.LeScanCallback leScanCallback) {
        if (bluetoothAdapter != null) {
            bluetoothAdapter.stopLeScan(leScanCallback);
        }
    }

    /**
     * 开始扫描
     * @param periodScanCallback 自定义回调
     */
    public void startScan(ScanCallback periodScanCallback) {
        if (periodScanCallback == null) {
            throw new IllegalArgumentException("this ScanCallback is Null!");
        }
        periodScanCallback.setScan(true).setScanTimeout(scanTimeout).scan();
    }

    /**
     * 停止扫描
     * @param periodScanCallback 自定义回调
     */
    public void stopScan(ScanCallback periodScanCallback) {
        if (periodScanCallback == null) {
            throw new IllegalArgumentException("this ScanCallback is Null!");
        }
        periodScanCallback.setScan(false).removeHandlerMsg().scan();
    }

    public void connect(BluetoothLeDevice bluetoothLeDevice, IConnectCallback connectCallback) {
        if (bluetoothLeDevice == null || connectCallback == null) {
            return;
        }
        deviceMirror = new DeviceMirror(bluetoothLeDevice);
        deviceMirror.connect(connectCallback);
    }

    public void connectByMac(String mac, final IConnectCallback connectCallback) {
        if (mac == null || connectCallback == null) {
            return;
        }
        startScan(new SingleFilterScanCallback(new IScanCallback() {
            @Override
            public void onDeviceFound(BluetoothLeDeviceStore bluetoothLeDeviceStore) {

            }

            @Override
            public void onScanFinish(BluetoothLeDeviceStore bluetoothLeDeviceStore) {
                if (bluetoothLeDeviceStore.getDeviceList().size() > 0 ) {
                    connect(bluetoothLeDeviceStore.getDeviceList().get(0), connectCallback);
                }
            }

            @Override
            public void onScanTimeout() {
                connectCallback.onConnectFailure(new TimeoutException());
            }
        }).setDeviceMac(mac));
    }

    public void connectByName(String name, final IConnectCallback connectCallback) {
        if (name == null || connectCallback == null) {
            return;
        }
        startScan(new SingleFilterScanCallback(new IScanCallback() {
            @Override
            public void onDeviceFound(BluetoothLeDeviceStore bluetoothLeDeviceStore) {

            }

            @Override
            public void onScanFinish(BluetoothLeDeviceStore bluetoothLeDeviceStore) {
                if (bluetoothLeDeviceStore.getDeviceList().size() > 0 ) {
                    connect(bluetoothLeDeviceStore.getDeviceList().get(0), connectCallback);
                }
            }

            @Override
            public void onScanTimeout() {
                connectCallback.onConnectFailure(new TimeoutException());
            }
        }).setDeviceName(name));
    }

    public void writeData(UUID serviceUUID, UUID characteristicUUID, UUID descriptorUUID, byte[] data) {

    }

    public void readData(UUID serviceUUID, UUID characteristicUUID, UUID descriptorUUID, byte[] data) {

    }

    public void registerNotifyListener(UUID serviceUUID, UUID characteristicUUID, UUID descriptorUUID) {

    }

    public void unregisterNotifyListener(UUID serviceUUID, UUID characteristicUUID, UUID descriptorUUID) {

    }

    /**
     * 获取Context
     * @return 返回Context
     */
    public Context getContext() {
        return context;
    }

    /**
     * 获取蓝牙管理
     * @return 返回蓝牙管理
     */
    public BluetoothManager getBluetoothManager() {
        return bluetoothManager;
    }

    /**
     * 获取蓝牙适配器
     * @return 返回蓝牙适配器
     */
    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    /**
     * 获取发送数据超时时间
     * @return 返回发送数据超时时间
     */
    public int getOperateTimeout() {
        return operateTimeout;
    }

    /**
     * 设置发送数据超时时间
     * @param operateTimeout 发送数据超时时间
     * @return 返回ViseBle
     */
    public ViseBle setOperateTimeout(int operateTimeout) {
        this.operateTimeout = operateTimeout;
        return this;
    }

    /**
     * 获取连接超时时间
     * @return 返回连接超时时间
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * 设置连接超时时间
     * @param connectTimeout 连接超时时间
     * @return 返回ViseBle
     */
    public ViseBle setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    /**
     * 获取扫描超时时间
     * @return 返回扫描超时时间
     */
    public int getScanTimeout() {
        return scanTimeout;
    }

    /**
     * 设置扫描超时时间
     * @param scanTimeout 扫描超时时间
     * @return 返回ViseBle
     */
    public ViseBle setScanTimeout(int scanTimeout) {
        this.scanTimeout = scanTimeout;
        return this;
    }
}
