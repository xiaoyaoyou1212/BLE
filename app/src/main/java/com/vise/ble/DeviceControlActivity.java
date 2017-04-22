package com.vise.ble;

import android.app.AlertDialog;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
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

import com.vise.baseble.ViseBluetooth;
import com.vise.baseble.callback.IBleCallback;
import com.vise.baseble.callback.IConnectCallback;
import com.vise.baseble.common.State;
import com.vise.baseble.exception.BleException;
import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.baseble.model.resolver.GattAttributeResolver;
import com.vise.baseble.utils.HexUtil;
import com.vise.log.ViseLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private BluetoothLeDevice mDevice;
    private BluetoothGattCharacteristic mCharacteristic;
    private StringBuilder mOutputInfo = new StringBuilder();
    private List<List<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<>();

    private IConnectCallback connectCallback = new IConnectCallback() {
        @Override
        public void onConnectSuccess(BluetoothGatt gatt, int status) {
            ViseLog.i("Connect Success!");
            Toast.makeText(DeviceControlActivity.this, "Connect Success!", Toast.LENGTH_SHORT).show();
            mConnectionState.setText("true");
            invalidateOptionsMenu();
            if (gatt != null) {
                simpleExpandableListAdapter = displayGattServices(gatt.getServices());
            }
        }

        @Override
        public void onConnectFailure(BleException exception) {
            ViseLog.i("Connect Failure!");
            Toast.makeText(DeviceControlActivity.this, "Connect Failure!", Toast.LENGTH_SHORT).show();
            mConnectionState.setText("false");
            invalidateOptionsMenu();
            clearUI();
        }

        @Override
        public void onDisconnect() {
            ViseLog.i("Disconnect!");
            Toast.makeText(DeviceControlActivity.this, "Disconnect!", Toast.LENGTH_SHORT).show();
            mConnectionState.setText("false");
            invalidateOptionsMenu();
            clearUI();
        }
    };

    private IBleCallback bleCallback = new IBleCallback() {
        @Override
        public void onSuccess(Object o, int type) {
            if (o == null) {
                return;
            }
            if (o instanceof BluetoothGattCharacteristic) {
                if (((BluetoothGattCharacteristic) o).getValue() == null) {
                    return;
                }
                ViseLog.i("notify success:" + HexUtil.encodeHexStr(((BluetoothGattCharacteristic) o).getValue()));
                mOutputInfo.append(HexUtil.encodeHexStr(((BluetoothGattCharacteristic) o).getValue())).append("\n");
                mOutput.setText(mOutputInfo.toString());
            }
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
                ViseBluetooth.getInstance().writeCharacteristic(mCharacteristic, HexUtil.decodeHex(mInput.getText().toString()
                        .toCharArray()), new IBleCallback<BluetoothGattCharacteristic>() {
                    @Override
                    public void onSuccess(BluetoothGattCharacteristic characteristic, int type) {
                        ViseLog.i("Send onSuccess!");
                    }

                    @Override
                    public void onFailure(BleException exception) {
                        ViseLog.i("Send onFail!");
                    }
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        ViseBluetooth.getInstance().connect(mDevice, false, connectCallback);
    }

    @Override
    protected void onStop() {
        super.onStop();
        ViseBluetooth.getInstance().disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ViseBluetooth.getInstance().clear();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.connect, menu);
        if (ViseBluetooth.getInstance().isConnected()) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        if (ViseBluetooth.getInstance().getState() == State.CONNECT_PROCESS) {
            menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_progress_indeterminate);
        } else {
            menu.findItem(R.id.menu_refresh).setActionView(null);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                invalidateOptionsMenu();
                if (!ViseBluetooth.getInstance().isConnected()) {
                    ViseBluetooth.getInstance().connect(mDevice, false, connectCallback);
                }
                break;
            case R.id.menu_disconnect:
                invalidateOptionsMenu();
                if (ViseBluetooth.getInstance().isConnected()) {
                    ViseBluetooth.getInstance().disconnect();
                }
                break;
        }
        return true;
    }


    private SimpleExpandableListAdapter displayGattServices(final List<BluetoothGattService> gattServices) {
        if (gattServices == null) return null;
        String uuid;
        final String unknownServiceString = getResources().getString(R.string.unknown_service);
        final String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        final List<Map<String, String>> gattServiceData = new ArrayList<>();
        final List<List<Map<String, String>>> gattCharacteristicData = new ArrayList<>();
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
    }

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
                final BluetoothGattCharacteristic characteristic = mGattCharacteristics.get(groupPosition).get(childPosition);
                final int charaProp = characteristic.getProperties();
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                    mCharacteristic = characteristic;
                    ((EditText) findViewById(R.id.show_write_characteristic)).setText(characteristic.getUuid().toString());
                } else if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                    ViseBluetooth.getInstance().readCharacteristic(characteristic, new IBleCallback<BluetoothGattCharacteristic>() {
                        @Override
                        public void onSuccess(final BluetoothGattCharacteristic characteristic, int type) {
                            if (characteristic == null) {
                                return;
                            }
                            ViseLog.i("readCharacteristic onSuccess:" + HexUtil.encodeHexStr(characteristic.getValue()));
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showInfo(characteristic.getUuid().toString(), characteristic.getValue());
                                }
                            });
                        }

                        @Override
                        public void onFailure(BleException exception) {
                            if (exception == null) {
                                return;
                            }
                            ViseLog.i("readCharacteristic onFailure:" + exception.getDescription());
                        }
                    });
                }
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    ((EditText) findViewById(R.id.show_notify_characteristic)).setText(characteristic.getUuid().toString());
                    ViseBluetooth.getInstance().enableCharacteristicNotification(characteristic, bleCallback, false);
                } else if ((charaProp & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
                    ((EditText) findViewById(R.id.show_notify_characteristic)).setText(characteristic.getUuid().toString());
                    ViseBluetooth.getInstance().enableCharacteristicNotification(characteristic, bleCallback, true);
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
        for (char ch : chars) {
            if (ch >= '0' && ch <= '9') continue;
            if (ch >= 'A' && ch <= 'F') continue;
            if (ch >= 'a' && ch <= 'f') continue;
            return false;
        }
        return true;
    }

}
