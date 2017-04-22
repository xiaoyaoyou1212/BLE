package com.vise.baseble.model;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.vise.baseble.common.BluetoothServiceType;
import com.vise.baseble.model.adrecord.AdRecordStore;
import com.vise.baseble.model.resolver.BluetoothClassResolver;
import com.vise.baseble.utils.AdRecordUtil;
import com.vise.baseble.utils.HexUtil;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @Description: 设备信息
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/8/5 20:44.
 */
public class BluetoothLeDevice implements Parcelable {

    /**
     * The Constant CREATOR.
     */
    public static final Parcelable.Creator<BluetoothLeDevice> CREATOR = new Parcelable.Creator<BluetoothLeDevice>() {
        public BluetoothLeDevice createFromParcel(final Parcel in) {
            return new BluetoothLeDevice(in);
        }

        public BluetoothLeDevice[] newArray(final int size) {
            return new BluetoothLeDevice[size];
        }
    };
    protected static final int MAX_RSSI_LOG_SIZE = 10;
    private static final String PARCEL_EXTRA_BLUETOOTH_DEVICE = "bluetooth_device";
    private static final String PARCEL_EXTRA_CURRENT_RSSI = "current_rssi";
    private static final String PARCEL_EXTRA_CURRENT_TIMESTAMP = "current_timestamp";
    private static final String PARCEL_EXTRA_DEVICE_RSSI_LOG = "device_rssi_log";
    private static final String PARCEL_EXTRA_DEVICE_SCANRECORD = "device_scanrecord";
    private static final String PARCEL_EXTRA_DEVICE_SCANRECORD_STORE = "device_scanrecord_store";
    private static final String PARCEL_EXTRA_FIRST_RSSI = "device_first_rssi";
    private static final String PARCEL_EXTRA_FIRST_TIMESTAMP = "first_timestamp";
    private static final long LOG_INVALIDATION_THRESHOLD = 10 * 1000;
    private final AdRecordStore mRecordStore;
    private final BluetoothDevice mDevice;
    private final Map<Long, Integer> mRssiLog;
    private final byte[] mScanRecord;
    private final int mFirstRssi;
    private final long mFirstTimestamp;
    private int mCurrentRssi;
    private long mCurrentTimestamp;
    private transient Set<BluetoothServiceType> mServiceSet;

    /**
     * Instantiates a new Bluetooth LE device.
     *
     * @param device     a standard android Bluetooth device
     * @param rssi       the RSSI value of the Bluetooth device
     * @param scanRecord the scan record of the device
     * @param timestamp  the timestamp of the RSSI reading
     */
    public BluetoothLeDevice(final BluetoothDevice device, final int rssi, final byte[] scanRecord, final long timestamp) {
        mDevice = device;
        mFirstRssi = rssi;
        mFirstTimestamp = timestamp;
        mRecordStore = new AdRecordStore(AdRecordUtil.parseScanRecordAsSparseArray(scanRecord));
        mScanRecord = scanRecord;
        mRssiLog = new LinkedHashMap<>(MAX_RSSI_LOG_SIZE);
        updateRssiReading(timestamp, rssi);
    }

    /**
     * Instantiates a new Bluetooth LE device.
     *
     * @param device the device
     */
    public BluetoothLeDevice(final BluetoothLeDevice device) {
        mCurrentRssi = device.getRssi();
        mCurrentTimestamp = device.getTimestamp();
        mDevice = device.getDevice();
        mFirstRssi = device.getFirstRssi();
        mFirstTimestamp = device.getFirstTimestamp();
        mRecordStore = new AdRecordStore(AdRecordUtil.parseScanRecordAsSparseArray(device.getScanRecord()));
        mRssiLog = device.getRssiLog();
        mScanRecord = device.getScanRecord();
    }

