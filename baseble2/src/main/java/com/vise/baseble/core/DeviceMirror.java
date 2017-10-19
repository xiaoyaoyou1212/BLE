package com.vise.baseble.core;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
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
import com.vise.baseble.exception.ConnectException;
import com.vise.baseble.exception.GattException;
import com.vise.baseble.exception.TimeoutException;
import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.baseble.utils.HexUtil;
import com.vise.log.ViseLog;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
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
    private IRssiCallback rssiCallback;//获取信号值回调
    private IConnectCallback connectCallback;//连接回调
    private int connectRetryCount = 0;//当前连接重试次数
    private ConnectState connectState = ConnectState.CONNECT_DISCONNECT;//设备状态描述
    private HashMap<String, BluetoothGattInfo> writeInfoMap = new HashMap<>();//写入数据GATT信息集合
    private HashMap<String, BluetoothGattInfo> readInfoMap = new HashMap<>();//读取数据GATT信息集合
    private HashMap<String, BluetoothGattInfo> receiveInfoMap = new HashMap<>();//接收数据GATT信息集合
    private HashMap<String, IBleCallback> bleCallbackMap = new HashMap<>();//数据操作回调集合
    private LinkedBlockingQueue<DataPacket> writePacketBufferQueue = new LinkedBlockingQueue<>();

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
                    connectState = ConnectState.CONNECT_TIMEOUT;
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
                connectState = ConnectState.CONNECT_DISCONNECT;
                close();
                if (connectCallback != null) {
                    connectCallback.onDisconnect();
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
            ViseLog.i("onServicesDiscovered  status: " + status);
            if (handler != null) {
                handler.removeMessages(MSG_CONNECT_TIMEOUT);
            }
            if (status == 0) {
                bluetoothGatt = gatt;
                connectState = ConnectState.CONNECT_SUCCESS;
                if (connectCallback != null) {
                    connectCallback.onConnectSuccess(deviceMirror);
                }
                ViseBle.getInstance().getDeviceMirrorPool().addDeviceMirror(deviceMirror);
                ViseLog.i("onServicesDiscovered connectSuccess.");
            } else {
                if (connectRetryCount < BleConfig.getInstance().getConnectRetryCount()) {
                    connectRetryCount++;
                    if (handler != null) {
                        handler.sendEmptyMessageDelayed(MSG_CONNECT_RETRY, BleConfig.getInstance().getConnectRetryInterval());
                    }
                    ViseLog.i("onServicesDiscovered connectRetryCount is " + connectRetryCount);
                } else {
                    connectState = ConnectState.CONNECT_FAILURE;
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
            Iterator<Map.Entry<String, IBleCallback>> bleCallbackIterator = bleCallbackMap.entrySet().iterator();
            Iterator<Map.Entry<String, BluetoothGattInfo>> bluetoothGattInfoIterator = readInfoMap.entrySet().iterator();
            while (bleCallbackIterator.hasNext()) {
                String bleCallbackKey = bleCallbackIterator.next().getKey();
                IBleCallback bleCallbackValue = bleCallbackIterator.next().getValue();
                while (bluetoothGattInfoIterator.hasNext()) {
                    String bluetoothGattInfoKey = bluetoothGattInfoIterator.next().getKey();
                    BluetoothGattInfo bluetoothGattInfoValue = bluetoothGattInfoIterator.next().getValue();
                    if (bleCallbackKey.equals(bluetoothGattInfoKey) && bluetoothGattInfoValue.getDescriptor() == null) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            bleCallbackValue.onSuccess(characteristic.getValue(), bluetoothGattInfoValue);
                        } else {
                            bleCallbackValue.onFailure(new GattException(status));
                        }
                        bleCallbackMap.remove(bleCallbackKey);
                        readInfoMap.remove(bluetoothGattInfoKey);
                    }
                }
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
            ViseLog.i("onCharacteristicWrite  status: " + status + ", data:" + HexUtil.encodeHexStr(characteristic.getValue()));
            Iterator<Map.Entry<String, IBleCallback>> bleCallbackIterator = bleCallbackMap.entrySet().iterator();
            Iterator<Map.Entry<String, BluetoothGattInfo>> bluetoothGattInfoIterator = writeInfoMap.entrySet().iterator();
            while (bleCallbackIterator.hasNext()) {
                String bleCallbackKey = bleCallbackIterator.next().getKey();
                IBleCallback bleCallbackValue = bleCallbackIterator.next().getValue();
                while (bluetoothGattInfoIterator.hasNext()) {
                    String bluetoothGattInfoKey = bluetoothGattInfoIterator.next().getKey();
                    BluetoothGattInfo bluetoothGattInfoValue = bluetoothGattInfoIterator.next().getValue();
                    if (bleCallbackKey.equals(bluetoothGattInfoKey) && bluetoothGattInfoValue.getDescriptor() == null) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            bleCallbackValue.onSuccess(characteristic.getValue(), bluetoothGattInfoValue);
                        } else {
                            bleCallbackValue.onFailure(new GattException(status));
                        }
                    }
                }
            }
        }

        /**
         * 特征值改变，主要用来接收设备返回的数据信息
         * @param gatt GATT
         * @param characteristic 特征值
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            ViseLog.i("onCharacteristicChanged data:" + HexUtil.encodeHexStr(characteristic.getValue()));
            Iterator<Map.Entry<String, IBleCallback>> bleCallbackIterator = bleCallbackMap.entrySet().iterator();
            Iterator<Map.Entry<String, BluetoothGattInfo>> bluetoothGattInfoIterator = receiveInfoMap.entrySet().iterator();
            while (bleCallbackIterator.hasNext()) {
                String bleCallbackKey = bleCallbackIterator.next().getKey();
                IBleCallback bleCallbackValue = bleCallbackIterator.next().getValue();
                while (bluetoothGattInfoIterator.hasNext()) {
                    String bluetoothGattInfoKey = bluetoothGattInfoIterator.next().getKey();
                    BluetoothGattInfo bluetoothGattInfoValue = bluetoothGattInfoIterator.next().getValue();
                    if (bleCallbackKey.equals(bluetoothGattInfoKey)) {
                        bleCallbackValue.onSuccess(characteristic.getValue(), bluetoothGattInfoValue);
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
            ViseLog.i("onDescriptorRead  status: " + status + ", data:" + HexUtil.encodeHexStr(descriptor.getValue()));
            Iterator<Map.Entry<String, IBleCallback>> bleCallbackIterator = bleCallbackMap.entrySet().iterator();
            Iterator<Map.Entry<String, BluetoothGattInfo>> bluetoothGattInfoIterator = readInfoMap.entrySet().iterator();
            while (bleCallbackIterator.hasNext()) {
                String bleCallbackKey = bleCallbackIterator.next().getKey();
                IBleCallback bleCallbackValue = bleCallbackIterator.next().getValue();
                while (bluetoothGattInfoIterator.hasNext()) {
                    String bluetoothGattInfoKey = bluetoothGattInfoIterator.next().getKey();
                    BluetoothGattInfo bluetoothGattInfoValue = bluetoothGattInfoIterator.next().getValue();
                    if (bleCallbackKey.equals(bluetoothGattInfoKey) && bluetoothGattInfoValue.getDescriptor() != null) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            bleCallbackValue.onSuccess(descriptor.getValue(), bluetoothGattInfoValue);
                        } else {
                            bleCallbackValue.onFailure(new GattException(status));
                        }
                        bleCallbackMap.remove(bleCallbackKey);
                        readInfoMap.remove(bluetoothGattInfoKey);
                    }
                }
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
            ViseLog.i("onDescriptorWrite  status: " + status + ", data:" + HexUtil.encodeHexStr(descriptor.getValue()));
            Iterator<Map.Entry<String, IBleCallback>> bleCallbackIterator = bleCallbackMap.entrySet().iterator();
            Iterator<Map.Entry<String, BluetoothGattInfo>> bluetoothGattInfoIterator = writeInfoMap.entrySet().iterator();
            while (bleCallbackIterator.hasNext()) {
                String bleCallbackKey = bleCallbackIterator.next().getKey();
                IBleCallback bleCallbackValue = bleCallbackIterator.next().getValue();
                while (bluetoothGattInfoIterator.hasNext()) {
                    String bluetoothGattInfoKey = bluetoothGattInfoIterator.next().getKey();
                    BluetoothGattInfo bluetoothGattInfoValue = bluetoothGattInfoIterator.next().getValue();
                    if (bleCallbackKey.equals(bluetoothGattInfoKey) && bluetoothGattInfoValue.getDescriptor() != null) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            bleCallbackValue.onSuccess(descriptor.getValue(), bluetoothGattInfoValue);
                        } else {
                            bleCallbackValue.onFailure(new GattException(status));
                        }
                    }
                }
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
            ViseLog.i("onReadRemoteRssi  status: " + status + ", rssi:" + rssi);
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

    public void withUUID(IBleCallback bleCallback, PropertyType propertyType, String serviceUUID, String characteristicUUID, String descriptorUUID) {
        withUUID(bleCallback, propertyType, formUUID(serviceUUID), formUUID(characteristicUUID), formUUID(descriptorUUID));
    }

    /**
     * 设置需要操作数据的相关信息，包含：数据操作回调，数据操作类型，数据通道建立所需的UUID。
     * @param bleCallback
     * @param propertyType
     * @param serviceUUID
     * @param characteristicUUID
     * @param descriptorUUID
     */
    public void withUUID(IBleCallback bleCallback, PropertyType propertyType, UUID serviceUUID, UUID characteristicUUID, UUID descriptorUUID) {
        if (propertyType != null && serviceUUID != null && characteristicUUID != null) {
            BluetoothGattInfo bluetoothGattInfo = new BluetoothGattInfo(bluetoothGatt, serviceUUID, characteristicUUID, descriptorUUID);
            String key;
            if (descriptorUUID != null) {
                key = serviceUUID.toString() + characteristicUUID.toString() + descriptorUUID.toString();
            } else {
                key = serviceUUID.toString() + characteristicUUID.toString();
            }
            bleCallbackMap.put(key, bleCallback);
            if (propertyType == PropertyType.PROPERTY_READ) {
                readInfoMap.put(key, bluetoothGattInfo);
            } else if (propertyType == PropertyType.PROPERTY_WRITE) {
                writeInfoMap.put(key, bluetoothGattInfo);
            } else if (propertyType == PropertyType.PROPERTY_NOTIFY) {
                receiveInfoMap.put(key, bluetoothGattInfo);
            } else if (propertyType == PropertyType.PROPERTY_INDICATE) {
                receiveInfoMap.put(key, bluetoothGattInfo);
            }
        }
    }

    /**
     * 写入数据
     * @param data
     */
    public void writeData(byte[] data) {
        setWrite(data);
    }

    /**
     * 读取数据
     */
    public void readData() {
        setRead();
    }

    /**
     * 获取设备信号值
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
     * @param propertyType
     */
    public void registerNotifyListener(PropertyType propertyType) {
        if (propertyType == PropertyType.PROPERTY_INDICATE) {
            setNotify(true, true);
        } else if (propertyType == PropertyType.PROPERTY_NOTIFY) {
            setNotify(true, false);
        }
    }

    /**
     * 取消获取数据通知
     * @param propertyType
     */
    public void unregisterNotifyListener(PropertyType propertyType) {
        if (propertyType == PropertyType.PROPERTY_INDICATE) {
            setNotify(false, true);
        } else if (propertyType == PropertyType.PROPERTY_NOTIFY) {
            setNotify(false, true);
        }
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
     * 获取设备连接状态
     * @return 返回设备连接状态
     */
    public ConnectState getConnectState() {
        return connectState;
    }

    /**
     * 设备是否连接
     * @return
     */
    public boolean isConnected() {
        return connectState == ConnectState.CONNECT_SUCCESS;
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

    /**
     * UUID转换
     * @param uuid
     * @return 返回UUID
     */
    private UUID formUUID(String uuid) {
        return uuid == null ? null : UUID.fromString(uuid);
    }

    private synchronized void connect() {
        if (handler != null) {
            handler.sendMessageDelayed(handler.obtainMessage(MSG_CONNECT_TIMEOUT, connectCallback), BleConfig.getInstance().getConnectTimeout());
        }
        connectState = ConnectState.CONNECT_PROCESS;
        if (bluetoothLeDevice != null && bluetoothLeDevice.getDevice() != null) {
            bluetoothLeDevice.getDevice().connectGatt(ViseBle.getInstance().getContext(), false, coreGattCallback);
        }
    }

    private boolean setNotify(boolean enable, boolean isIndication) {
        boolean success = false;
        Iterator<Map.Entry<String, BluetoothGattInfo>> bluetoothGattInfoIterator = receiveInfoMap.entrySet().iterator();
        while (bluetoothGattInfoIterator.hasNext()) {
            String bluetoothGattInfoKey = bluetoothGattInfoIterator.next().getKey();
            BluetoothGattInfo bluetoothGattInfoValue = bluetoothGattInfoIterator.next().getValue();
            if (bluetoothGatt != null && bluetoothGattInfoValue.getCharacteristic() != null) {
                success = bluetoothGatt.setCharacteristicNotification(bluetoothGattInfoValue.getCharacteristic(), enable);
            }
            BluetoothGattDescriptor bluetoothGattDescriptor;
            if (bluetoothGattInfoValue.getCharacteristic() != null && bluetoothGattInfoValue.getDescriptor() != null) {
                bluetoothGattDescriptor = bluetoothGattInfoValue.getDescriptor();
            } else if (bluetoothGattInfoValue.getCharacteristic() != null && bluetoothGattInfoValue.getDescriptor() == null) {
                bluetoothGattDescriptor = bluetoothGattInfoValue.getCharacteristic().getDescriptor(UUID.fromString(BleConstant.CLIENT_CHARACTERISTIC_CONFIG));
            }
            ViseLog.i("set notify value: " + HexUtil.encodeHexStr(bluetoothGattInfoValue.getDescriptor().getValue()));
            if (isIndication) {
                if (enable) {
                    bluetoothGattInfoValue.getDescriptor().setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                } else {
                    bluetoothGattInfoValue.getDescriptor().setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                }
            } else {
                if (enable) {
                    bluetoothGattInfoValue.getDescriptor().setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                } else {
                    bluetoothGattInfoValue.getDescriptor().setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                }
            }
            if (bluetoothGatt != null) {
                bluetoothGatt.writeDescriptor(bluetoothGattInfoValue.getDescriptor());
            }
        }
        return success;
    }

    private boolean setRead() {
        boolean success = false;
        Iterator<Map.Entry<String, BluetoothGattInfo>> bluetoothGattInfoIterator = readInfoMap.entrySet().iterator();
        while (bluetoothGattInfoIterator.hasNext()) {
            String bluetoothGattInfoKey = bluetoothGattInfoIterator.next().getKey();
            BluetoothGattInfo bluetoothGattInfoValue = bluetoothGattInfoIterator.next().getValue();
            if (bluetoothGatt != null && bluetoothGattInfoValue.getCharacteristic() != null && bluetoothGattInfoValue.getDescriptor() != null) {
                success = bluetoothGatt.readDescriptor(bluetoothGattInfoValue.getDescriptor());
                ViseLog.i("set read descriptor value: " + HexUtil.encodeHexStr(bluetoothGattInfoValue.getDescriptor().getValue()));
            } else if (bluetoothGatt != null && bluetoothGattInfoValue.getCharacteristic() != null && bluetoothGattInfoValue.getDescriptor() == null) {
                success = bluetoothGatt.readCharacteristic(bluetoothGattInfoValue.getCharacteristic());
                ViseLog.i("set read characteristic value: " + HexUtil.encodeHexStr(bluetoothGattInfoValue.getDescriptor().getValue()));
            }
        }
        return success;
    }

    private boolean setWrite(byte[] data) {
        boolean success = false;
        Iterator<Map.Entry<String, BluetoothGattInfo>> bluetoothGattInfoIterator = receiveInfoMap.entrySet().iterator();
        while (bluetoothGattInfoIterator.hasNext()) {
            String bluetoothGattInfoKey = bluetoothGattInfoIterator.next().getKey();
            BluetoothGattInfo bluetoothGattInfoValue = bluetoothGattInfoIterator.next().getValue();
            if (bluetoothGatt != null && bluetoothGattInfoValue.getCharacteristic() != null && bluetoothGattInfoValue.getDescriptor() != null) {
                bluetoothGattInfoValue.getDescriptor().setValue(data);
                success = bluetoothGatt.writeDescriptor(bluetoothGattInfoValue.getDescriptor());
                ViseLog.i("set write descriptor value: " + HexUtil.encodeHexStr(bluetoothGattInfoValue.getDescriptor().getValue()));
            } else if (bluetoothGatt != null && bluetoothGattInfoValue.getCharacteristic() != null && bluetoothGattInfoValue.getDescriptor() == null) {
                bluetoothGattInfoValue.getCharacteristic().setValue(data);
                success = bluetoothGatt.writeCharacteristic(bluetoothGattInfoValue.getCharacteristic());
                ViseLog.i("set write characteristic value: " + HexUtil.encodeHexStr(bluetoothGattInfoValue.getDescriptor().getValue()));
            }
        }
        return success;
    }

}
