package com.vise.baseble;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.vise.baseble.callback.IConnectCallback;
import com.vise.baseble.callback.data.IBleCallback;
import com.vise.baseble.callback.data.ICharacteristicCallback;
import com.vise.baseble.callback.data.IDescriptorCallback;
import com.vise.baseble.callback.data.IRssiCallback;
import com.vise.baseble.callback.scan.PeriodLScanCallback;
import com.vise.baseble.callback.scan.PeriodMacScanCallback;
import com.vise.baseble.callback.scan.PeriodNameScanCallback;
import com.vise.baseble.callback.scan.PeriodScanCallback;
import com.vise.baseble.common.BleConstant;
import com.vise.baseble.common.State;
import com.vise.baseble.exception.ConnectException;
import com.vise.baseble.exception.GattException;
import com.vise.baseble.exception.InitiatedException;
import com.vise.baseble.exception.OtherException;
import com.vise.baseble.exception.TimeoutException;
import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.baseble.utils.HexUtil;
import com.vise.log.ViseLog;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.vise.baseble.common.BleConstant.DEFAULT_CONN_TIME;
import static com.vise.baseble.common.BleConstant.DEFAULT_OPERATE_TIME;
import static com.vise.baseble.common.BleConstant.DEFAULT_SCAN_TIME;
import static com.vise.baseble.common.BleConstant.MSG_CONNECT_TIMEOUT;
import static com.vise.baseble.common.BleConstant.MSG_READ_CHA;
import static com.vise.baseble.common.BleConstant.MSG_READ_DES;
import static com.vise.baseble.common.BleConstant.MSG_READ_RSSI;
import static com.vise.baseble.common.BleConstant.MSG_WRITE_CHA;
import static com.vise.baseble.common.BleConstant.MSG_WRITE_DES;

/**
 * @Description: Bluetooth操作类
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/8/5 20:42.
 */
public class ViseBluetooth {

    private Context context;//上下文
    private BluetoothManager bluetoothManager;//蓝牙管理
    private BluetoothAdapter bluetoothAdapter;//蓝牙适配器
    private BluetoothGatt bluetoothGatt;//蓝牙GATT
    private BluetoothGattService service;//GATT服务
    private BluetoothGattCharacteristic characteristic;//GATT特征值
    private BluetoothGattDescriptor descriptor;//GATT属性描述
    private IConnectCallback connectCallback;//连接回调
    private ICharacteristicCallback receiveCallback;//接收数据回调
    private IBleCallback tempBleCallback;//存储操作回调，方便取消管理
    private volatile Set<IBleCallback> bleCallbacks = new LinkedHashSet<>();//操作回调集合
    private State state = State.DISCONNECT;//设备状态描述
    private int scanTimeout = DEFAULT_SCAN_TIME;//扫描超时时间
    private int connectTimeout = DEFAULT_CONN_TIME;//连接超时时间
    private int operateTimeout = DEFAULT_OPERATE_TIME;//数据操作超时时间
    private boolean isFound = false;//是否发现设备

    private static ViseBluetooth viseBluetooth;//入口操作管理

    /**
     * 单例方式获取蓝牙通信入口
     *
     * @return 返回ViseBluetooth
     */
    public static ViseBluetooth getInstance() {
        if (viseBluetooth == null) {
            synchronized (ViseBluetooth.class) {
                if (viseBluetooth == null) {
                    viseBluetooth = new ViseBluetooth();
                }
            }
        }
        return viseBluetooth;
    }

