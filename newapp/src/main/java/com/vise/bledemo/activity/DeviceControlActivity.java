package com.vise.bledemo.activity;

import android.app.AlertDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.vise.baseble.ViseBle;
import com.vise.baseble.callback.IBleCallback;
import com.vise.baseble.callback.IConnectCallback;
import com.vise.baseble.common.ConnectState;
import com.vise.baseble.common.PropertyType;
import com.vise.baseble.core.BluetoothGattInfo;
import com.vise.baseble.core.DeviceMirror;
import com.vise.baseble.exception.BleException;
import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.baseble.model.resolver.GattAttributeResolver;
import com.vise.baseble.utils.HexUtil;
import com.vise.bledemo.R;
import com.vise.log.ViseLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * 设备数据操作相关展示界面
 */
public class DeviceControlActivity extends AppCompatActivity {

    private static final String LIST_NAME = "NAME";
    private static final String LIST_UUID = "UUID";

    private SimpleExpandableListAdapter simpleExpandableListAdapter;
    private TextView mConnectionState;
    private TextView mGattUUID;
    private TextView mGattUUIDDesc;
    private TextView mDataAsString;
    private TextView mDataAsArray;
    private EditText mInput;
    private EditText mOutput;

    //设备镜像
    private DeviceMirror mDeviceMirror;
    //设备信息
    private BluetoothLeDevice mDevice;
    //特征值
    private BluetoothGattCharacteristic mCharacteristic;
    //输出数据展示
    private StringBuilder mOutputInfo = new StringBuilder();
    private List<BluetoothGattService> mGattServices = new ArrayList<>();
    //设备特征值集合
    private List<List<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<>();

