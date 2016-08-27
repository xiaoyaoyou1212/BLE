package com.vise.ble;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.vise.baseble.ViseBluetooth;
import com.vise.baseble.callback.scan.PeriodScanCallback;
import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.baseble.model.BluetoothLeDeviceStore;
import com.vise.baseble.utils.BleLog;
import com.vise.baseble.utils.BleUtil;
import com.vise.ble.adapter.DeviceAdapter;

import java.util.ArrayList;
import java.util.List;

public class DeviceScanActivity extends AppCompatActivity {

    private TextView supportTv;
    private TextView statusTv;
    private ListView deviceLv;
    private TextView scanCountTv;

    private ViseBluetooth viseBluetooth;
    private BluetoothLeDeviceStore bluetoothLeDeviceStore;
    private volatile List<BluetoothLeDevice> bluetoothLeDeviceList = new ArrayList<>();
    private DeviceAdapter adapter;

    private PeriodScanCallback periodScanCallback = new PeriodScanCallback() {
        @Override
        public void scanTimeout() {
            BleLog.i("scan timeout");
        }

        @Override
        public void onDeviceFound(BluetoothLeDevice bluetoothLeDevice) {
            if (bluetoothLeDeviceStore != null) {
                bluetoothLeDeviceStore.addDevice(bluetoothLeDevice);
                bluetoothLeDeviceList = bluetoothLeDeviceStore.getDeviceList();
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.setDeviceList(bluetoothLeDeviceList);
                    updateItemCount(adapter.getCount());
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);
        init();
    }

    private void init() {
        supportTv = (TextView) findViewById(R.id.scan_ble_support);
        statusTv = (TextView) findViewById(R.id.scan_ble_status);
        deviceLv = (ListView) findViewById(android.R.id.list);
        scanCountTv = (TextView) findViewById(R.id.scan_device_count);

        viseBluetooth = ViseBluetooth.getInstance(this);
        bluetoothLeDeviceStore = new BluetoothLeDeviceStore();
        adapter = new DeviceAdapter(this);
        deviceLv.setAdapter(adapter);

        deviceLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                BluetoothLeDevice device = (BluetoothLeDevice) adapter.getItem(position);
                if (device == null) return;
                Intent intent = new Intent(DeviceScanActivity.this, DeviceDetailActivity.class);
                intent.putExtra(DeviceDetailActivity.EXTRA_DEVICE, device);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean isSupport = BleUtil.isSupportBle(this);
        boolean isOpenBle = BleUtil.isBleEnable(this);
        if(isSupport){
            supportTv.setText(getString(R.string.supported));
        } else{
            supportTv.setText(getString(R.string.not_supported));
        }
        if (isOpenBle) {
            statusTv.setText(getString(R.string.on));
        } else{
            statusTv.setText(getString(R.string.off));
        }
        invalidateOptionsMenu();
        if(BleUtil.isBleEnable(this)){
            startScan();
        } else{
            BleUtil.enableBluetooth(this, 1);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (viseBluetooth != null) {
            viseBluetooth.clear();
            viseBluetooth = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.scan, menu);
        if (periodScanCallback != null && !periodScanCallback.isScanning()) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_progress_indeterminate);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                if(BleUtil.isBleEnable(this)){
                    startScan();
                } else{
                    BleUtil.enableBluetooth(this, 1);
                }
                break;
            case R.id.menu_stop:
                stopScan();
                break;
            case R.id.menu_about:
                displayAboutDialog();
                break;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK){
            statusTv.setText(getString(R.string.on));
            if(requestCode == 1){
                startScan();
            }
        } else if(resultCode == RESULT_CANCELED){
            finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void startScan(){
        updateItemCount(0);
        if (bluetoothLeDeviceStore != null) {
            bluetoothLeDeviceStore.clear();
        }
        if (adapter != null && bluetoothLeDeviceList != null) {
            bluetoothLeDeviceList.clear();
            adapter.setDeviceList(bluetoothLeDeviceList);
        }
        if (viseBluetooth != null) {
            viseBluetooth.setScanTimeout(-1).startScan(periodScanCallback);
        }
        invalidateOptionsMenu();
    }

    private void stopScan(){
        if (viseBluetooth != null) {
            viseBluetooth.stopScan(periodScanCallback);
        }
        invalidateOptionsMenu();
    }

    private void updateItemCount(final int count) {
        scanCountTv.setText(getString(R.string.formatter_item_count, String.valueOf(count)));
    }

    private void displayAboutDialog() {
        final int paddingSizeDp = 5;
        final float scale = getResources().getDisplayMetrics().density;
        final int dpAsPixels = (int) (paddingSizeDp * scale + 0.5f);

        final TextView textView = new TextView(this);
        final SpannableString text = new SpannableString(getString(R.string.about_dialog_text));

        textView.setText(text);
        textView.setAutoLinkMask(RESULT_OK);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setPadding(dpAsPixels, dpAsPixels, dpAsPixels, dpAsPixels);

        Linkify.addLinks(text, Linkify.ALL);
        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_about)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, null)
                .setView(textView)
                .show();
    }
}