    /**
     * Instantiates a new bluetooth le device.
     *
     * @param in the in
     */
    protected BluetoothLeDevice(final Parcel in) {
        final Bundle b = in.readBundle(getClass().getClassLoader());

        mCurrentRssi = b.getInt(PARCEL_EXTRA_CURRENT_RSSI, 0);
        mCurrentTimestamp = b.getLong(PARCEL_EXTRA_CURRENT_TIMESTAMP, 0);
        mDevice = b.getParcelable(PARCEL_EXTRA_BLUETOOTH_DEVICE);
        mFirstRssi = b.getInt(PARCEL_EXTRA_FIRST_RSSI, 0);
        mFirstTimestamp = b.getLong(PARCEL_EXTRA_FIRST_TIMESTAMP, 0);
        mRecordStore = b.getParcelable(PARCEL_EXTRA_DEVICE_SCANRECORD_STORE);
        mRssiLog = (Map<Long, Integer>) b.getSerializable(PARCEL_EXTRA_DEVICE_RSSI_LOG);
        mScanRecord = b.getByteArray(PARCEL_EXTRA_DEVICE_SCANRECORD);
    }

    /**
     * Adds the to rssi log.
     *
     * @param timestamp   the timestamp
     * @param rssiReading the rssi reading
     */
    private void addToRssiLog(final long timestamp, final int rssiReading) {
        synchronized (mRssiLog) {
            if (timestamp - mCurrentTimestamp > LOG_INVALIDATION_THRESHOLD) {
                mRssiLog.clear();
            }

            mCurrentRssi = rssiReading;
            mCurrentTimestamp = timestamp;
            mRssiLog.put(timestamp, rssiReading);
        }
    }

    /* (non-Javadoc)
     * @see android.os.Parcelable#describeContents()
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final BluetoothLeDevice other = (BluetoothLeDevice) obj;
        if (mCurrentRssi != other.mCurrentRssi) return false;
        if (mCurrentTimestamp != other.mCurrentTimestamp) return false;
        if (mDevice == null) {
            if (other.mDevice != null) return false;
        } else if (!mDevice.equals(other.mDevice)) return false;
        if (mFirstRssi != other.mFirstRssi) return false;
        if (mFirstTimestamp != other.mFirstTimestamp) return false;
        if (mRecordStore == null) {
            if (other.mRecordStore != null) return false;
        } else if (!mRecordStore.equals(other.mRecordStore)) return false;
        if (mRssiLog == null) {
            if (other.mRssiLog != null) return false;
        } else if (!mRssiLog.equals(other.mRssiLog)) return false;
        if (!Arrays.equals(mScanRecord, other.mScanRecord)) return false;
        return true;
    }

    /**
     * Gets the ad record store.
     *
     * @return the ad record store
     */
    public AdRecordStore getAdRecordStore() {
        return mRecordStore;
    }

    /**
     * Gets the address.
     *
     * @return the address
     */
    public String getAddress() {
        return mDevice.getAddress();
    }

    /**
     * Gets the bluetooth device bond state.
     *
     * @return the bluetooth device bond state
     */
    public String getBluetoothDeviceBondState() {
        return resolveBondingState(mDevice.getBondState());
    }

    /**
     * Gets the bluetooth device class name.
     *
     * @return the bluetooth device class name
     */
    public String getBluetoothDeviceClassName() {
        return BluetoothClassResolver.resolveDeviceClass(mDevice.getBluetoothClass().getDeviceClass());
    }

    public Set<BluetoothServiceType> getBluetoothDeviceKnownSupportedServices() {
        if (mServiceSet == null) {
            synchronized (this) {
                if (mServiceSet == null) {
                    final Set<BluetoothServiceType> serviceSet = new HashSet<>();
                    for (final BluetoothServiceType service : BluetoothServiceType.values()) {

                        if (mDevice.getBluetoothClass().hasService(service.getCode())) {
                            serviceSet.add(service);
                        }
                    }
                    mServiceSet = Collections.unmodifiableSet(serviceSet);
                }
            }
        }

        return mServiceSet;
    }

    /**
     * Gets the bluetooth device major class name.
     *
     * @return the bluetooth device major class name
     */
    public String getBluetoothDeviceMajorClassName() {
        return BluetoothClassResolver.resolveMajorDeviceClass(mDevice.getBluetoothClass().getMajorDeviceClass());
    }

    /**
     * Gets the device.
     *
     * @return the device
     */
    public BluetoothDevice getDevice() {
        return mDevice;
    }

    /**
     * Gets the first rssi.
     *
     * @return the first rssi
     */
    public int getFirstRssi() {
        return mFirstRssi;
    }

