package com.vise.baseble.core;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.vise.baseble.ViseBle;
import com.vise.baseble.callback.IConnectCallback;
import com.vise.baseble.common.BleConfig;
import com.vise.baseble.common.ConnectState;
import com.vise.baseble.exception.ConnectException;
import com.vise.baseble.exception.TimeoutException;
import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.baseble.utils.HexUtil;
import com.vise.log.ViseLog;

import java.util.concurrent.LinkedBlockingQueue;

import static com.vise.baseble.common.BleConstant.MSG_CONNECT_RETRY;
import static com.vise.baseble.common.BleConstant.MSG_CONNECT_TIMEOUT;

/**
 * @Description: 设备镜像（设备连接成功后返回的设备信息模型）
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 17/8/1 23:11.
 */
public class DeviceMirror {
    private final DeviceMirror deviceMirror;
    private final String uniqueSymbol;//唯一符号
    private final BluetoothLeDevice bluetoothLeDevice;//设备基础信息

    private BluetoothGatt bluetoothGatt;//蓝牙GATT
    private BluetoothGattService service;//GATT服务
    private BluetoothGattCharacteristic characteristic;//GATT特征值
    private BluetoothGattDescriptor descriptor;//GATT属性描述
    private ConnectState state = ConnectState.CONNECT_DISCONNECT;//设备状态描述

    private LinkedBlockingQueue<DataPacket> writePacketBufferQueue = new LinkedBlockingQueue<>();

    private IConnectCallback connectCallback;//连接回调
    private int connectRetryCount = 0;//当前连接重试次数

