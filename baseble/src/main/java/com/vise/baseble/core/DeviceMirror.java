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
import com.vise.baseble.callback.IBleCallback;
import com.vise.baseble.callback.IConnectCallback;
import com.vise.baseble.callback.IRssiCallback;
import com.vise.baseble.common.BleConfig;
import com.vise.baseble.common.BleConstant;
import com.vise.baseble.common.ConnectState;
import com.vise.baseble.common.PropertyType;
import com.vise.baseble.exception.BleException;
import com.vise.baseble.exception.ConnectException;
import com.vise.baseble.exception.GattException;
import com.vise.baseble.exception.TimeoutException;
import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.baseble.utils.HexUtil;
import com.vise.log.ViseLog;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.vise.baseble.common.BleConstant.MSG_CONNECT_RETRY;
import static com.vise.baseble.common.BleConstant.MSG_CONNECT_TIMEOUT;
import static com.vise.baseble.common.BleConstant.MSG_READ_DATA_RETRY;
import static com.vise.baseble.common.BleConstant.MSG_READ_DATA_TIMEOUT;
import static com.vise.baseble.common.BleConstant.MSG_RECEIVE_DATA_RETRY;
import static com.vise.baseble.common.BleConstant.MSG_RECEIVE_DATA_TIMEOUT;
import static com.vise.baseble.common.BleConstant.MSG_WRITE_DATA_RETRY;
import static com.vise.baseble.common.BleConstant.MSG_WRITE_DATA_TIMEOUT;

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
    private IRssiCallback rssiCallback;//获取信号值回调
    private IConnectCallback connectCallback;//连接回调
    private int connectRetryCount = 0;//当前连接重试次数
    private int writeDataRetryCount = 0;//当前写入数据重试次数
    private int readDataRetryCount = 0;//当前读取数据重试次数
    private int receiveDataRetryCount = 0;//当前接收数据重试次数
    private boolean isActiveDisconnect = false;//是否主动断开连接
    private boolean isIndication;//是否是指示器方式
    private boolean enable;//是否设置使能
    private byte[] writeData;//写入数据
    private ConnectState connectState = ConnectState.CONNECT_INIT;//设备状态描述
    private volatile HashMap<String, BluetoothGattChannel> writeInfoMap = new HashMap<>();//写入数据GATT信息集合
    private volatile HashMap<String, BluetoothGattChannel> readInfoMap = new HashMap<>();//读取数据GATT信息集合
    private volatile HashMap<String, BluetoothGattChannel> enableInfoMap = new HashMap<>();//设置使能GATT信息集合
    private volatile HashMap<String, IBleCallback> bleCallbackMap = new HashMap<>();//数据操作回调集合
    private volatile HashMap<String, IBleCallback> receiveCallbackMap = new HashMap<>();//数据接收回调集合

    private final Handler handler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_CONNECT_TIMEOUT) {
                connectFailure(new TimeoutException());
            } else if (msg.what == MSG_CONNECT_RETRY) {
                connect();
            } else if (msg.what == MSG_WRITE_DATA_TIMEOUT) {
                writeFailure(new TimeoutException(), true);
            } else if (msg.what == MSG_WRITE_DATA_RETRY) {
                write(writeData);
            } else if (msg.what == MSG_READ_DATA_TIMEOUT) {
                readFailure(new TimeoutException(), true);
            } else if (msg.what == MSG_READ_DATA_RETRY) {
                read();
            } else if (msg.what == MSG_RECEIVE_DATA_TIMEOUT) {
                enableFailure(new TimeoutException(), true);
            } else if (msg.what == MSG_RECEIVE_DATA_RETRY) {
                enable(enable, isIndication);
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
                    "  ,thread: " + Thread.currentThread());
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                gatt.discoverServices();
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                close();
                if (connectCallback != null) {
                    if (handler != null) {
                        handler.removeCallbacksAndMessages(null);
                    }
                    ViseBle.getInstance().getDeviceMirrorPool().removeDeviceMirror(deviceMirror);
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        connectState = ConnectState.CONNECT_DISCONNECT;
                        connectCallback.onDisconnect(isActiveDisconnect);
                    } else {
                        connectState = ConnectState.CONNECT_FAILURE;
                        connectCallback.onConnectFailure(new ConnectException(gatt, status));
                    }
                }
            } else if (newState == BluetoothGatt.STATE_CONNECTING) {
                connectState = ConnectState.CONNECT_PROCESS;
            }
        }

        /**
         * 发现服务，主要用来获取设备支持的服务列表
         * @param gatt GATT
         * @param status 当前状态
         */
        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            ViseLog.i("onServicesDiscovered  status: " + status + "  ,thread: " + Thread.currentThread());
            if (handler != null) {
                handler.removeMessages(MSG_CONNECT_TIMEOUT);
            }
            if (status == 0) {
                ViseLog.i("onServicesDiscovered connectSuccess.");
                bluetoothGatt = gatt;
                connectState = ConnectState.CONNECT_SUCCESS;
                if (connectCallback != null) {
                    isActiveDisconnect = false;
                    ViseBle.getInstance().getDeviceMirrorPool().addDeviceMirror(deviceMirror);
                    connectCallback.onConnectSuccess(deviceMirror);
                }
            } else {
                connectFailure(new ConnectException(gatt, status));
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
            ViseLog.i("onCharacteristicRead  status: " + status + ", data:" + HexUtil.encodeHexStr(characteristic.getValue()) +
                    "  ,thread: " + Thread.currentThread());
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleSuccessData(readInfoMap, characteristic.getValue(), status, true);
            } else {
                readFailure(new GattException(status), true);
            }
        }

        /**
         * 写入特征值，主要用来发送数据到设备
         * @param gatt GATT
         * @param characteristic 特征值
         * @param status 当前状态
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            ViseLog.i("onCharacteristicWrite  status: " + status + ", data:" + HexUtil.encodeHexStr(characteristic.getValue()) +
                    "  ,thread: " + Thread.currentThread());
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleSuccessData(writeInfoMap, characteristic.getValue(), status, false);
            } else {
                writeFailure(new GattException(status), true);
            }
        }

        /**
         * 特征值改变，主要用来接收设备返回的数据信息
         * @param gatt GATT
         * @param characteristic 特征值
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            ViseLog.i("onCharacteristicChanged data:" + HexUtil.encodeHexStr(characteristic.getValue()) +
                    "  ,thread: " + Thread.currentThread());
            for (Map.Entry<String, IBleCallback> receiveEntry : receiveCallbackMap.entrySet()) {
                String receiveKey = receiveEntry.getKey();
                IBleCallback receiveValue = receiveEntry.getValue();
                for (Map.Entry<String, BluetoothGattChannel> gattInfoEntry : enableInfoMap.entrySet()) {
                    String bluetoothGattInfoKey = gattInfoEntry.getKey();
                    BluetoothGattChannel bluetoothGattInfoValue = gattInfoEntry.getValue();
                    if (receiveKey.equals(bluetoothGattInfoKey)) {
                        receiveValue.onSuccess(characteristic.getValue(), bluetoothGattInfoValue, bluetoothLeDevice);
                    }
                }
            }
        }

        /**
         * 读取属性描述值，主要用来获取设备当前属性描述的值
         * @param gatt GATT
         * @param descriptor 属性描述
         * @param status 当前状态
         */
        @Override
        public void onDescriptorRead(BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
            ViseLog.i("onDescriptorRead  status: " + status + ", data:" + HexUtil.encodeHexStr(descriptor.getValue()) +
                    "  ,thread: " + Thread.currentThread());
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleSuccessData(readInfoMap, descriptor.getValue(), status, true);
            } else {
                readFailure(new GattException(status), true);
            }
        }

        /**
         * 写入属性描述值，主要用来根据当前属性描述值写入数据到设备
         * @param gatt GATT
         * @param descriptor 属性描述值
         * @param status 当前状态
         */
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
            ViseLog.i("onDescriptorWrite  status: " + status + ", data:" + HexUtil.encodeHexStr(descriptor.getValue()) +
                    "  ,thread: " + Thread.currentThread());
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleSuccessData(writeInfoMap, descriptor.getValue(), status, false);
            } else {
                writeFailure(new GattException(status), true);
            }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleSuccessData(enableInfoMap, descriptor.getValue(), status, false);
            } else {
                enableFailure(new GattException(status), true);
            }
        }

        /**
         * 阅读设备信号值
         * @param gatt GATT
         * @param rssi 设备当前信号
         * @param status 当前状态
         */
        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            ViseLog.i("onReadRemoteRssi  status: " + status + ", rssi:" + rssi +
                    "  ,thread: " + Thread.currentThread());
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (rssiCallback != null) {
                    rssiCallback.onSuccess(rssi);
                }
            } else {
                if (rssiCallback != null) {
                    rssiCallback.onFailure(new GattException(status));
                }
            }
        }
    };

    public DeviceMirror(BluetoothLeDevice bluetoothLeDevice) {
        deviceMirror = this;
        this.bluetoothLeDevice = bluetoothLeDevice;
        this.uniqueSymbol = bluetoothLeDevice.getAddress() + bluetoothLeDevice.getName();
    }

    /**
     * 连接设备
     *
     * @param connectCallback
     */
    public synchronized void connect(IConnectCallback connectCallback) {
        if (connectState == ConnectState.CONNECT_SUCCESS || connectState == ConnectState.CONNECT_PROCESS
                || (connectState == ConnectState.CONNECT_INIT && connectRetryCount != 0)) {
            ViseLog.e("this connect state is connecting, connectSuccess or current retry count less than config connect retry count.");
            return;
        }
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        this.connectCallback = connectCallback;
        connectRetryCount = 0;
        connect();
    }

    /**
     * 绑定一个具备读写或可通知能力的通道，设置需要操作数据的相关信息，包含：数据操作回调，数据操作类型，数据通道建立所需的UUID。
     *
     * @param bleCallback
     * @param bluetoothGattChannel
     */
    public synchronized void bindChannel(IBleCallback bleCallback, BluetoothGattChannel bluetoothGattChannel) {
        if (bleCallback != null && bluetoothGattChannel != null) {
            String key = bluetoothGattChannel.getGattInfoKey();
            PropertyType propertyType = bluetoothGattChannel.getPropertyType();
            if (!bleCallbackMap.containsKey(key)) {
                bleCallbackMap.put(key, bleCallback);
            }
            if (propertyType == PropertyType.PROPERTY_READ) {
                if (!readInfoMap.containsKey(key)) {
                    readInfoMap.put(key, bluetoothGattChannel);
                }
            } else if (propertyType == PropertyType.PROPERTY_WRITE) {
                if (!writeInfoMap.containsKey(key)) {
                    writeInfoMap.put(key, bluetoothGattChannel);
                }
            } else if (propertyType == PropertyType.PROPERTY_NOTIFY) {
                if (!enableInfoMap.containsKey(key)) {
                    enableInfoMap.put(key, bluetoothGattChannel);
                }
            } else if (propertyType == PropertyType.PROPERTY_INDICATE) {
                if (!enableInfoMap.containsKey(key)) {
                    enableInfoMap.put(key, bluetoothGattChannel);
                }
            }
        }
    }

    /**
     * 解绑通道
     *
     * @param bluetoothGattChannel
     */
    public synchronized void unbindChannel(BluetoothGattChannel bluetoothGattChannel) {
        if (bluetoothGattChannel != null) {
            String key = bluetoothGattChannel.getGattInfoKey();
            if (bleCallbackMap.containsKey(key)) {
                bleCallbackMap.remove(key);
            }
            if (readInfoMap.containsKey(key)) {
                readInfoMap.remove(key);
            } else if (writeInfoMap.containsKey(key)) {
                writeInfoMap.remove(key);
            } else if (enableInfoMap.containsKey(key)) {
                enableInfoMap.remove(key);
            }
        }
    }

    /**
     * 写入数据
     *
     * @param data
     */
    public void writeData(byte[] data) {
        if (data == null || data.length > 20) {
            ViseLog.e("this data is null or length beyond 20 byte.");
            return;
        }
        if (!checkBluetoothGattInfo(writeInfoMap)) {
            return;
        }
        if (handler != null) {
            handler.removeMessages(MSG_WRITE_DATA_TIMEOUT);
            handler.removeMessages(MSG_WRITE_DATA_RETRY);
        }
        writeDataRetryCount = 0;
        writeData = data;
        write(data);
    }

    /**
     * 读取数据
     */
    public void readData() {
        if (!checkBluetoothGattInfo(readInfoMap)) {
            return;
        }
        if (handler != null) {
            handler.removeMessages(MSG_READ_DATA_TIMEOUT);
            handler.removeMessages(MSG_READ_DATA_RETRY);
        }
        readDataRetryCount = 0;
        read();
    }

    /**
     * 获取设备信号值
     *
     * @param rssiCallback
     */
    public void readRemoteRssi(IRssiCallback rssiCallback) {
        this.rssiCallback = rssiCallback;
        if (bluetoothGatt != null) {
            bluetoothGatt.readRemoteRssi();
        }
    }

    /**
     * 注册获取数据通知
     *
     * @param isIndication
     */
    public void registerNotify(boolean isIndication) {
        if (!checkBluetoothGattInfo(enableInfoMap)) {
            return;
        }
        if (handler != null) {
            handler.removeMessages(MSG_RECEIVE_DATA_TIMEOUT);
            handler.removeMessages(MSG_RECEIVE_DATA_RETRY);
        }
        receiveDataRetryCount = 0;
        enable = true;
        this.isIndication = isIndication;
        enable(enable, this.isIndication);
    }

    /**
     * 取消获取数据通知
     *
     * @param isIndication
     */
    public void unregisterNotify(boolean isIndication) {
        if (!checkBluetoothGattInfo(enableInfoMap)) {
            return;
        }
        if (handler != null) {
            handler.removeMessages(MSG_RECEIVE_DATA_TIMEOUT);
            handler.removeMessages(MSG_RECEIVE_DATA_RETRY);
        }
        enable = false;
        this.isIndication = isIndication;
        enable(enable, this.isIndication);
    }

    /**
     * 设置接收数据监听
     *
     * @param key             接收数据回调key，由serviceUUID+characteristicUUID+descriptorUUID组成
     * @param receiveCallback 接收数据回调
     */
    public void setNotifyListener(String key, IBleCallback receiveCallback) {
        receiveCallbackMap.put(key, receiveCallback);
    }

    /**
     * 获取当前连接失败重试次数
     *
     * @return
     */
    public int getConnectRetryCount() {
        return connectRetryCount;
    }

    /**
     * 获取当前读取数据失败重试次数
     *
     * @return
     */
    public int getReadDataRetryCount() {
        return readDataRetryCount;
    }

    /**
     * 获取当前使能数据失败重试次数
     *
     * @return
     */
    public int getReceiveDataRetryCount() {
        return receiveDataRetryCount;
    }

    /**
     * 获取当前写入数据失败重试次数
     *
     * @return
     */
    public int getWriteDataRetryCount() {
        return writeDataRetryCount;
    }

    /**
     * 获取设备唯一标识
     *
     * @return
     */
    public String getUniqueSymbol() {
        return uniqueSymbol;
    }

    /**
     * 获取蓝牙GATT
     *
     * @return 返回蓝牙GATT
     */
    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }

    /**
     * 获取设备连接状态
     *
     * @return 返回设备连接状态
     */
    public ConnectState getConnectState() {
        return connectState;
    }

    /**
     * 获取服务列表
     *
     * @return
     */
    public List<BluetoothGattService> getGattServiceList() {
        if (bluetoothGatt != null) {
            return bluetoothGatt.getServices();
        }
        return null;
    }

    /**
     * 根据服务UUID获取指定服务
     *
     * @param serviceUuid
     * @return
     */
    public BluetoothGattService getGattService(UUID serviceUuid) {
        if (bluetoothGatt != null && serviceUuid != null) {
            return bluetoothGatt.getService(serviceUuid);
        }
        return null;
    }

    /**
     * 获取某个服务的特征值列表
     *
     * @param serviceUuid
     * @return
     */
    public List<BluetoothGattCharacteristic> getGattCharacteristicList(UUID serviceUuid) {
        if (getGattService(serviceUuid) != null && serviceUuid != null) {
            return getGattService(serviceUuid).getCharacteristics();
        }
        return null;
    }

    /**
     * 根据特征值UUID获取某个服务的指定特征值
     *
     * @param serviceUuid
     * @param characteristicUuid
     * @return
     */
    public BluetoothGattCharacteristic getGattCharacteristic(UUID serviceUuid, UUID characteristicUuid) {
        if (getGattService(serviceUuid) != null && serviceUuid != null && characteristicUuid != null) {
            return getGattService(serviceUuid).getCharacteristic(characteristicUuid);
        }
        return null;
    }

    /**
     * 获取某个特征值的描述属性列表
     *
     * @param serviceUuid
     * @param characteristicUuid
     * @return
     */
    public List<BluetoothGattDescriptor> getGattDescriptorList(UUID serviceUuid, UUID characteristicUuid) {
        if (getGattCharacteristic(serviceUuid, characteristicUuid) != null && serviceUuid != null && characteristicUuid != null) {
            return getGattCharacteristic(serviceUuid, characteristicUuid).getDescriptors();
        }
        return null;
    }

    /**
     * 根据描述属性UUID获取某个特征值的指定属性值
     *
     * @param serviceUuid
     * @param characteristicUuid
     * @param descriptorUuid
     * @return
     */
    public BluetoothGattDescriptor getGattDescriptor(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid) {
        if (getGattCharacteristic(serviceUuid, characteristicUuid) != null && serviceUuid != null && characteristicUuid != null && descriptorUuid != null) {
            return getGattCharacteristic(serviceUuid, characteristicUuid).getDescriptor(descriptorUuid);
        }
        return null;
    }

    /**
     * 获取设备详细信息
     *
     * @return
     */
    public BluetoothLeDevice getBluetoothLeDevice() {
        return bluetoothLeDevice;
    }

    /**
     * 设备是否连接
     *
     * @return
     */
    public boolean isConnected() {
        return connectState == ConnectState.CONNECT_SUCCESS;
    }

    /**
     * 移除数据操作回调
     *
     * @param key
     */
    public synchronized void removeBleCallback(String key) {
        if (bleCallbackMap.containsKey(key)) {
            bleCallbackMap.remove(key);
        }
    }

    /**
     * 移除接收数据回调
     *
     * @param key
     */
    public synchronized void removeReceiveCallback(String key) {
        if (receiveCallbackMap.containsKey(key)) {
            receiveCallbackMap.remove(key);
        }
    }

    /**
     * 移除所有回调
     */
    public synchronized void removeAllCallback() {
        bleCallbackMap.clear();
        receiveCallbackMap.clear();
    }

    /**
     * 刷新设备缓存
     *
     * @return 返回是否刷新成功
     */
    public synchronized boolean refreshDeviceCache() {
        try {
            final Method refresh = BluetoothGatt.class.getMethod("refresh");
            if (refresh != null && bluetoothGatt != null) {
                final boolean success = (Boolean) refresh.invoke(getBluetoothGatt());
                ViseLog.i("Refreshing result: " + success);
                return success;
            }
        } catch (Exception e) {
            ViseLog.e("An exception occured while refreshing device" + e);
        }
        return false;
    }

    /**
     * 主动断开设备连接
     */
    public synchronized void disconnect() {
        connectState = ConnectState.CONNECT_INIT;
        connectRetryCount = 0;
        if (bluetoothGatt != null) {
            isActiveDisconnect = true;
            bluetoothGatt.disconnect();
        }
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
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

    @Override
    public String toString() {
        return "DeviceMirror{" +
                "bluetoothLeDevice=" + bluetoothLeDevice +
                ", uniqueSymbol='" + uniqueSymbol + '\'' +
                '}';
    }

    /**
     * 清除设备资源，在不使用该设备时调用
     */
    public synchronized void clear() {
        ViseLog.i("deviceMirror clear.");
        disconnect();
        refreshDeviceCache();
        close();
        if (bleCallbackMap != null) {
            bleCallbackMap.clear();
        }
        if (receiveCallbackMap != null) {
            receiveCallbackMap.clear();
        }
        if (writeInfoMap != null) {
            writeInfoMap.clear();
        }
        if (readInfoMap != null) {
            readInfoMap.clear();
        }
        if (enableInfoMap != null) {
            enableInfoMap.clear();
        }
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    /**
     * UUID转换
     *
     * @param uuid
     * @return 返回UUID
     */
    private UUID formUUID(String uuid) {
        return uuid == null ? null : UUID.fromString(uuid);
    }

    /**
     * 检查BluetoothGattChannel集合是否有值
     *
     * @param bluetoothGattInfoHashMap
     * @return
     */
    private boolean checkBluetoothGattInfo(HashMap<String, BluetoothGattChannel> bluetoothGattInfoHashMap) {
        if (bluetoothGattInfoHashMap == null || bluetoothGattInfoHashMap.size() == 0) {
            ViseLog.e("this bluetoothGattInfo map is not value.");
            return false;
        }
        return true;
    }

    /**
     * 连接设备
     */
    private synchronized void connect() {
        if (handler != null) {
            handler.removeMessages(MSG_CONNECT_TIMEOUT);
            handler.sendEmptyMessageDelayed(MSG_CONNECT_TIMEOUT, BleConfig.getInstance().getConnectTimeout());
        }
        connectState = ConnectState.CONNECT_PROCESS;
        if (bluetoothLeDevice != null && bluetoothLeDevice.getDevice() != null) {
            bluetoothLeDevice.getDevice().connectGatt(ViseBle.getInstance().getContext(), false, coreGattCallback);
        }
    }

    /**
     * 设置使能
     *
     * @param enable       是否具备使能
     * @param isIndication 是否是指示器方式
     * @return
     */
    private synchronized boolean enable(boolean enable, boolean isIndication) {
        if (handler != null) {
            handler.removeMessages(MSG_RECEIVE_DATA_TIMEOUT);
            handler.sendEmptyMessageDelayed(MSG_RECEIVE_DATA_TIMEOUT, BleConfig.getInstance().getOperateTimeout());
        }
        boolean success = false;
        for (Map.Entry<String, BluetoothGattChannel> entry : enableInfoMap.entrySet()) {
            String bluetoothGattInfoKey = entry.getKey();
            BluetoothGattChannel bluetoothGattInfoValue = entry.getValue();
            if (bluetoothGatt != null && bluetoothGattInfoValue.getCharacteristic() != null) {
                success = bluetoothGatt.setCharacteristicNotification(bluetoothGattInfoValue.getCharacteristic(), enable);
            }
            BluetoothGattDescriptor bluetoothGattDescriptor = null;
            if (bluetoothGattInfoValue.getCharacteristic() != null && bluetoothGattInfoValue.getDescriptor() != null) {
                bluetoothGattDescriptor = bluetoothGattInfoValue.getDescriptor();
            } else if (bluetoothGattInfoValue.getCharacteristic() != null && bluetoothGattInfoValue.getDescriptor() == null) {
                if (bluetoothGattInfoValue.getCharacteristic().getDescriptors() != null
                        && bluetoothGattInfoValue.getCharacteristic().getDescriptors().size() == 1) {
                    bluetoothGattDescriptor = bluetoothGattInfoValue.getCharacteristic().getDescriptors().get(0);
                } else {
                    bluetoothGattDescriptor = bluetoothGattInfoValue.getCharacteristic()
                            .getDescriptor(UUID.fromString(BleConstant.CLIENT_CHARACTERISTIC_CONFIG));
                }
            }
            if (bluetoothGattDescriptor != null) {
                bluetoothGattInfoValue.setDescriptor(bluetoothGattDescriptor);
                if (isIndication) {
                    if (enable) {
                        bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                    } else {
                        bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                    }
                } else {
                    if (enable) {
                        bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    } else {
                        bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                    }
                }
                if (bluetoothGatt != null) {
                    bluetoothGatt.writeDescriptor(bluetoothGattDescriptor);
                }
            }
        }
        return success;
    }

    /**
     * 读取数据
     *
     * @return
     */
    private synchronized boolean read() {
        if (handler != null) {
            handler.removeMessages(MSG_READ_DATA_TIMEOUT);
            handler.sendEmptyMessageDelayed(MSG_READ_DATA_TIMEOUT, BleConfig.getInstance().getOperateTimeout());
        }
        boolean success = false;
        for (Map.Entry<String, BluetoothGattChannel> entry : readInfoMap.entrySet()) {
            String bluetoothGattInfoKey = entry.getKey();
            BluetoothGattChannel bluetoothGattInfoValue = entry.getValue();
            if (bluetoothGatt != null && bluetoothGattInfoValue.getCharacteristic() != null && bluetoothGattInfoValue.getDescriptor() != null) {
                success = bluetoothGatt.readDescriptor(bluetoothGattInfoValue.getDescriptor());
            } else if (bluetoothGatt != null && bluetoothGattInfoValue.getCharacteristic() != null && bluetoothGattInfoValue.getDescriptor() == null) {
                success = bluetoothGatt.readCharacteristic(bluetoothGattInfoValue.getCharacteristic());
            }
        }
        return success;
    }

    /**
     * 写入数据
     *
     * @param data
     * @return
     */
    private synchronized boolean write(byte[] data) {
        if (handler != null) {
            handler.removeMessages(MSG_WRITE_DATA_TIMEOUT);
            handler.sendEmptyMessageDelayed(MSG_WRITE_DATA_TIMEOUT, BleConfig.getInstance().getOperateTimeout());
        }
        boolean success = false;
        for (Map.Entry<String, BluetoothGattChannel> entry : writeInfoMap.entrySet()) {
            String bluetoothGattInfoKey = entry.getKey();
            BluetoothGattChannel bluetoothGattInfoValue = entry.getValue();
            if (bluetoothGatt != null && bluetoothGattInfoValue.getCharacteristic() != null && bluetoothGattInfoValue.getDescriptor() != null) {
                bluetoothGattInfoValue.getDescriptor().setValue(data);
                success = bluetoothGatt.writeDescriptor(bluetoothGattInfoValue.getDescriptor());
            } else if (bluetoothGatt != null && bluetoothGattInfoValue.getCharacteristic() != null && bluetoothGattInfoValue.getDescriptor() == null) {
                bluetoothGattInfoValue.getCharacteristic().setValue(data);
                success = bluetoothGatt.writeCharacteristic(bluetoothGattInfoValue.getCharacteristic());
            }
        }
        return success;
    }

    /**
     * 连接失败处理
     *
     * @param bleException 回调异常
     */
    private void connectFailure(BleException bleException) {
        if (connectRetryCount < BleConfig.getInstance().getConnectRetryCount()) {
            connectRetryCount++;
            if (handler != null) {
                handler.removeMessages(MSG_CONNECT_TIMEOUT);
                handler.sendEmptyMessageDelayed(MSG_CONNECT_RETRY, BleConfig.getInstance().getConnectRetryInterval());
            }
            ViseLog.i("connectFailure connectRetryCount is " + connectRetryCount);
        } else {
            if (bleException instanceof TimeoutException) {
                connectState = ConnectState.CONNECT_TIMEOUT;
            } else {
                connectState = ConnectState.CONNECT_FAILURE;
            }
            close();
            if (connectCallback != null) {
                connectCallback.onConnectFailure(bleException);
            }
            ViseLog.i("connectFailure " + bleException);
        }
    }

    /**
     * 使能失败
     *
     * @param bleException
     * @param isRemoveCall
     */
    private void enableFailure(BleException bleException, boolean isRemoveCall) {
        if (receiveDataRetryCount < BleConfig.getInstance().getOperateRetryCount()) {
            receiveDataRetryCount++;
            if (handler != null) {
                handler.removeMessages(MSG_RECEIVE_DATA_TIMEOUT);
                handler.sendEmptyMessageDelayed(MSG_RECEIVE_DATA_RETRY, BleConfig.getInstance().getOperateRetryInterval());
            }
            ViseLog.i("enableFailure receiveDataRetryCount is " + receiveDataRetryCount);
        } else {
            handleFailureData(enableInfoMap, bleException, isRemoveCall);
            ViseLog.i("enableFailure " + bleException);
        }
    }

    /**
     * 读取数据失败
     *
     * @param bleException
     * @param isRemoveCall
     */
    private void readFailure(BleException bleException, boolean isRemoveCall) {
        if (readDataRetryCount < BleConfig.getInstance().getOperateRetryCount()) {
            readDataRetryCount++;
            if (handler != null) {
                handler.removeMessages(MSG_READ_DATA_TIMEOUT);
                handler.sendEmptyMessageDelayed(MSG_READ_DATA_RETRY, BleConfig.getInstance().getOperateRetryInterval());
            }
            ViseLog.i("readFailure readDataRetryCount is " + readDataRetryCount);
        } else {
            handleFailureData(readInfoMap, bleException, isRemoveCall);
            ViseLog.i("readFailure " + bleException);
        }
    }

    /**
     * 写入数据失败
     *
     * @param bleException
     * @param isRemoveCall
     */
    private void writeFailure(BleException bleException, boolean isRemoveCall) {
        if (writeDataRetryCount < BleConfig.getInstance().getOperateRetryCount()) {
            writeDataRetryCount++;
            if (handler != null) {
                handler.removeMessages(MSG_WRITE_DATA_TIMEOUT);
                handler.sendEmptyMessageDelayed(MSG_WRITE_DATA_RETRY, BleConfig.getInstance().getOperateRetryInterval());
            }
            ViseLog.i("writeFailure writeDataRetryCount is " + writeDataRetryCount);
        } else {
            handleFailureData(writeInfoMap, bleException, isRemoveCall);
            ViseLog.i("writeFailure " + bleException);
        }
    }

    /**
     * 处理数据发送成功
     *
     * @param bluetoothGattInfoHashMap
     * @param value                    待发送数据
     * @param status                   发送数据状态
     * @param isRemoveCall             是否需要移除回调
     */
    private synchronized void handleSuccessData(HashMap<String, BluetoothGattChannel> bluetoothGattInfoHashMap, byte[] value, int status,
                                                boolean isRemoveCall) {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        String removeBleCallbackKey = null;
        String removeBluetoothGattInfoKey = null;
        for (Map.Entry<String, IBleCallback> callbackEntry : bleCallbackMap.entrySet()) {
            String bleCallbackKey = callbackEntry.getKey();
            IBleCallback bleCallbackValue = callbackEntry.getValue();
            for (Map.Entry<String, BluetoothGattChannel> gattInfoEntry : bluetoothGattInfoHashMap.entrySet()) {
                String bluetoothGattInfoKey = gattInfoEntry.getKey();
                BluetoothGattChannel bluetoothGattInfoValue = gattInfoEntry.getValue();
                if (bleCallbackKey.equals(bluetoothGattInfoKey)) {
                    bleCallbackValue.onSuccess(value, bluetoothGattInfoValue, bluetoothLeDevice);
                    removeBleCallbackKey = bleCallbackKey;
                    removeBluetoothGattInfoKey = bluetoothGattInfoKey;
                }
            }
        }
        synchronized (bleCallbackMap) {
            if (isRemoveCall && removeBleCallbackKey != null && removeBluetoothGattInfoKey != null) {
                bleCallbackMap.remove(removeBleCallbackKey);
                bluetoothGattInfoHashMap.remove(removeBluetoothGattInfoKey);
            }
        }
    }

    /**
     * 处理数据发送失败
     *
     * @param bluetoothGattInfoHashMap
     * @param bleExceprion             回调异常
     * @param isRemoveCall             是否需要移除回调
     */
    private synchronized void handleFailureData(HashMap<String, BluetoothGattChannel> bluetoothGattInfoHashMap, BleException bleExceprion,
                                                boolean isRemoveCall) {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        String removeBleCallbackKey = null;
        String removeBluetoothGattInfoKey = null;
        for (Map.Entry<String, IBleCallback> callbackEntry : bleCallbackMap.entrySet()) {
            String bleCallbackKey = callbackEntry.getKey();
            IBleCallback bleCallbackValue = callbackEntry.getValue();
            for (Map.Entry<String, BluetoothGattChannel> gattInfoEntry : bluetoothGattInfoHashMap.entrySet()) {
                String bluetoothGattInfoKey = gattInfoEntry.getKey();
                if (bleCallbackKey.equals(bluetoothGattInfoKey)) {
                    bleCallbackValue.onFailure(bleExceprion);
                    removeBleCallbackKey = bleCallbackKey;
                    removeBluetoothGattInfoKey = bluetoothGattInfoKey;
                }
            }
        }
        synchronized (bleCallbackMap) {
            if (isRemoveCall && removeBleCallbackKey != null && removeBluetoothGattInfoKey != null) {
                bleCallbackMap.remove(removeBleCallbackKey);
                bluetoothGattInfoHashMap.remove(removeBluetoothGattInfoKey);
            }
        }
    }
}
