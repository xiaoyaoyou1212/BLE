package com.vise.bledemo.common;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.vise.baseble.ViseBle;
import com.vise.baseble.callback.IBleCallback;
import com.vise.baseble.callback.IConnectCallback;
import com.vise.baseble.common.PropertyType;
import com.vise.baseble.core.BluetoothGattChannel;
import com.vise.baseble.core.DeviceMirror;
import com.vise.baseble.core.DeviceMirrorPool;
import com.vise.baseble.exception.BleException;
import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.baseble.utils.HexUtil;
import com.vise.bledemo.event.CallbackDataEvent;
import com.vise.bledemo.event.ConnectEvent;
import com.vise.bledemo.event.NotifyDataEvent;
import com.vise.bledemo.event.ScanEvent;
import com.vise.log.ViseLog;
import com.vise.xsnow.event.BusManager;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

/**
 * @Description: 蓝牙设备管理
 * @author: <a href="http://xiaoyaoyou1212.360doc.com">DAWI</a>
 * @date: 2017/10/27 17:09
 */
public class BluetoothDeviceManager {

    private static BluetoothDeviceManager instance;
    private DeviceMirrorPool mDeviceMirrorPool;
    private ScanEvent scanEvent = new ScanEvent();
    private ConnectEvent connectEvent = new ConnectEvent();
    private CallbackDataEvent callbackDataEvent = new CallbackDataEvent();
    private NotifyDataEvent notifyDataEvent = new NotifyDataEvent();

    /**
     * 连接回调
     */
    private IConnectCallback connectCallback = new IConnectCallback() {

        @Override
        public void onConnectSuccess(final DeviceMirror deviceMirror) {
            ViseLog.i("Connect Success!");
            BusManager.getBus().post(connectEvent.setDeviceMirror(deviceMirror).setSuccess(true));
        }

        @Override
        public void onConnectFailure(BleException exception) {
            ViseLog.i("Connect Failure!");
            BusManager.getBus().post(connectEvent.setSuccess(false).setDisconnected(false));
        }

        @Override
        public void onDisconnect(boolean isActive) {
            ViseLog.i("Disconnect!");
            BusManager.getBus().post(connectEvent.setSuccess(false).setDisconnected(true));
        }
    };

