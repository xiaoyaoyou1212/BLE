package com.vise.ble;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 100;

    private TextView supportTv;
    private TextView statusTv;
    private ListView deviceLv;
    private TextView scanCountTv;

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
        ViseBluetooth.getInstance().init(getApplicationContext());
        init();
    }

    private void init() {
        supportTv = (TextView) findViewById(R.id.scan_ble_support);
        statusTv = (TextView) findViewById(R.id.scan_ble_status);
        deviceLv = (ListView) findViewById(android.R.id.list);
        scanCountTv = (TextView) findViewById(R.id.scan_device_count);

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
        if (isSupport) {
            supportTv.setText(getString(R.string.supported));
        } else {
            supportTv.setText(getString(R.string.not_supported));
        }
        if (isOpenBle) {
            statusTv.setText(getString(R.string.on));
        } else {
            statusTv.setText(getString(R.string.off));
        }
        invalidateOptionsMenu();
        checkBluetoothPermission();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
                checkBluetoothPermission();
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
        if (resultCode == RESULT_OK) {
            statusTv.setText(getString(R.string.on));
            if (requestCode == 1) {
                startScan();
            }
        } else if (resultCode == RESULT_CANCELED) {
            finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /*  校验蓝牙权限  */
    private void checkBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            //校验是否已具有模糊定位权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
            } else {
                //具有权限
                scanBluetooth();
            }
        } else {
            //系统不高于6.0直接执行
            scanBluetooth();
        }
    }

    /**
     * 对返回的值进行处理，相当于StartActivityForResult
     */
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        doNext(requestCode, grantResults);
    }

    private void doNext(int requestCode, int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //同意权限
                scanBluetooth();
            } else {
                // 权限拒绝，提示用户开启权限
                denyPermission();
            }
        }
    }

    private void denyPermission() {
        finish();
    }

    private void scanBluetooth() {
        if (BleUtil.isBleEnable(this)) {
            startScan();
        } else {
            BleUtil.enableBluetooth(this, 1);
        }
    }

    private void startScan() {
        updateItemCount(0);
        if (bluetoothLeDeviceStore != null) {
            bluetoothLeDeviceStore.clear();
        }
        if (adapter != null && bluetoothLeDeviceList != null) {
            bluetoothLeDeviceList.clear();
            adapter.setDeviceList(bluetoothLeDeviceList);
        }
        ViseBluetooth.getInstance().setScanTimeout(-1).startScan(periodScanCallback);
        invalidateOptionsMenu();
    }

    private void stopScan() {
        ViseBluetooth.getInstance().stopScan(periodScanCallback);
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
        new AlertDialog.Builder(this).setTitle(R.string.menu_about).setCancelable(false).setPositiveButton(android.R.string.ok, null)
                .setView(textView).show();
    }
}
