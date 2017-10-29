package com.vise.bledemo.common;

import android.content.Context;

import com.vise.baseble.ViseBle;
import com.vise.baseble.callback.IBleCallback;
import com.vise.baseble.callback.IConnectCallback;
import com.vise.baseble.callback.scan.IScanCallback;
import com.vise.baseble.callback.scan.ScanCallback;
import com.vise.baseble.common.BleConstant;
import com.vise.baseble.common.PropertyType;
import com.vise.baseble.core.BluetoothGattChannel;
import com.vise.baseble.core.DeviceMirror;
import com.vise.baseble.core.DeviceMirrorPool;
import com.vise.baseble.exception.BleException;
import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.baseble.model.BluetoothLeDeviceStore;
import com.vise.baseble.utils.HexUtil;
import com.vise.bledemo.event.CallbackDataEvent;
import com.vise.bledemo.event.ConnectEvent;
import com.vise.bledemo.event.NotifyDataEvent;
import com.vise.bledemo.event.ScanEvent;
import com.vise.log.ViseLog;
import com.vise.xsnow.event.BusManager;

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
     * 扫描回调
     */
    private ScanCallback periodScanCallback = new ScanCallback(new IScanCallback() {
        @Override
        public void onDeviceFound(BluetoothLeDeviceStore bluetoothLeDeviceStore) {
            ViseLog.i("Founded Scan Device:" + bluetoothLeDeviceStore);
            BusManager.getBus().post(scanEvent.setBluetoothLeDeviceStore(bluetoothLeDeviceStore));
        }

        @Override
        public void onScanFinish(BluetoothLeDeviceStore bluetoothLeDeviceStore) {
            ViseLog.i("scan finish " + bluetoothLeDeviceStore);
            BusManager.getBus().post(scanEvent.setBluetoothLeDeviceStore(bluetoothLeDeviceStore).setScanFinish(true));
        }

        @Override
        public void onScanTimeout() {
            ViseLog.i("scan timeout");
            BusManager.getBus().post(scanEvent.setScanTimeout(true));
        }
    });

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
            BusManager.getBus().post(connectEvent.setSuccess(false));
        }

        @Override
        public void onDisconnect(boolean isActive) {
            ViseLog.i("Disconnect!");
            BusManager.getBus().post(connectEvent.setDisconnected(true));
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
            BusManager.getBus().post(notifyDataEvent.setData(data).setBluetoothGattChannel(bluetoothGattInfo));
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
        public void onSuccess(final byte[] data, BluetoothGattChannel bluetoothGattInfo) {
            if (data == null) {
                return;
            }
            ViseLog.i("callback success:" + HexUtil.encodeHexStr(data));
            BusManager.getBus().post(callbackDataEvent.setData(data).setSuccess(true)
                    .setBluetoothGattChannel(bluetoothGattInfo));
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
                .setScanTimeout(BleConstant.TIME_FOREVER)
                .setMaxConnectCount(3);
        //蓝牙信息初始化，全局唯一，必须在应用初始化时调用
        ViseBle.getInstance().init(context.getApplicationContext());
        mDeviceMirrorPool = ViseBle.getInstance().getDeviceMirrorPool();
    }

    public void startScan() {
        ViseBle.getInstance().startScan(periodScanCallback);
    }

    public void stopScan() {
        ViseBle.getInstance().stopScan(periodScanCallback);
    }

    public void connect(BluetoothLeDevice bluetoothLeDevice) {
        ViseBle.getInstance().connect(bluetoothLeDevice, connectCallback);
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

    public void write(BluetoothLeDevice bluetoothLeDevice, byte[] data) {
        DeviceMirror deviceMirror = mDeviceMirrorPool.getDeviceMirror(bluetoothLeDevice);
        if (deviceMirror != null) {
            deviceMirror.writeData(data);
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

}