    /**
     * handler处理连接超时和操作异常超时
     */
    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_CONNECT_TIMEOUT) {
                IConnectCallback connectCallback = (IConnectCallback) msg.obj;
                if (connectCallback != null && state != State.CONNECT_SUCCESS) {
                    close();
                    state = State.CONNECT_TIMEOUT;
                    connectCallback.onConnectFailure(new TimeoutException());
                }
            } else {
                IBleCallback bleCallback = (IBleCallback) msg.obj;
                if (bleCallback != null) {
                    bleCallback.onFailure(new TimeoutException());
                    removeBleCallback(bleCallback);
                }
            }
            msg.obj = null;
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
                state = State.DISCONNECT;
                if (handler != null) {
                    handler.removeMessages(MSG_CONNECT_TIMEOUT);
                }
                if (connectCallback != null) {
                    close();
                    runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            if (status == 0) {
                                connectCallback.onDisconnect();
                            } else {
                                connectCallback.onConnectFailure(new ConnectException(gatt, status));
                            }
                        }
                    });
                }
            } else if (newState == BluetoothGatt.STATE_CONNECTING) {
                state = State.CONNECT_PROCESS;
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
                state = State.CONNECT_SUCCESS;
                if (connectCallback != null) {
                    runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            connectCallback.onConnectSuccess(gatt, status);
                        }
                    });
                }
            } else {
                state = State.CONNECT_FAILURE;
                if (connectCallback != null) {
                    close();
                    runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            connectCallback.onConnectFailure(new ConnectException(gatt, status));
                        }
                    });
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
            if (bleCallbacks == null) {
                return;
            }
            if (handler != null) {
                handler.removeMessages(MSG_READ_CHA);
            }
            runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    for (IBleCallback bleCallback : bleCallbacks) {
                        if (bleCallback instanceof ICharacteristicCallback) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                ((ICharacteristicCallback) bleCallback).onSuccess(characteristic);
                            } else {
                                bleCallback.onFailure(new GattException(status));
                            }
                        }
                    }
                    removeBleCallback(tempBleCallback);
                }
            });
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
            if (bleCallbacks == null) {
                return;
            }
            if (handler != null) {
                handler.removeMessages(MSG_WRITE_CHA);
            }
            runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    for (IBleCallback bleCallback : bleCallbacks) {
                        if (bleCallback instanceof ICharacteristicCallback) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                ((ICharacteristicCallback) bleCallback).onSuccess(characteristic);
                            } else {
                                bleCallback.onFailure(new GattException(status));
                            }
                        }
                    }
                    removeBleCallback(tempBleCallback);
                }
            });
        }

        /**
         * 特征值改变，主要用来接收设备返回的数据信息
         * @param gatt GATT
         * @param characteristic 特征值
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            ViseLog.i("onCharacteristicChanged data:" + HexUtil.encodeHexStr(characteristic.getValue()));
            runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    if (receiveCallback != null) {
                        receiveCallback.onSuccess(characteristic);
                    }
                }
            });
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
            if (bleCallbacks == null) {
                return;
            }
            if (handler != null) {
                handler.removeMessages(MSG_READ_DES);
            }
            runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    for (IBleCallback bleCallback : bleCallbacks) {
                        if (bleCallback instanceof IDescriptorCallback) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                ((IDescriptorCallback) bleCallback).onSuccess(descriptor);
                            } else {
                                bleCallback.onFailure(new GattException(status));
                            }
                        }
                    }
                    removeBleCallback(tempBleCallback);
                }
            });
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
            if (bleCallbacks == null) {
                return;
            }
            if (handler != null) {
                handler.removeMessages(MSG_WRITE_DES);
            }
            runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    for (IBleCallback bleCallback : bleCallbacks) {
                        if (bleCallback instanceof IDescriptorCallback) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                ((IDescriptorCallback) bleCallback).onSuccess(descriptor);
                            } else {
                                bleCallback.onFailure(new GattException(status));
                            }
                        }
                    }
                    removeBleCallback(tempBleCallback);
                }
            });
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
            if (bleCallbacks == null) {
                return;
            }
            if (handler != null) {
                handler.removeMessages(MSG_READ_RSSI);
            }
            runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    for (IBleCallback bleCallback : bleCallbacks) {
                        if (bleCallback instanceof IRssiCallback) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                ((IRssiCallback) bleCallback).onSuccess(rssi);
                            } else {
                                bleCallback.onFailure(new GattException(status));
                            }
                        }
                    }
                    removeBleCallback(tempBleCallback);
                }
            });
        }
    };

    private ViseBluetooth() {
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

    /*==================Android API 18 Scan========================*/

    /**
     * 开始扫描
     * @param leScanCallback 回调
     */
    public void startLeScan(BluetoothAdapter.LeScanCallback leScanCallback) {
        if (bluetoothAdapter != null) {
            bluetoothAdapter.startLeScan(leScanCallback);
            state = State.SCAN_PROCESS;
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
    public void startScan(PeriodScanCallback periodScanCallback) {
        if (periodScanCallback == null) {
            throw new IllegalArgumentException("this PeriodScanCallback is Null!");
        }
        periodScanCallback.setViseBluetooth(this).setScan(true).setScanTimeout(scanTimeout).scan();
    }

    /**
     * 停止扫描
     * @param periodScanCallback 自定义回调
     */
    public void stopScan(PeriodScanCallback periodScanCallback) {
        if (periodScanCallback == null) {
            throw new IllegalArgumentException("this PeriodScanCallback is Null!");
        }
        periodScanCallback.setViseBluetooth(this).setScan(false).removeHandlerMsg().scan();
    }

    /*==================Android API 21 Scan========================*/

    /**
     * 开始扫描
     * @param leScanCallback 回调
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void startLeScan(ScanCallback leScanCallback) {
        if (bluetoothAdapter != null && bluetoothAdapter.getBluetoothLeScanner() != null) {
            bluetoothAdapter.getBluetoothLeScanner().startScan(leScanCallback);
            state = State.SCAN_PROCESS;
        }
    }

    /**
     * 开始扫描
     * @param filters 过滤条件
     * @param settings 设置
     * @param leScanCallback 回调
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void startLeScan(List<ScanFilter> filters, ScanSettings settings, ScanCallback leScanCallback) {
        if (bluetoothAdapter != null && bluetoothAdapter.getBluetoothLeScanner() != null) {
            bluetoothAdapter.getBluetoothLeScanner().startScan(filters, settings, leScanCallback);
            state = State.SCAN_PROCESS;
        }
    }

    /**
     * 停止扫描
     * @param leScanCallback 回调
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void stopLeScan(ScanCallback leScanCallback) {
        if (bluetoothAdapter != null && bluetoothAdapter.getBluetoothLeScanner() != null) {
            bluetoothAdapter.getBluetoothLeScanner().stopScan(leScanCallback);
        }
    }

    /**
     * 开始扫描
     * @param periodScanCallback 自定义回调
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void startScan(PeriodLScanCallback periodScanCallback) {
        if (periodScanCallback == null) {
            throw new IllegalArgumentException("this PeriodScanCallback is Null!");
        }
        periodScanCallback.setViseBluetooth(this).setScan(true).setScanTimeout(scanTimeout).scan();
    }

    /**
     * 开始扫描
     * @param filters 过滤条件
     * @param settings 设置
     * @param periodScanCallback 自定义回调
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void startScan(List<ScanFilter> filters, ScanSettings settings, PeriodLScanCallback periodScanCallback) {
        if (periodScanCallback == null) {
            throw new IllegalArgumentException("this PeriodScanCallback is Null!");
        }
        periodScanCallback.setViseBluetooth(this).setScan(true).setScanTimeout(scanTimeout).setFilters(filters).setSettings(settings)
                .scan();
    }

    /**
     * 停止扫描
     * @param periodScanCallback 自定义回调
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void stopScan(PeriodLScanCallback periodScanCallback) {
        if (periodScanCallback == null) {
            throw new IllegalArgumentException("this PeriodScanCallback is Null!");
        }
        periodScanCallback.setViseBluetooth(this).setScan(false).removeHandlerMsg().scan();
    }

    /*==================connect========================*/

    /**
     * 连接设备
     * @param bluetoothDevice 设备信息
     * @param autoConnect 是否自动连接
     * @param connectCallback 连接回调
     * @return GATT
     */
    public synchronized BluetoothGatt connect(BluetoothDevice bluetoothDevice, boolean autoConnect, IConnectCallback connectCallback) {
        if (bluetoothDevice == null || connectCallback == null) {
            throw new IllegalArgumentException("this BluetoothDevice or IConnectCallback is Null!");
        }
        if (handler != null) {
            Message msg = handler.obtainMessage(MSG_CONNECT_TIMEOUT, connectCallback);
            handler.sendMessageDelayed(msg, connectTimeout);
        }
        this.connectCallback = connectCallback;
        state = State.CONNECT_PROCESS;
        return bluetoothDevice.connectGatt(this.context, autoConnect, coreGattCallback);
    }

    /**
     * 连接设备
     * @param bluetoothLeDevice 自定义设备信息
     * @param autoConnect 是否自动连接
     * @param connectCallback 连接回调
     */
    public void connect(BluetoothLeDevice bluetoothLeDevice, boolean autoConnect, IConnectCallback connectCallback) {
        if (bluetoothLeDevice == null) {
            throw new IllegalArgumentException("this BluetoothLeDevice is Null!");
        }
        connect(bluetoothLeDevice.getDevice(), autoConnect, connectCallback);
    }

    /**
     * 连接指定名称的设备
     * @param name 设备名称
     * @param autoConnect 是否自动连接
     * @param connectCallback 连接回调
     */
    public void connectByName(String name, final boolean autoConnect, final IConnectCallback connectCallback) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Illegal Name!");
        }
        isFound = false;
        startScan(new PeriodNameScanCallback(name) {
            @Override
            public void onDeviceFound(final BluetoothLeDevice bluetoothLeDevice) {
                isFound = true;
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        connect(bluetoothLeDevice, autoConnect, connectCallback);
                    }
                });
            }

            @Override
            public void scanTimeout() {
                if (isFound) {
                    isFound = false;
                    return;
                }
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (connectCallback != null) {
                            connectCallback.onConnectFailure(new TimeoutException());
                        }
                    }
                });
            }
        });
    }

    /**
     * 连接指定Mac的设备
     * @param mac 设备Mac
     * @param autoConnect 是否自动连接
     * @param connectCallback 连接回调
     */
    public void connectByMac(String mac, final boolean autoConnect, final IConnectCallback connectCallback) {
        if (mac == null || mac.split(":").length != 6) {
            throw new IllegalArgumentException("Illegal MAC!");
        }
        isFound = false;
        startScan(new PeriodMacScanCallback(mac) {
            @Override
            public void onDeviceFound(final BluetoothLeDevice bluetoothLeDevice) {
                isFound = true;
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        connect(bluetoothLeDevice, autoConnect, connectCallback);
                    }
                });
            }

            @Override
            public void scanTimeout() {
                if (isFound) {
                    isFound = false;
                    return;
                }
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (connectCallback != null) {
                            connectCallback.onConnectFailure(new TimeoutException());
                        }
                    }
                });
            }
        });
    }

    /**
     * 连接指定名称的设备
     * @param name 设备名称
     * @param autoConnect 是否自动连接
     * @param connectCallback 连接回调
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void connectByLName(String name, final boolean autoConnect, final IConnectCallback connectCallback) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Illegal Name!");
        }
        isFound = false;
        List<ScanFilter> bleScanFilters = new ArrayList<>();
        bleScanFilters.add(new ScanFilter.Builder().setDeviceName(name).build());
        startScan(bleScanFilters, new ScanSettings.Builder().build(), new PeriodLScanCallback() {
            @Override
            public void onDeviceFound(final BluetoothLeDevice bluetoothLeDevice) {
                isFound = true;
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        connect(bluetoothLeDevice, autoConnect, connectCallback);
                    }
                });
            }

            @Override
            public void scanTimeout() {
                if (isFound) {
                    isFound = false;
                    return;
                }
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (connectCallback != null) {
                            connectCallback.onConnectFailure(new TimeoutException());
                        }
                    }
                });
            }
        });
    }

    /**
     * 连接指定Mac的设备
     * @param mac 设备Mac
     * @param autoConnect 是否自动连接
     * @param connectCallback 连接回调
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void connectByLMac(String mac, final boolean autoConnect, final IConnectCallback connectCallback) {
        if (mac == null || mac.split(":").length != 6) {
            throw new IllegalArgumentException("Illegal MAC!");
        }
        isFound = false;
        List<ScanFilter> bleScanFilters = new ArrayList<>();
        bleScanFilters.add(new ScanFilter.Builder().setDeviceAddress(mac).build());
        startScan(bleScanFilters, new ScanSettings.Builder().build(), new PeriodLScanCallback() {
            @Override
            public void onDeviceFound(final BluetoothLeDevice bluetoothLeDevice) {
                isFound = true;
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        connect(bluetoothLeDevice, autoConnect, connectCallback);
                    }
                });
            }

            @Override
            public void scanTimeout() {
                if (isFound) {
                    isFound = false;
                    return;
                }
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (connectCallback != null) {
                            connectCallback.onConnectFailure(new TimeoutException());
                        }
                    }
                });
            }
        });
    }

    /*=================main operate========================*/

    /**
     * 设置UUID，这里设置完后面就不用传characteristic和descriptor
     * @param serviceUUID 服务UUID
     * @param characteristicUUID 特征值UUID
     * @param descriptorUUID 属性描述UUID
     * @return 返回ViseBluetooth
     */
    public ViseBluetooth withUUID(UUID serviceUUID, UUID characteristicUUID, UUID descriptorUUID) {
        if (serviceUUID != null && bluetoothGatt != null) {
            service = bluetoothGatt.getService(serviceUUID);
        }
        if (service != null && characteristicUUID != null) {
            characteristic = service.getCharacteristic(characteristicUUID);
        }
        if (characteristic != null && descriptorUUID != null) {
            descriptor = characteristic.getDescriptor(descriptorUUID);
        }
        return this;
    }

    /**
     * 设置UUID，传入字符串形式，这里设置完后面就不用传characteristic和descriptor
     * @param serviceUUID 服务UUID
     * @param characteristicUUID 特征值UUID
     * @param descriptorUUID 属性描述UUID
     * @return 返回ViseBluetooth
     */
    public ViseBluetooth withUUIDString(String serviceUUID, String characteristicUUID, String descriptorUUID) {
        return withUUID(formUUID(serviceUUID), formUUID(characteristicUUID), formUUID(descriptorUUID));
    }

    /**
     * UUID转换
     * @param uuid
     * @return 返回UUID
     */
    private UUID formUUID(String uuid) {
        return uuid == null ? null : UUID.fromString(uuid);
    }

    /**
     * 写入特征值数据
     * @param data 待发送的数据
     * @param bleCallback 发送回调
     * @return 返回写入是否成功的状态
     */
    public boolean writeCharacteristic(byte[] data, ICharacteristicCallback bleCallback) {
        return writeCharacteristic(getCharacteristic(), data, bleCallback);
    }

    /**
     * 写入特征值数据
     * @param characteristic 特征值
     * @param data 待发送的数据
     * @param bleCallback 发送回调
     * @return 返回写入是否成功的状态
     */
    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] data, final ICharacteristicCallback bleCallback) {
        if (characteristic == null) {
            if (bleCallback != null) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        bleCallback.onFailure(new OtherException("this characteristic is null!"));
                        removeBleCallback(bleCallback);
                    }
                });
            }
            return false;
        }
        ViseLog.i(characteristic.getUuid() + " characteristic write bytes: " + Arrays.toString(data) + " ,hex: " + HexUtil.encodeHexStr
                (data));
        listenAndTimer(bleCallback, MSG_WRITE_CHA);
        characteristic.setValue(data);
        return handleAfterInitialed(getBluetoothGatt().writeCharacteristic(characteristic), bleCallback);
    }

    /**
     * 写入属性描述值
     * @param data 待发送数据
     * @param bleCallback 写入回调
     * @return 返回写入是否成功的状态
     */
    public boolean writeDescriptor(byte[] data, IDescriptorCallback bleCallback) {
        return writeDescriptor(getDescriptor(), data, bleCallback);
    }

    /**
     * 写入属性描述值
     * @param descriptor 属性描述值
     * @param data 待发送数据
     * @param bleCallback 写入回调
     * @return 返回写入是否成功的状态
     */
    public boolean writeDescriptor(BluetoothGattDescriptor descriptor, byte[] data, final IDescriptorCallback bleCallback) {
        if (descriptor == null) {
            if (bleCallback != null) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        bleCallback.onFailure(new OtherException("this descriptor is null!"));
                        removeBleCallback(bleCallback);
                    }
                });
            }
            return false;
        }
        ViseLog.i(descriptor.getUuid() + " descriptor write bytes: " + Arrays.toString(data) + " ,hex: " + HexUtil.encodeHexStr(data));
        listenAndTimer(bleCallback, MSG_WRITE_DES);
        descriptor.setValue(data);
        return handleAfterInitialed(getBluetoothGatt().writeDescriptor(descriptor), bleCallback);
    }

    /**
     * 读取特征值
     * @param bleCallback 读取回调
     * @return 返回读取是否成功的状态
     */
    public boolean readCharacteristic(ICharacteristicCallback bleCallback) {
        return readCharacteristic(getCharacteristic(), bleCallback);
    }

    /**
     * 读取特征值
     * @param characteristic 特征值
     * @param bleCallback 读取回调
     * @return 返回写入是否成功的状态
     */
    public boolean readCharacteristic(BluetoothGattCharacteristic characteristic, final ICharacteristicCallback bleCallback) {
        if (characteristic != null && (characteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            setCharacteristicNotification(getBluetoothGatt(), characteristic, false, false);
            listenAndTimer(bleCallback, MSG_READ_CHA);
            return handleAfterInitialed(getBluetoothGatt().readCharacteristic(characteristic), bleCallback);
        } else {
            if (bleCallback != null) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        bleCallback.onFailure(new OtherException("Characteristic [is not] readable!"));
                        removeBleCallback(bleCallback);
                    }
                });
            }
            return false;
        }
    }

    /**
     * 读取属性描述值
     * @param bleCallback 读取回调
     * @return 返回读取是否成功的状态
     */
    public boolean readDescriptor(IDescriptorCallback bleCallback) {
        return readDescriptor(getDescriptor(), bleCallback);
    }

    /**
     * 读取属性描述值
     * @param descriptor 属性描述值
     * @param bleCallback 读取回调
     * @return 返回读取是否成功的状态
     */
    public boolean readDescriptor(BluetoothGattDescriptor descriptor, IDescriptorCallback bleCallback) {
        listenAndTimer(bleCallback, MSG_READ_DES);
        return handleAfterInitialed(getBluetoothGatt().readDescriptor(descriptor), bleCallback);
    }

    /**
     * 读取设备信号值
     * @param bleCallback 读取回调
     * @return 返回读取是否成功的状态
     */
    public boolean readRemoteRssi(IRssiCallback bleCallback) {
        listenAndTimer(bleCallback, MSG_READ_RSSI);
        return handleAfterInitialed(getBluetoothGatt().readRemoteRssi(), bleCallback);
    }

    /**
     * 设置特征值监听，用来设置获取设备返回数据的监听
     * @param bleCallback 设备返回数据回调
     * @param isIndication 是否是指示器方式，指示器方式比普通通知方式更可靠，底层有应答处理
     * @return 返回设置监听是否成功
     */
    public boolean enableCharacteristicNotification(ICharacteristicCallback bleCallback, boolean isIndication) {
        return enableCharacteristicNotification(getCharacteristic(), bleCallback, isIndication);
    }

    /**
     * 设置特征值监听，用来设置获取设备返回数据的监听
     * @param characteristic 特征值
     * @param bleCallback 设备返回数据回调
     * @param isIndication 是否是指示器方式，指示器方式比普通通知方式更可靠，底层有应答处理
     * @return 返回设置监听是否成功
     */
    public boolean enableCharacteristicNotification(BluetoothGattCharacteristic characteristic, final ICharacteristicCallback
            bleCallback, boolean isIndication) {
        if (characteristic != null && (characteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            receiveCallback = bleCallback;
            return setCharacteristicNotification(getBluetoothGatt(), characteristic, true, isIndication);
        } else {
            if (bleCallback != null) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        bleCallback.onFailure(new OtherException("Characteristic [not supports] readable!"));
                        removeBleCallback(bleCallback);
                    }
                });
            }
            return false;
        }
    }

    /**
     * 设置属性描述值的数据获取监听
     * @param enable 是否可通知
     * @param isIndication 是否是指示器方式
     * @return 返回设置监听是否成功
     */
    public boolean setNotification(boolean enable, boolean isIndication) {
        return setNotification(getBluetoothGatt(), getCharacteristic(), getDescriptor(), enable, isIndication);
    }

    /**
     * 设置属性描述值的数据获取监听
     * @param gatt GATT
     * @param characteristic 特征值
     * @param descriptor 属性描述值
     * @param enable 是否可通知
     * @param isIndication 是否是指示器方式
     * @return 返回设置监听是否成功
     */
    public boolean setNotification(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, BluetoothGattDescriptor descriptor,
                                   boolean enable, boolean isIndication) {
        return setCharacteristicNotification(gatt, characteristic, enable, isIndication) && setDescriptorNotification(gatt, descriptor,
                enable);
    }

    /**
     * 设置特征值监听，用来设置获取设备返回数据的监听
     * @param gatt GATT
     * @param characteristic 特征值
     * @param enable 是否可通知
     * @param isIndication 是否是指示器方式
     * @return 返回设置监听是否成功
     */
    public boolean setCharacteristicNotification(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean enable, boolean
            isIndication) {
        if (gatt != null && characteristic != null) {
            ViseLog.i("Characteristic set notification value: " + enable);
            boolean success = gatt.setCharacteristicNotification(characteristic, enable);
            if (enable) {
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(BleConstant
                        .CLIENT_CHARACTERISTIC_CONFIG));
                if (descriptor != null) {
                    if (isIndication) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                    } else {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    }
                    gatt.writeDescriptor(descriptor);
                    ViseLog.i("Characteristic set notification is Success!");
                }
            }
            return success;
        }
        return false;
    }

    /**
     * 设置属性描述值的数据获取监听
     * @param gatt GATT
     * @param descriptor 属性描述值
     * @param enable 是否可通知
     * @return 返回设置监听是否成功
     */
    public boolean setDescriptorNotification(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, boolean enable) {
        if (gatt != null && descriptor != null) {
            ViseLog.i("Descriptor set notification value: " + enable);
            if (enable) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            } else {
                descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            }
            return gatt.writeDescriptor(descriptor);
        }
        return false;
    }

    /**
     * 初始化操作
     * @param initiated 是否初始化成功
     * @param bleCallback 操作回调
     * @return 返回初始化是否成功
     */
    private boolean handleAfterInitialed(boolean initiated, final IBleCallback bleCallback) {
        if (bleCallback != null) {
            if (!initiated) {
                if (handler != null) {
                    handler.removeCallbacksAndMessages(null);
                }
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        bleCallback.onFailure(new InitiatedException());
                        removeBleCallback(bleCallback);
                    }
                });
            }
        }
        return initiated;
    }

    /**
     * 设置数据操作超时监听
     * @param bleCallback 操作回调
     * @param what 操作类型
     */
    private synchronized void listenAndTimer(final IBleCallback bleCallback, int what) {
        if (bleCallbacks != null && handler != null) {
            this.tempBleCallback = bleCallback;
            bleCallbacks.add(bleCallback);
            Message msg = handler.obtainMessage(what, bleCallback);
            handler.sendMessageDelayed(msg, operateTimeout);
        }
    }

    /**
     * 是否是主线程
     * @return
     */
    public boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    /**
     * 切换到主线程
     * @param runnable
     */
    public void runOnMainThread(Runnable runnable) {
        if (isMainThread()) {
            runnable.run();
        } else {
            if (handler != null) {
                handler.post(runnable);
            }
        }
    }

    /**
     * 设备是否连接
     * @return 返回设备是否连接
     */
    public boolean isConnected() {
        if (state == State.CONNECT_SUCCESS) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 移除操作回调
     * @param bleCallback
     */
    public synchronized void removeBleCallback(IBleCallback bleCallback) {
        if (bleCallbacks != null && bleCallbacks.size() > 0) {
            bleCallbacks.remove(bleCallback);
        }
    }

    /**
     * 刷新设备缓存
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
     * 清除设备的相关信息，一般是在不使用该设备时调用
     */
    public synchronized void clear() {
        disconnect();
        refreshDeviceCache();
        close();
        if (bleCallbacks != null) {
            bleCallbacks.clear();
        }
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    /*==================get and set========================*/

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
     * 获取蓝牙GATT
     * @return 返回蓝牙GATT
     */
    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }

    /**
     * 获取操作回调集合
     * @return 返回操作回调集合
     */
    public Set<IBleCallback> getBleCallbacks() {
        return bleCallbacks;
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
     * @return 返回ViseBluetooth
     */
    public ViseBluetooth setService(BluetoothGattService service) {
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
     * @return 返回ViseBluetooth
     */
    public ViseBluetooth setCharacteristic(BluetoothGattCharacteristic characteristic) {
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
     * @return 返回ViseBluetooth
     */
    public ViseBluetooth setDescriptor(BluetoothGattDescriptor descriptor) {
        this.descriptor = descriptor;
        return this;
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
     * @return 返回ViseBluetooth
     */
    public ViseBluetooth setOperateTimeout(int operateTimeout) {
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
     * @return 返回ViseBluetooth
     */
    public ViseBluetooth setConnectTimeout(int connectTimeout) {
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
     * @return 返回ViseBluetooth
     */
    public ViseBluetooth setScanTimeout(int scanTimeout) {
        this.scanTimeout = scanTimeout;
        return this;
    }

    /**
     * 获取设备当前状态
     * @return 返回设备当前状态
     */
    public State getState() {
        return state;
    }

    /**
     * 设置设备状态
     * @param state 设备状态
     * @return 返回ViseBluetooth
     */
    public ViseBluetooth setState(State state) {
        this.state = state;
        return this;
    }
}
