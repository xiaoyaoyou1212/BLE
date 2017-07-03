package com.vise.ble;

import android.Manifest;
import android.annotation.TargetApi;
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
import com.vise.baseble.callback.scan.PeriodLScanCallback;
import com.vise.baseble.callback.scan.PeriodScanCallback;
import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.baseble.model.BluetoothLeDeviceStore;
import com.vise.baseble.utils.BleUtil;
import com.vise.ble.adapter.DeviceAdapter;
import com.vise.log.ViseLog;
import com.vise.log.inner.LogcatTree;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备扫描展示界面
 */
public class DeviceScanActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 100;

    private TextView supportTv;
    private TextView statusTv;
    private ListView deviceLv;
    private TextView scanCountTv;

    //设备扫描结果存储仓库
    private BluetoothLeDeviceStore bluetoothLeDeviceStore;
    //设备扫描结果集合
    private volatile List<BluetoothLeDevice> bluetoothLeDeviceList = new ArrayList<>();
    //设备扫描结果展示适配器
    private DeviceAdapter adapter;

    //针对API 15以上版本的新扫描方式（可以不用该方式，下面的方式能兼容新API）
    private PeriodLScanCallback periodLScanCallback;
    //自定义的扫描方式，基本能满足大部分需求
    private PeriodScanCallback periodScanCallback = new PeriodScanCallback() {
        @Override
        public void scanTimeout() {
            ViseLog.i("scan timeout");
        }

        @Override
        public void onDeviceFound(BluetoothLeDevice bluetoothLeDevice) {
            ViseLog.i("Founded Scan Device:" + bluetoothLeDevice);
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
        ViseLog.getLogConfig().configAllowLog(true);//配置日志信息
        ViseLog.plant(new LogcatTree());//添加Logcat打印信息
        //蓝牙信息初始化，全局唯一，必须在应用初始化时调用
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
                //点击某个扫描到的设备进入设备详细信息界面
                BluetoothLeDevice device = (BluetoothLeDevice) adapter.getItem(position);
                if (device == null) return;
                Intent intent = new Intent(DeviceScanActivity.this, DeviceDetailActivity.class);
                intent.putExtra(DeviceDetailActivity.EXTRA_DEVICE, device);
                startActivity(intent);
            }
        });

        /**
         * 注：增加这个扫描回调处理是为了演示在使用5.0新API上的使用示例
         * 以前的扫描方式完全可以在5.0以上系统使用，此项目中也做了一定的封装，
         * 基本也能达到新API中扫描方式的大部分功能。
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            periodLScanCallback = new PeriodLScanCallback() {
                @Override
                public void scanTimeout() {
                    ViseLog.i("scan timeout");
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
        }
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

    /**
     * 菜单栏的显示
     * @param menu 菜单
     * @return 返回是否拦截操作
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.scan, menu);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (periodLScanCallback != null && !periodLScanCallback.isScanning()) {
                menu.findItem(R.id.menu_stop).setVisible(false);
                menu.findItem(R.id.menu_scan).setVisible(true);
                menu.findItem(R.id.menu_refresh).setActionView(null);
            } else {
                menu.findItem(R.id.menu_stop).setVisible(true);
                menu.findItem(R.id.menu_scan).setVisible(false);
                menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_progress_indeterminate);
            }
        } else {
            if (periodScanCallback != null && !periodScanCallback.isScanning()) {
                menu.findItem(R.id.menu_stop).setVisible(false);
                menu.findItem(R.id.menu_scan).setVisible(true);
                menu.findItem(R.id.menu_refresh).setActionView(null);
            } else {
                menu.findItem(R.id.menu_stop).setVisible(true);
                menu.findItem(R.id.menu_scan).setVisible(false);
                menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_progress_indeterminate);
            }
        }
        return true;
    }

    /**
     * 点击菜单栏的处理
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan://开始扫描
                checkBluetoothPermission();
                break;
            case R.id.menu_stop://停止扫描
                stopScan();
                break;
            case R.id.menu_about://关于
                displayAboutDialog();
                break;
        }
        return true;
    }

    /**
     * 打开或关闭蓝牙后的回调
     * @param requestCode
     * @param resultCode
     * @param data
     */
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

    /**
     * 检查蓝牙权限
     */
    private void checkBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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

    /**
     * 权限申请的下一步处理
     * @param requestCode 申请码
     * @param grantResults 申请结果
     */
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

    /**
     * 权限申请被拒绝的处理方式
     */
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

    /**
     * 开始扫描，可以不用区分版本，这里是方便演示
     */
    private void startScan() {
        updateItemCount(0);
        if (bluetoothLeDeviceStore != null) {
            bluetoothLeDeviceStore.clear();
        }
        if (adapter != null && bluetoothLeDeviceList != null) {
            bluetoothLeDeviceList.clear();
            adapter.setDeviceList(bluetoothLeDeviceList);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ViseBluetooth.getInstance().setScanTimeout(-1).startScan(periodLScanCallback);
        } else {
            ViseBluetooth.getInstance().setScanTimeout(-1).startScan(periodScanCallback);
        }
        invalidateOptionsMenu();
    }

    /**
     * 停止扫描，可以不用区分版本，这里是方便演示
     */
    private void stopScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ViseBluetooth.getInstance().stopScan(periodLScanCallback);
        } else {
            ViseBluetooth.getInstance().stopScan(periodScanCallback);
        }
        invalidateOptionsMenu();
    }

    /**
     * 更新扫描到的设备个数
     * @param count
     */
    private void updateItemCount(final int count) {
        scanCountTv.setText(getString(R.string.formatter_item_count, String.valueOf(count)));
    }

    /**
     * 显示项目信息
     */
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