    /**
     * Gets the first timestamp.
     *
     * @return the first timestamp
     */
    public long getFirstTimestamp() {
        return mFirstTimestamp;
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return mDevice.getName();
    }

    /**
     * Gets the rssi.
     *
     * @return the rssi
     */
    public int getRssi() {
        return mCurrentRssi;
    }

    /**
     * Gets the rssi log.
     *
     * @return the rssi log
     */
    protected Map<Long, Integer> getRssiLog() {
        synchronized (mRssiLog) {
            return mRssiLog;
        }
    }

    /**
     * Gets the running average rssi.
     *
     * @return the running average rssi
     */
    public double getRunningAverageRssi() {
        int sum = 0;
        int count = 0;

        synchronized (mRssiLog) {

            for (final Long aLong : mRssiLog.keySet()) {
                count++;
                sum += mRssiLog.get(aLong);
            }
        }

        if (count > 0) {
            return sum / count;
        } else {
            return 0;
        }

    }

    /**
     * Gets the scan record.
     *
     * @return the scan record
     */
    public byte[] getScanRecord() {
        return mScanRecord;
    }

    /**
     * Gets the timestamp.
     *
     * @return the timestamp
     */
    public long getTimestamp() {
        return mCurrentTimestamp;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + mCurrentRssi;
        result = prime * result + (int) (mCurrentTimestamp ^ (mCurrentTimestamp >>> 32));
        result = prime * result + ((mDevice == null) ? 0 : mDevice.hashCode());
        result = prime * result + mFirstRssi;
        result = prime * result + (int) (mFirstTimestamp ^ (mFirstTimestamp >>> 32));
        result = prime * result + ((mRecordStore == null) ? 0 : mRecordStore.hashCode());
        result = prime * result + ((mRssiLog == null) ? 0 : mRssiLog.hashCode());
        result = prime * result + Arrays.hashCode(mScanRecord);
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "BluetoothLeDevice [mDevice=" + mDevice + ", " +
                "mRssi=" + mFirstRssi + ", mScanRecord=" + HexUtil.encodeHexStr(mScanRecord) +
                ", mRecordStore=" + mRecordStore + ", getBluetoothDeviceBondState()=" +
                getBluetoothDeviceBondState() + ", getBluetoothDeviceClassName()=" +
                getBluetoothDeviceClassName() + "]";
    }

    /**
     * Update rssi reading.
     *
     * @param timestamp   the timestamp
     * @param rssiReading the rssi reading
     */
    public void updateRssiReading(final long timestamp, final int rssiReading) {
        addToRssiLog(timestamp, rssiReading);
    }

    /* (non-Javadoc)
     * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
     */
    @Override
    public void writeToParcel(final Parcel parcel, final int arg1) {
        final Bundle b = new Bundle(getClass().getClassLoader());

        b.putByteArray(PARCEL_EXTRA_DEVICE_SCANRECORD, mScanRecord);

        b.putInt(PARCEL_EXTRA_FIRST_RSSI, mFirstRssi);
        b.putInt(PARCEL_EXTRA_CURRENT_RSSI, mCurrentRssi);

        b.putLong(PARCEL_EXTRA_FIRST_TIMESTAMP, mFirstTimestamp);
        b.putLong(PARCEL_EXTRA_CURRENT_TIMESTAMP, mCurrentTimestamp);

        b.putParcelable(PARCEL_EXTRA_BLUETOOTH_DEVICE, mDevice);
        b.putParcelable(PARCEL_EXTRA_DEVICE_SCANRECORD_STORE, mRecordStore);
        b.putSerializable(PARCEL_EXTRA_DEVICE_RSSI_LOG, (Serializable) mRssiLog);

        parcel.writeBundle(b);
    }

    /**
     * Resolve bonding state.
     *
     * @param bondState the bond state
     * @return the string
     */
    private static String resolveBondingState(final int bondState) {
        switch (bondState) {
            case BluetoothDevice.BOND_BONDED://已配对
                return "Paired";
            case BluetoothDevice.BOND_BONDING://配对中
                return "Pairing";
            case BluetoothDevice.BOND_NONE://未配对
                return "UnBonded";
            default:
                return "Unknown";//未知状态
        }
    }
}