    private final Handler handler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_CONNECT_TIMEOUT) {
                if (connectRetryCount < BleConfig.getInstance().getConnectRetryCount()) {
                    connectRetryCount++;
                    if (handler != null) {
                        handler.sendEmptyMessageDelayed(MSG_CONNECT_RETRY, BleConfig.getInstance().getConnectRetryInterval());
                    }
                    ViseLog.i("handleMessage connectRetryCount is " + connectRetryCount);
                } else {
                    state = ConnectState.CONNECT_TIMEOUT;
                    close();
                    if (connectCallback != null) {
                        connectCallback.onConnectFailure(new TimeoutException());
                    }
                    ViseLog.i("handleMessage connectTimeout.");
                }
            } else if (msg.what == MSG_CONNECT_RETRY) {
                connect();
            }
        }
    };

    /**
     * 蓝牙所有相关操作的核心回调类
     */
    private BluetoothGattCallback coreGattCallback = new BluetoothGattCallback() {

        /**
         * 连接状态改变，主要用来分析设备的连接与断开
         * @param gatt GATT
         * @param status 改变前状态
         * @param newState 改变后状态
         */
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            ViseLog.i("onConnectionStateChange  status: " + status + " ,newState: " + newState +
                    "  ,thread: " + Thread.currentThread().getId());
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                gatt.discoverServices();
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                state = ConnectState.CONNECT_DISCONNECT;
                close();
                if (connectCallback != null) {
                    connectCallback.onDisconnect();
                }
            } else if (newState == BluetoothGatt.STATE_CONNECTING) {
                state = ConnectState.CONNECT_PROCESS;
            }
        }

        /**
         * 发现服务，主要用来获取设备支持的服务列表
         * @param gatt GATT
         * @param status 当前状态
         */
        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            ViseLog.i("onServicesDiscovered  status: " + status);
            if (handler != null) {
                handler.removeMessages(MSG_CONNECT_TIMEOUT);
            }
            if (status == 0) {
                bluetoothGatt = gatt;
                state = ConnectState.CONNECT_SUCCESS;
                if (connectCallback != null) {
                    connectCallback.onConnectSuccess(deviceMirror);
                }
                ViseLog.i("onServicesDiscovered connectSuccess.");
            } else {
                if (connectRetryCount < BleConfig.getInstance().getConnectRetryCount()) {
                    connectRetryCount++;
                    if (handler != null) {
                        handler.sendEmptyMessageDelayed(MSG_CONNECT_RETRY, BleConfig.getInstance().getConnectRetryInterval());
                    }
                    ViseLog.i("onServicesDiscovered connectRetryCount is " + connectRetryCount);
                } else {
                    state = ConnectState.CONNECT_FAILURE;
                    close();
                    if (connectCallback != null) {
                        connectCallback.onConnectFailure(new ConnectException(gatt, status));
                    }
                    ViseLog.i("onServicesDiscovered connectFailure.");
                }
            }
        }

        /**
         * 读取特征值，主要用来读取该特征值包含的可读信息
         * @param gatt GATT
         * @param characteristic 特征值
         * @param status 当前状态
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            ViseLog.i("onCharacteristicRead  status: " + status + ", data:" + HexUtil.encodeHexStr(characteristic.getValue()));
        }

        /**
         * 写入特征值，主要用来发送数据到设备
         * @param gatt GATT
         * @param characteristic 特征值
         * @param status 当前状态
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            ViseLog.i("onCharacteristicWrite  status: " + status + ", data:" + HexUtil.encodeHexStr(characteristic.getValue()));
        }

        /**
         * 特征值改变，主要用来接收设备返回的数据信息
         * @param gatt GATT
         * @param characteristic 特征值
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            ViseLog.i("onCharacteristicChanged data:" + HexUtil.encodeHexStr(characteristic.getValue()));
        }

        /**
         * 读取属性描述值，主要用来获取设备当前属性描述的值
         * @param gatt GATT
         * @param descriptor 属性描述
         * @param status 当前状态
         */
        @Override
        public void onDescriptorRead(BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
            ViseLog.i("onDescriptorRead  status: " + status + ", data:" + HexUtil.encodeHexStr(descriptor.getValue()));
        }

        /**
         * 写入属性描述值，主要用来根据当前属性描述值写入数据到设备
         * @param gatt GATT
         * @param descriptor 属性描述值
         * @param status 当前状态
         */
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
            ViseLog.i("onDescriptorWrite  status: " + status + ", data:" + HexUtil.encodeHexStr(descriptor.getValue()));
        }

        /**
         * 阅读设备信号值
         * @param gatt GATT
         * @param rssi 设备当前信号
         * @param status 当前状态
         */
        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, final int rssi, final int status) {
            ViseLog.i("onReadRemoteRssi  status: " + status + ", rssi:" + rssi);
        }
    };

    public DeviceMirror(BluetoothLeDevice bluetoothLeDevice) {
        deviceMirror = this;
        this.bluetoothLeDevice = bluetoothLeDevice;
        this.uniqueSymbol = bluetoothLeDevice.getAddress() + bluetoothLeDevice.getName();
    }

    /**
     * 连接设备
     * @param connectCallback
     */
    public void connect(IConnectCallback connectCallback) {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        this.connectCallback = connectCallback;
        connectRetryCount = 0;
        connect();
    }

    /**
     * 获取设备唯一标识
     * @return
     */
    public String getUniqueSymbol() {
        return uniqueSymbol;
    }

    /**
     * 获取蓝牙GATT
     * @return 返回蓝牙GATT
     */
    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }

    /**
     * 获取当前服务
     * @return 返回当前服务
     */
    public BluetoothGattService getService() {
        return service;
    }

    /**
     * 设置服务
     * @param service 服务
     * @return 返回DeviceMirror
     */
    public DeviceMirror setService(BluetoothGattService service) {
        this.service = service;
        return this;
    }

    /**
     * 获取当前特征值
     * @return 返回当前特征值
     */
    public BluetoothGattCharacteristic getCharacteristic() {
        return characteristic;
    }

    /**
     * 设置特征值
     * @param characteristic 特征值
     * @return 返回DeviceMirror
     */
    public DeviceMirror setCharacteristic(BluetoothGattCharacteristic characteristic) {
        this.characteristic = characteristic;
        return this;
    }

    /**
     * 获取当前属性描述值
     * @return 返回当前属性描述值
     */
    public BluetoothGattDescriptor getDescriptor() {
        return descriptor;
    }

    /**
     * 设置属性描述值
     * @param descriptor 属性描述值
     * @return 返回DeviceMirror
     */
    public DeviceMirror setDescriptor(BluetoothGattDescriptor descriptor) {
        this.descriptor = descriptor;
        return this;
    }

    /**
     * 获取设备连接状态
     * @return 返回设备连接状态
     */
    public ConnectState getState() {
        return state;
    }

    /**
     * 设置设备连接状态
     * @param state 设备连接状态
     * @return 返回ViseBle
     */
    public DeviceMirror setConnectState(ConnectState state) {
        this.state = state;
        return this;
    }

    /**
     * 主动断开设备连接
     */
    public synchronized void disconnect() {
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
        }
    }

    /**
     * 关闭GATT
     */
    public synchronized void close() {
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
        }
    }

    /*=============================================================================================*/

    private synchronized void connect() {
        if (handler != null) {
            handler.sendMessageDelayed(handler.obtainMessage(MSG_CONNECT_TIMEOUT, connectCallback), BleConfig.getInstance().getConnectTimeout());
        }
        state = ConnectState.CONNECT_PROCESS;
        if (bluetoothLeDevice != null && bluetoothLeDevice.getDevice() != null) {
            bluetoothLeDevice.getDevice().connectGatt(ViseBle.getInstance().getContext(), false, coreGattCallback);
        }
    }
}
