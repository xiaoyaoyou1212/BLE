package com.vise.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import com.vise.baseble.ViseBluetooth;
import com.vise.baseble.callback.IBleCallback;
import com.vise.baseble.callback.IConnectCallback;
import com.vise.baseble.exception.BleException;
import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.baseble.model.resolver.GattAttributeResolver;
import com.vise.baseble.utils.BleLog;
import com.vise.baseble.utils.HexUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/8/21 21:28.
 */
public class DeviceControlActivity extends AppCompatActivity {

    private static final String LIST_NAME = "NAME";
    private static final String LIST_UUID = "UUID";

    private ExpandableListView mGattServicesList;
    private TextView mDeviceAddress;
    private TextView mConnectionState;
    private TextView mGattUUID;
    private TextView mGattUUIDDesc;
    private TextView mDataAsString;
    private TextView mDataAsArray;

    private ViseBluetooth viseBluetooth;
    private BluetoothLeDevice mDevice;
    private List<List<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<>();

    private final ExpandableListView.OnChildClickListener servicesListClickListener = new ExpandableListView.OnChildClickListener() {
        @Override
        public boolean onChildClick(final ExpandableListView parent, final View v, final int groupPosition, final int childPosition, final long id) {
            if (mGattCharacteristics != null) {
                final BluetoothGattCharacteristic characteristic = mGattCharacteristics.get(groupPosition).get(childPosition);
                final int charaProp = characteristic.getProperties();
                if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                    viseBluetooth.readCharacteristic(characteristic, new IBleCallback<BluetoothGattCharacteristic>() {
                        @Override
                        public void onSuccess(final BluetoothGattCharacteristic characteristic, int type) {
                            if (characteristic == null) {
                                return;
                            }
                            BleLog.i("readCharacteristic onSuccess:"+ HexUtil.encodeHexStr(characteristic.getValue()));
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
                            BleLog.i("readCharacteristic onFailure:"+exception.getDescription());
                        }
                    });
                }
                return true;
            }
            return false;
        }
    };

    private IBleCallback<BluetoothGattCharacteristic> bleCallback = new IBleCallback<BluetoothGattCharacteristic>() {
        @Override
        public void onSuccess(final BluetoothGattCharacteristic characteristic, int type) {
            if (characteristic == null) {
                return;
            }
            BleLog.i("readCharacteristic onSuccess:"+ HexUtil.encodeHexStr(characteristic.getValue()));
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
            BleLog.i("readCharacteristic onFailure:"+exception.getDescription());
        }
    };

    private IConnectCallback connectCallback = new IConnectCallback() {
        @Override
        public void onConnectSuccess(BluetoothGatt gatt, int status) {
            BleLog.i("Connect Success!");
            mConnectionState.setText("true");
            invalidateOptionsMenu();
            if(gatt != null){
                displayGattServices(gatt.getServices());
            }
        }

        @Override
        public void onConnectFailure(BleException exception) {
            BleLog.i("Connect Failure!");
            mConnectionState.setText("false");
            invalidateOptionsMenu();
            clearUI();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_control);
        init();
    }

    private void init() {
        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        mDeviceAddress = (TextView) findViewById(R.id.device_address);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mGattUUID = (TextView) findViewById(R.id.uuid);
        mGattUUIDDesc = (TextView) findViewById(R.id.description);
        mDataAsString = (TextView) findViewById(R.id.data_as_string);
        mDataAsArray = (TextView) findViewById(R.id.data_as_array);

        viseBluetooth = ViseBluetooth.getInstance(this);
        mDevice = getIntent().getParcelableExtra(DeviceDetailActivity.EXTRA_DEVICE);
        if(mDevice != null){
            mDeviceAddress.setText(mDevice.getAddress());
        }
        mGattServicesList.setOnChildClickListener(servicesListClickListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        viseBluetooth.connect(mDevice, false, connectCallback);
    }

    @Override
    protected void onStop() {
        super.onStop();
        viseBluetooth.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viseBluetooth.clear();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.connect, menu);
        if (viseBluetooth.isConnected()) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                if(!viseBluetooth.isConnected()){
                    viseBluetooth.connect(mDevice, false, connectCallback);
                }
                break;
            case R.id.menu_disconnect:
                if(viseBluetooth.isConnected()){
                    viseBluetooth.disconnect();
                }
                break;
        }
        return true;
    }


    private void displayGattServices(final List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
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

        final SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[]{LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2},
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[]{LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2}
        );

        mGattServicesList.setAdapter(gattServiceAdapter);
        invalidateOptionsMenu();
    }

    private void showInfo(String uuid, byte[] dataArr){
        mGattUUID.setText(uuid != null ? uuid : getString(R.string.no_data));
        mGattUUIDDesc.setText(GattAttributeResolver.getAttributeName(uuid, getString(R.string.unknown)));
        mDataAsArray.setText(HexUtil.encodeHexStr(dataArr));
        mDataAsString.setText(new String(dataArr));
    }

    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mGattUUID.setText(R.string.no_data);
        mGattUUIDDesc.setText(R.string.no_data);
        mDataAsArray.setText(R.string.no_data);
        mDataAsString.setText(R.string.no_data);
    }
}
