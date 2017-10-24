package com.vise.bledemo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.vise.baseble.ViseBle;
import com.vise.baseble.callback.IBleCallback;
import com.vise.baseble.callback.IConnectCallback;
import com.vise.baseble.callback.scan.IScanCallback;
import com.vise.baseble.callback.scan.ScanCallback;
import com.vise.baseble.common.PropertyType;
import com.vise.baseble.core.BluetoothGattInfo;
import com.vise.baseble.core.DeviceMirror;
import com.vise.baseble.exception.BleException;
import com.vise.baseble.model.BluetoothLeDeviceStore;
import com.vise.baseble.utils.HexUtil;
import com.vise.log.ViseLog;
import com.vise.log.inner.LogcatTree;

import java.nio.ByteBuffer;

/**
 * @Description:
 * @author: <a href="http://xiaoyaoyou1212.360doc.com">DAWI</a>
 * @date: 2017/10/20 17:35
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mDevice_scan;
    private Button mDevice_connect;
    private Button mDevice_send_data;
    private Button mDevice_read_data;
    private Button mDevice_receive_data;

    private DeviceMirror mDeviceMirrorFirst;
    private DeviceMirror mDeviceMirrorSecond;
    private int count = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViseLog.getLogConfig().configAllowLog(true);//配置日志信息
        ViseLog.plant(new LogcatTree());//添加Logcat打印信息
        //蓝牙相关配置修改
        ViseBle.config()
                .setScanTimeout(5000)
                .setMaxConnectCount(1);
        //蓝牙信息初始化，全局唯一，必须在应用初始化时调用
        ViseBle.getInstance().init(getApplicationContext());
        init();
    }

    private void init() {
        bindViews();
        bindEvent();
    }

    private void bindViews() {
        mDevice_scan = (Button) findViewById(R.id.device_scan);
        mDevice_connect = (Button) findViewById(R.id.device_connect);
        mDevice_send_data = (Button) findViewById(R.id.device_send_data);
        mDevice_read_data = (Button) findViewById(R.id.device_read_data);
        mDevice_receive_data = (Button) findViewById(R.id.device_receive_data);
    }

    private void bindEvent() {
        mDevice_scan.setOnClickListener(this);
        mDevice_connect.setOnClickListener(this);
        mDevice_send_data.setOnClickListener(this);
        mDevice_read_data.setOnClickListener(this);
        mDevice_receive_data.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.device_scan:
                scan();
                break;
            case R.id.device_connect:
                connect();
                break;
            case R.id.device_send_data:
                write();
                break;
            case R.id.device_read_data:
                read();
                break;
            case R.id.device_receive_data:
                receive();
                break;
        }
    }

    private void scan() {
        ViseLog.i("start scan");
        ViseBle.getInstance().startScan(new ScanCallback(new IScanCallback() {
            @Override
            public void onDeviceFound(BluetoothLeDeviceStore bluetoothLeDeviceStore) {
            }

            @Override
            public void onScanFinish(BluetoothLeDeviceStore bluetoothLeDeviceStore) {
                ViseLog.i("onScanFinish" + bluetoothLeDeviceStore);
            }

            @Override
            public void onScanTimeout() {
                ViseLog.i("onScanTimeout");
            }
        }));
    }

    private void connect() {
        ViseLog.i("start connect");
        ViseBle.getInstance().connectByName("het-31-8", new IConnectCallback() {
            @Override
            public void onConnectSuccess(DeviceMirror deviceMirror) {
                ViseLog.i("onConnectSuccess het-31-8 " + deviceMirror);
                mDeviceMirrorFirst = deviceMirror;
            }

            @Override
            public void onConnectFailure(BleException exception) {
                ViseLog.i("onConnectFailure" + exception);
            }

            @Override
            public void onDisconnect(boolean isActive) {
                ViseLog.i("onDisconnect:" + isActive);
            }
        });
        ViseBle.getInstance().connectByName("HET-175690-31-9", new IConnectCallback() {
            @Override
            public void onConnectSuccess(DeviceMirror deviceMirror) {
                ViseLog.i("onConnectSuccess HET-175690-31-9 " + deviceMirror);
                mDeviceMirrorSecond = deviceMirror;
            }

            @Override
            public void onConnectFailure(BleException exception) {
                ViseLog.i("onConnectFailure" + exception);
            }

            @Override
            public void onDisconnect(boolean isActive) {
                ViseLog.i("onDisconnect");
            }
        });
    }

    private void write() {
        if (mDeviceMirrorFirst != null) {
            final ByteBuffer byteBuffer = ByteBuffer.allocate(20);
            for (int i = 0; i < 20; i++) {
                byteBuffer.put((byte) 0x00);
            }
            mDeviceMirrorFirst.withUUID(new IBleCallback() {
                @Override
                public void onSuccess(byte[] data, BluetoothGattInfo bluetoothGattInfo) {
                    ViseLog.i("onSuccess:" + HexUtil.encodeHexStr(data) + "###" + count);
                    if (count < 20) {
                        count++;
                        mDeviceMirrorFirst.writeData(byteBuffer.array());
                    }
                }

                @Override
                public void onFailure(BleException exception) {
                    ViseLog.i("onFailure:" + exception);
                }
            }, PropertyType.PROPERTY_WRITE, Constants.UUID.BLE_SERVICE_UUID, Constants.UUID.BLE_WRITE_UUID, null);
            mDeviceMirrorFirst.writeData(byteBuffer.array());
            ViseLog.i("start write data:" + HexUtil.encodeHexStr(byteBuffer.array()));
            /*for (int i = 0; i < 20; i++) {
                count = i;
                mDeviceMirrorFirst.writeData(byteBuffer.array());
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }*/
        }
    }

    private void read() {
        if (mDeviceMirrorSecond != null) {
            mDeviceMirrorSecond.withUUID(new IBleCallback() {
                @Override
                public void onSuccess(byte[] data, BluetoothGattInfo bluetoothGattInfo) {
                    ViseLog.i("onSuccess:" + HexUtil.encodeHexStr(data));
                }

                @Override
                public void onFailure(BleException exception) {
                    ViseLog.i("onFailure:" + exception);
                }
            }, PropertyType.PROPERTY_READ, Constants.UUID.BATTERY_SERVICE_UUID, Constants.UUID.BATTERY_LEVEL_CHARACTERISTIC_UUID, null);
            mDeviceMirrorSecond.readData();
        }
    }

    private void receive() {
        if (mDeviceMirrorFirst != null) {
            mDeviceMirrorFirst.withUUID(new IBleCallback() {
                @Override
                public void onSuccess(byte[] data, BluetoothGattInfo bluetoothGattInfo) {
                    ViseLog.i("onSuccess:" + HexUtil.encodeHexStr(data));
                    ViseBle.getInstance().setNotifyListener(mDeviceMirrorFirst, Constants.UUID.BLE_SERVICE_UUID
                            + Constants.UUID.BLE_NOTIFY_UUID, new IBleCallback() {
                        @Override
                        public void onSuccess(byte[] data, BluetoothGattInfo bluetoothGattInfo) {
                            ViseLog.i("onSuccess:" + HexUtil.encodeHexStr(data));
                        }

                        @Override
                        public void onFailure(BleException exception) {
                            ViseLog.i("onFailure:" + exception);
                        }
                    });
                }

                @Override
                public void onFailure(BleException exception) {
                    ViseLog.i("onFailure:" + exception);
                }
            }, PropertyType.PROPERTY_NOTIFY, Constants.UUID.BLE_SERVICE_UUID, Constants.UUID.BLE_NOTIFY_UUID, null);
            mDeviceMirrorFirst.registerNotify(false);
        }
    }
}