    /**
     * 接收数据回调
     */
    private IBleCallback receiveCallback = new IBleCallback() {
        @Override
        public void onSuccess(final byte[] data, BluetoothGattChannel bluetoothGattInfo, BluetoothLeDevice bluetoothLeDevice) {
            if (data == null) {
                return;
            }
            ViseLog.i("notify success:" + HexUtil.encodeHexStr(data));
            BusManager.getBus().post(notifyDataEvent.setData(data)
                    .setBluetoothLeDevice(bluetoothLeDevice)
                    .setBluetoothGattChannel(bluetoothGattInfo));
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
     * 操作数据回调
     */
    private IBleCallback bleCallback = new IBleCallback() {
        @Override
        public void onSuccess(final byte[] data, BluetoothGattChannel bluetoothGattInfo, BluetoothLeDevice bluetoothLeDevice) {
            if (data == null) {
                return;
            }
            ViseLog.i("callback success:" + HexUtil.encodeHexStr(data));
            BusManager.getBus().post(callbackDataEvent.setData(data).setSuccess(true)
                    .setBluetoothLeDevice(bluetoothLeDevice)
                    .setBluetoothGattChannel(bluetoothGattInfo));
            if (bluetoothGattInfo != null && (bluetoothGattInfo.getPropertyType() == PropertyType.PROPERTY_INDICATE
                    || bluetoothGattInfo.getPropertyType() == PropertyType.PROPERTY_NOTIFY)) {
                DeviceMirror deviceMirror = mDeviceMirrorPool.getDeviceMirror(bluetoothLeDevice);
                if (deviceMirror != null) {
                    deviceMirror.setNotifyListener(bluetoothGattInfo.getGattInfoKey(), receiveCallback);
                }
            }
        }

        @Override
        public void onFailure(BleException exception) {
            if (exception == null) {
                return;
            }
            ViseLog.i("callback fail:" + exception.getDescription());
            BusManager.getBus().post(callbackDataEvent.setSuccess(false));
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
                .setScanTimeout(-1)//扫描超时时间，这里设置为永久扫描
                .setScanRepeatInterval(5 * 1000)//扫描间隔5秒
                .setConnectTimeout(10 * 1000)//连接超时时间
                .setOperateTimeout(5 * 1000)//设置数据操作超时时间
                .setConnectRetryCount(3)//设置连接失败重试次数
                .setConnectRetryInterval(1000)//设置连接失败重试间隔时间
                .setOperateRetryCount(3)//设置数据操作失败重试次数
                .setOperateRetryInterval(1000)//设置数据操作失败重试间隔时间
                .setMaxConnectCount(3);//设置最大连接设备数量
        //蓝牙信息初始化，全局唯一，必须在应用初始化时调用
        ViseBle.getInstance().init(context.getApplicationContext());
        mDeviceMirrorPool = ViseBle.getInstance().getDeviceMirrorPool();
    }

    public void connect(BluetoothLeDevice bluetoothLeDevice) {
        ViseBle.getInstance().connect(bluetoothLeDevice, connectCallback);
    }

    public void disconnect(BluetoothLeDevice bluetoothLeDevice) {
        ViseBle.getInstance().disconnect(bluetoothLeDevice);
    }

    public boolean isConnected(BluetoothLeDevice bluetoothLeDevice) {
        return ViseBle.getInstance().isConnect(bluetoothLeDevice);
    }

    public void bindChannel(BluetoothLeDevice bluetoothLeDevice, PropertyType propertyType, UUID serviceUUID,
                                 UUID characteristicUUID, UUID descriptorUUID) {
        DeviceMirror deviceMirror = mDeviceMirrorPool.getDeviceMirror(bluetoothLeDevice);
        if (deviceMirror != null) {
            BluetoothGattChannel bluetoothGattChannel = new BluetoothGattChannel.Builder()
                    .setBluetoothGatt(deviceMirror.getBluetoothGatt())
                    .setPropertyType(propertyType)
                    .setServiceUUID(serviceUUID)
                    .setCharacteristicUUID(characteristicUUID)
                    .setDescriptorUUID(descriptorUUID)
                    .builder();
            deviceMirror.bindChannel(bleCallback, bluetoothGattChannel);
        }
    }

    public void write(final BluetoothLeDevice bluetoothLeDevice, byte[] data) {
        if (dataInfoQueue != null) {
            dataInfoQueue.clear();
            dataInfoQueue = splitPacketFor20Byte(data);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    send(bluetoothLeDevice);
                }
            });
        }
    }

    public void read(BluetoothLeDevice bluetoothLeDevice) {
        DeviceMirror deviceMirror = mDeviceMirrorPool.getDeviceMirror(bluetoothLeDevice);
        if (deviceMirror != null) {
            deviceMirror.readData();
        }
    }

    public void registerNotify(BluetoothLeDevice bluetoothLeDevice, boolean isIndicate) {
        DeviceMirror deviceMirror = mDeviceMirrorPool.getDeviceMirror(bluetoothLeDevice);
        if (deviceMirror != null) {
            deviceMirror.registerNotify(isIndicate);
        }
    }

    //发送队列，提供一种简单的处理方式，实际项目场景需要根据需求优化
    private Queue<byte[]> dataInfoQueue = new LinkedList<>();
    private void send(final BluetoothLeDevice bluetoothLeDevice) {
        if (dataInfoQueue != null && !dataInfoQueue.isEmpty()) {
            DeviceMirror deviceMirror = mDeviceMirrorPool.getDeviceMirror(bluetoothLeDevice);
            if (dataInfoQueue.peek() != null && deviceMirror != null) {
                deviceMirror.writeData(dataInfoQueue.poll());
            }
            if (dataInfoQueue.peek() != null) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        send(bluetoothLeDevice);
                    }
                }, 100);
            }
        }
    }

    /**
     * 数据分包
     *
     * @param data
     * @return
     */
    private Queue<byte[]> splitPacketFor20Byte(byte[] data) {
        Queue<byte[]> dataInfoQueue = new LinkedList<>();
        if (data != null) {
            int index = 0;
            do {
                byte[] surplusData = new byte[data.length - index];
                byte[] currentData;
                System.arraycopy(data, index, surplusData, 0, data.length - index);
                if (surplusData.length <= 20) {
                    currentData = new byte[surplusData.length];
                    System.arraycopy(surplusData, 0, currentData, 0, surplusData.length);
                    index += surplusData.length;
                } else {
                    currentData = new byte[20];
                    System.arraycopy(data, index, currentData, 0, 20);
                    index += 20;
                }
                dataInfoQueue.offer(currentData);
            } while (index < data.length);
        }
        return dataInfoQueue;
    }

}
