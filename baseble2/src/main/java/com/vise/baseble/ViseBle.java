package com.vise.baseble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import com.vise.baseble.callback.ScanCallback;

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
    private boolean isFound = false;//是否发现设备

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
