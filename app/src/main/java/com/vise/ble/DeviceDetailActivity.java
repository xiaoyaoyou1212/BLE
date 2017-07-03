package com.vise.ble;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.vise.baseble.common.BluetoothServiceType;
import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.baseble.model.adrecord.AdRecord;
import com.vise.baseble.utils.AdRecordUtil;
import com.vise.baseble.utils.HexUtil;
import com.vise.ble.adapter.MergeAdapter;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 设备详细信息展示界面
 */
public class DeviceDetailActivity extends AppCompatActivity {

    public static final String EXTRA_DEVICE = "extra_device";
    private ListView mList;
    private View mEmpty;
    private BluetoothLeDevice mDevice;

    /**
     * 追加广播包信息
     * @param adapter
     * @param title
     * @param record
     */
    private void appendAdRecordView(final MergeAdapter adapter, final String title, final AdRecord record) {
        final LinearLayout lt = (LinearLayout) getLayoutInflater().inflate(R.layout.list_item_view_adrecord, null);
        final TextView tvString = (TextView) lt.findViewById(R.id.data_as_string);
        final TextView tvArray = (TextView) lt.findViewById(R.id.data_as_array);
        final TextView tvTitle = (TextView) lt.findViewById(R.id.title);

        tvTitle.setText(title);
        tvString.setText("'" + AdRecordUtil.getRecordDataAsString(record) + "'");
        tvArray.setText("'" + HexUtil.encodeHexStr(record.getData()) + "'");

        adapter.addView(lt);
    }

    /**
     * 追加设备基础信息
     * @param adapter
     * @param device
     */
    private void appendDeviceInfo(final MergeAdapter adapter, final BluetoothLeDevice device) {
        final LinearLayout lt = (LinearLayout) getLayoutInflater().inflate(R.layout.list_item_view_device_info, null);
        final TextView tvName = (TextView) lt.findViewById(R.id.deviceName);
        final TextView tvAddress = (TextView) lt.findViewById(R.id.deviceAddress);
        final TextView tvClass = (TextView) lt.findViewById(R.id.deviceClass);
        final TextView tvMajorClass = (TextView) lt.findViewById(R.id.deviceMajorClass);
        final TextView tvServices = (TextView) lt.findViewById(R.id.deviceServiceList);
        final TextView tvBondingState = (TextView) lt.findViewById(R.id.deviceBondingState);

        tvName.setText(device.getName());
        tvAddress.setText(device.getAddress());
        tvClass.setText(device.getBluetoothDeviceClassName());
        tvMajorClass.setText(device.getBluetoothDeviceMajorClassName());
        tvBondingState.setText(device.getBluetoothDeviceBondState());

        final String supportedServices;
        if (device.getBluetoothDeviceKnownSupportedServices().isEmpty()) {
            supportedServices = getString(R.string.no_known_services);
        } else {
            final StringBuilder sb = new StringBuilder();

            for (final BluetoothServiceType service : device.getBluetoothDeviceKnownSupportedServices()) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }

                sb.append(service);
            }
            supportedServices = sb.toString();
        }

        tvServices.setText(supportedServices);

        adapter.addView(lt);
    }

    /**
     * 追加信息头
     * @param adapter
     * @param title
     */
    private void appendHeader(final MergeAdapter adapter, final String title) {
        final LinearLayout lt = (LinearLayout) getLayoutInflater().inflate(R.layout.list_item_view_header, null);
        final TextView tvTitle = (TextView) lt.findViewById(R.id.title);
        tvTitle.setText(title);

        adapter.addView(lt);
    }

    /**
     * 追加设备信号信息
     * @param adapter
     * @param device
     */
    private void appendRssiInfo(final MergeAdapter adapter, final BluetoothLeDevice device) {
        final LinearLayout lt = (LinearLayout) getLayoutInflater().inflate(R.layout.list_item_view_rssi_info, null);
        final TextView tvFirstTimestamp = (TextView) lt.findViewById(R.id.firstTimestamp);
        final TextView tvFirstRssi = (TextView) lt.findViewById(R.id.firstRssi);
        final TextView tvLastTimestamp = (TextView) lt.findViewById(R.id.lastTimestamp);
        final TextView tvLastRssi = (TextView) lt.findViewById(R.id.lastRssi);
        final TextView tvRunningAverageRssi = (TextView) lt.findViewById(R.id.runningAverageRssi);

        tvFirstTimestamp.setText(formatTime(device.getFirstTimestamp()));
        tvFirstRssi.setText(formatRssi(device.getFirstRssi()));
        tvLastTimestamp.setText(formatTime(device.getTimestamp()));
        tvLastRssi.setText(formatRssi(device.getRssi()));
        tvRunningAverageRssi.setText(formatRssi(device.getRunningAverageRssi()));

        adapter.addView(lt);
    }

    /**
     * 追加简单信息
     * @param adapter
     * @param data
     */
    private void appendSimpleText(final MergeAdapter adapter, final byte[] data) {
        appendSimpleText(adapter, HexUtil.encodeHexStr(data));
    }

    private void appendSimpleText(final MergeAdapter adapter, final String data) {
        final LinearLayout lt = (LinearLayout) getLayoutInflater().inflate(R.layout.list_item_view_textview, null);
        final TextView tvData = (TextView) lt.findViewById(R.id.data);

        tvData.setText(data);

        adapter.addView(lt);
    }


    private String formatRssi(final double rssi) {
        return getString(R.string.formatter_db, String.valueOf(rssi));
    }

    private String formatRssi(final int rssi) {
        return getString(R.string.formatter_db, String.valueOf(rssi));
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_detail);
        init();
    }

    private void init() {
        mEmpty = findViewById(android.R.id.empty);
        mList = (ListView) findViewById(android.R.id.list);
        mList.setEmptyView(mEmpty);
        mDevice = getIntent().getParcelableExtra(EXTRA_DEVICE);
        pupulateDetails(mDevice);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                if (mDevice == null) return false;
                Intent intent = new Intent(DeviceDetailActivity.this, DeviceControlActivity.class);
                intent.putExtra(DeviceDetailActivity.EXTRA_DEVICE, mDevice);
                startActivity(intent);
                break;
        }
        return true;
    }

    /**
     * 展示设备详细信息
     * @param device 设备信息
     */
    private void pupulateDetails(final BluetoothLeDevice device) {
        final MergeAdapter adapter = new MergeAdapter();
        if (device == null) {
            appendHeader(adapter, getString(R.string.header_device_info));
            appendSimpleText(adapter, getString(R.string.invalid_device_data));
        } else {
            appendHeader(adapter, getString(R.string.header_device_info));
            appendDeviceInfo(adapter, device);

            appendHeader(adapter, getString(R.string.header_rssi_info));
            appendRssiInfo(adapter, device);

            appendHeader(adapter, getString(R.string.header_scan_record));
            appendSimpleText(adapter, device.getScanRecord());

            final Collection<AdRecord> adRecords = device.getAdRecordStore().getRecordsAsCollection();
            if (adRecords.size() > 0) {
                appendHeader(adapter, getString(R.string.header_raw_ad_records));

                for (final AdRecord record : adRecords) {

                    appendAdRecordView(adapter, "#" + record.getType() + " " + record.getHumanReadableType(), record);
                }
            }
        }
        mList.setAdapter(adapter);
    }

    /**
     * 格式化时间
     * @param time
     * @return
     */
    private static String formatTime(final long time) {
        String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS zzz";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(ISO_FORMAT, Locale.CHINA);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return simpleDateFormat.format(new Date(time));
    }

}