    //发送队列，提供一种简单的处理方式，实际项目场景需要根据需求优化
    private Queue<byte[]> dataInfoQueue = new LinkedList<>();
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            send();
        }
    };

    private void send(byte[] data) {
        if (dataInfoQueue != null) {
            dataInfoQueue.clear();
            dataInfoQueue = splitPacketFor20Byte(data);
            handler.post(runnable);
        }
    }

    private void send() {
        if (dataInfoQueue != null && !dataInfoQueue.isEmpty()) {
            if (dataInfoQueue.peek() != null && mDeviceMirror != null) {
                mDeviceMirror.writeData(dataInfoQueue.poll());
            }
            if (dataInfoQueue.peek() != null) {
                handler.postDelayed(runnable, 100);
            }
        }
    }

    /**
     * 数据分包
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

    /**
     * 连接回调
     */
    private IConnectCallback connectCallback = new IConnectCallback() {

        @Override
        public void onConnectSuccess(final DeviceMirror deviceMirror) {
            ViseLog.i("Connect Success!");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(DeviceControlActivity.this, "Connect Success!", Toast.LENGTH_SHORT).show();
                    mConnectionState.setText("true");
                    invalidateOptionsMenu();
                    mDeviceMirror = deviceMirror;
                    if (deviceMirror != null && deviceMirror.getBluetoothGatt() != null) {
                        simpleExpandableListAdapter = displayGattServices(deviceMirror.getBluetoothGatt().getServices());
                    }
                }
            });
        }

        @Override
        public void onConnectFailure(BleException exception) {
            ViseLog.i("Connect Failure!");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(DeviceControlActivity.this, "Connect Failure!", Toast.LENGTH_SHORT).show();
                    mConnectionState.setText("false");
                    invalidateOptionsMenu();
                    clearUI();
                }
            });
        }

        @Override
        public void onDisconnect(boolean isActive) {
            ViseLog.i("Disconnect!");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(DeviceControlActivity.this, "Disconnect!", Toast.LENGTH_SHORT).show();
                    mConnectionState.setText("false");
                    invalidateOptionsMenu();
                    clearUI();
                }
            });
        }
    };

    /**
     * 接收设备返回的数据回调
     */
    private IBleCallback receiveCallback = new IBleCallback() {
        @Override
        public void onSuccess(final byte[] data, BluetoothGattInfo bluetoothGattInfo) {
            if (data == null) {
                return;
            }
            ViseLog.i("notify success:" + HexUtil.encodeHexStr(data));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mOutputInfo.append(HexUtil.encodeHexStr(data)).append("\n");
                    mOutput.setText(mOutputInfo.toString());
                }
            });
        }

        @Override
        public void onFailure(BleException exception) {
            if (exception == null) {
                return;
            }
            ViseLog.i("notify fail:" + exception.getDescription());
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_control);
        init();
    }

    private void init() {
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mGattUUID = (TextView) findViewById(R.id.uuid);
        mGattUUIDDesc = (TextView) findViewById(R.id.description);
        mDataAsString = (TextView) findViewById(R.id.data_as_string);
        mDataAsArray = (TextView) findViewById(R.id.data_as_array);
        mInput = (EditText) findViewById(R.id.input);
        mOutput = (EditText) findViewById(R.id.output);

        mDevice = getIntent().getParcelableExtra(DeviceDetailActivity.EXTRA_DEVICE);
        if (mDevice != null) {
            ((TextView) findViewById(R.id.device_address)).setText(mDevice.getAddress());
        }

        findViewById(R.id.select_write_characteristic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showGattServices();
            }
        });
        findViewById(R.id.select_notify_characteristic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showGattServices();
            }
        });
        findViewById(R.id.select_read_characteristic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showGattServices();
            }
        });
        findViewById(R.id.send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCharacteristic == null) {
                    Toast.makeText(DeviceControlActivity.this, "Please select enable write characteristic!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (mInput.getText() == null || mInput.getText().toString() == null) {
                    Toast.makeText(DeviceControlActivity.this, "Please input command!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!isHexData(mInput.getText().toString())) {
                    Toast.makeText(DeviceControlActivity.this, "Please input hex data command!", Toast.LENGTH_SHORT).show();
                    return;
                }
                send(HexUtil.decodeHex(mInput.getText().toString().toCharArray()));
            }
        });
    }

    @Override
    protected void onResume() {
        invalidateOptionsMenu();
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.connect, menu);
        if (ViseBle.getInstance().isConnect(mDevice)) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
            mConnectionState.setText("true");
            DeviceMirror deviceMirror = ViseBle.getInstance().getDeviceMirror(mDevice);
            if (deviceMirror != null) {
                simpleExpandableListAdapter = displayGattServices(deviceMirror.getBluetoothGatt().getServices());
            }
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            mConnectionState.setText("false");
        }
        if (ViseBle.getInstance().getConnectState(mDevice) == ConnectState.CONNECT_PROCESS) {
            menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_progress_indeterminate);
        } else {
            menu.findItem(R.id.menu_refresh).setActionView(null);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect://连接设备
                invalidateOptionsMenu();
                if (!ViseBle.getInstance().isConnect(mDevice)) {
                    ViseBle.getInstance().connect(mDevice, connectCallback);
                }
                break;
            case R.id.menu_disconnect://断开设备
                invalidateOptionsMenu();
                if (ViseBle.getInstance().isConnect(mDevice)) {
                    ViseBle.getInstance().disconnect(mDevice);
                }
                break;
        }
        return true;
    }

    /**
     * 根据GATT服务显示该服务下的所有特征值
     * @param gattServices GATT服务
     * @return
     */
    private SimpleExpandableListAdapter displayGattServices(final List<BluetoothGattService> gattServices) {
        if (gattServices == null) return null;
        String uuid;
        final String unknownServiceString = getResources().getString(R.string.unknown_service);
        final String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        final List<Map<String, String>> gattServiceData = new ArrayList<>();
        final List<List<Map<String, String>>> gattCharacteristicData = new ArrayList<>();

        mGattServices = new ArrayList<>();
        mGattCharacteristics = new ArrayList<>();

        // Loops through available GATT Services.
        for (final BluetoothGattService gattService : gattServices) {
            final Map<String, String> currentServiceData = new HashMap<>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(LIST_NAME, GattAttributeResolver.getAttributeName(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            final List<Map<String, String>> gattCharacteristicGroupData = new ArrayList<>();
            final List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            final List<BluetoothGattCharacteristic> charas = new ArrayList<>();

            // Loops through available Characteristics.
            for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                final Map<String, String> currentCharaData = new HashMap<>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(LIST_NAME, GattAttributeResolver.getAttributeName(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }

            mGattServices.add(gattService);
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        final SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(this, gattServiceData, android.R.layout
                .simple_expandable_list_item_2, new String[]{LIST_NAME, LIST_UUID}, new int[]{android.R.id.text1, android.R.id.text2},
                gattCharacteristicData, android.R.layout.simple_expandable_list_item_2, new String[]{LIST_NAME, LIST_UUID}, new
                int[]{android.R.id.text1, android.R.id.text2});
        return gattServiceAdapter;
    }

    private void showInfo(String uuid, byte[] dataArr) {
        mGattUUID.setText(uuid != null ? uuid : getString(R.string.no_data));
        mGattUUIDDesc.setText(GattAttributeResolver.getAttributeName(uuid, getString(R.string.unknown)));
        mDataAsArray.setText(HexUtil.encodeHexStr(dataArr));
        mDataAsString.setText(new String(dataArr));
    }

    private void clearUI() {
        mGattUUID.setText(R.string.no_data);
        mGattUUIDDesc.setText(R.string.no_data);
        mDataAsArray.setText(R.string.no_data);
        mDataAsString.setText(R.string.no_data);
        mInput.setText("");
        mOutput.setText("");
        ((EditText) findViewById(R.id.show_write_characteristic)).setText("");
        ((EditText) findViewById(R.id.show_notify_characteristic)).setText("");
        mOutputInfo = new StringBuilder();
        simpleExpandableListAdapter = null;
    }

    /**
     * 显示GATT服务展示的信息
     */
    private void showGattServices() {
        if (simpleExpandableListAdapter == null) {
            return;
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(DeviceControlActivity.this);
        View view = LayoutInflater.from(DeviceControlActivity.this).inflate(R.layout.item_gatt_services, null);
        ExpandableListView expandableListView = (ExpandableListView) view.findViewById(R.id.dialog_gatt_services_list);
        expandableListView.setAdapter(simpleExpandableListAdapter);
        builder.setView(view);
        final AlertDialog dialog = builder.show();
        expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                dialog.dismiss();
                final BluetoothGattService service = mGattServices.get(groupPosition);
                final BluetoothGattCharacteristic characteristic = mGattCharacteristics.get(groupPosition).get(childPosition);
                final int charaProp = characteristic.getProperties();
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                    mCharacteristic = characteristic;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((EditText) findViewById(R.id.show_write_characteristic)).setText(characteristic.getUuid().toString());
                        }
                    });
                    if (mDeviceMirror != null) {
                        mDeviceMirror.withUUID(new IBleCallback() {
                            @Override
                            public void onSuccess(byte[] data, BluetoothGattInfo bluetoothGattInfo) {

                            }

                            @Override
                            public void onFailure(BleException exception) {

                            }
                        }, PropertyType.PROPERTY_WRITE, service.getUuid(), characteristic.getUuid(), null);
                    }
                } else if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                    if (mDeviceMirror != null) {
                        mDeviceMirror.withUUID(new IBleCallback() {
                            @Override
                            public void onSuccess(final byte[] data, BluetoothGattInfo bluetoothGattInfo) {
                                ViseLog.i("read data success:" + HexUtil.encodeHexStr(data));
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        showInfo(characteristic.getUuid().toString(), data);
                                    }
                                });
                            }

                            @Override
                            public void onFailure(BleException exception) {
                                ViseLog.e("read data fail:" + exception);
                            }
                        }, PropertyType.PROPERTY_READ, service.getUuid(), characteristic.getUuid(), null);
                        mDeviceMirror.readData();
                    }
                }
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((EditText) findViewById(R.id.show_notify_characteristic)).setText(characteristic.getUuid().toString());
                        }
                    });
                    if (mDeviceMirror != null) {
                        mDeviceMirror.withUUID(new IBleCallback() {
                            @Override
                            public void onSuccess(byte[] data, BluetoothGattInfo bluetoothGattInfo) {
                                ViseLog.i("enable notify success:" + HexUtil.encodeHexStr(data));
                                mDeviceMirror.setNotifyListener(service.getUuid().toString()
                                        + characteristic.getUuid().toString(), receiveCallback);
                            }

                            @Override
                            public void onFailure(BleException exception) {
                                ViseLog.e("enable notify fail:" + exception);
                            }
                        }, PropertyType.PROPERTY_NOTIFY, service.getUuid(), characteristic.getUuid(), null);
                        mDeviceMirror.registerNotify(false);
                    }
                } else if ((charaProp & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((EditText) findViewById(R.id.show_notify_characteristic)).setText(characteristic.getUuid().toString());
                        }
                    });
                    if (mDeviceMirror != null) {
                        mDeviceMirror.withUUID(new IBleCallback() {
                            @Override
                            public void onSuccess(byte[] data, BluetoothGattInfo bluetoothGattInfo) {
                                ViseLog.i("enable indicate success:" + HexUtil.encodeHexStr(data));
                                mDeviceMirror.setNotifyListener(service.getUuid().toString()
                                        + characteristic.getUuid().toString(), receiveCallback);
                            }

                            @Override
                            public void onFailure(BleException exception) {
                                ViseLog.e("enable indicate fail:" + exception);
                            }
                        }, PropertyType.PROPERTY_INDICATE, service.getUuid(), characteristic.getUuid(), null);
                        mDeviceMirror.registerNotify(true);
                    }
                }
                return true;
            }
        });
    }

    private boolean isHexData(String str) {
        if (str == null) {
            return false;
        }
        char[] chars = str.toCharArray();
        if ((chars.length & 1) != 0) {//个数为奇数，直接返回false
            return false;
        }
        for (char ch : chars) {
            if (ch >= '0' && ch <= '9') continue;
            if (ch >= 'A' && ch <= 'F') continue;
            if (ch >= 'a' && ch <= 'f') continue;
            return false;
        }
        return true;
    }

}
