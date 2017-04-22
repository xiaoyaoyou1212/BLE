package com.vise.baseble.model.adrecord;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;

/**
 * @Description: 广播包解析model
 * 参考：https://www.bluetooth.com/zh-cn/specifications/assigned-numbers/generic-access-profile
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/8/7 21:53.
 */
public class AdRecord implements Parcelable {

    public static final int BLE_GAP_AD_TYPE_FLAGS = 0x01;//< Flags for discoverAbility.
    public static final int BLE_GAP_AD_TYPE_16BIT_SERVICE_UUID_MORE_AVAILABLE = 0x02;//< Partial list of 16 bit service UUIDs.
    public static final int BLE_GAP_AD_TYPE_16BIT_SERVICE_UUID_COMPLETE = 0x03;//< Complete list of 16 bit service UUIDs.
    public static final int BLE_GAP_AD_TYPE_32BIT_SERVICE_UUID_MORE_AVAILABLE = 0x04;//< Partial list of 32 bit service UUIDs.
    public static final int BLE_GAP_AD_TYPE_32BIT_SERVICE_UUID_COMPLETE = 0x05;//< Complete list of 32 bit service UUIDs.
    public static final int BLE_GAP_AD_TYPE_128BIT_SERVICE_UUID_MORE_AVAILABLE = 0x06;//< Partial list of 128 bit service UUIDs.
    public static final int BLE_GAP_AD_TYPE_128BIT_SERVICE_UUID_COMPLETE = 0x07;//< Complete list of 128 bit service UUIDs.
    public static final int BLE_GAP_AD_TYPE_SHORT_LOCAL_NAME = 0x08;//< Short local device name.
    public static final int BLE_GAP_AD_TYPE_COMPLETE_LOCAL_NAME = 0x09;//< Complete local device name.
    public static final int BLE_GAP_AD_TYPE_TX_POWER_LEVEL = 0x0A;//< Transmit power level.
    public static final int BLE_GAP_AD_TYPE_CLASS_OF_DEVICE = 0x0D;//< Class of device.
    public static final int BLE_GAP_AD_TYPE_SIMPLE_PAIRING_HASH_C = 0x0E;//< Simple Pairing Hash C.
    public static final int BLE_GAP_AD_TYPE_SIMPLE_PAIRING_RANDOMIZER_R = 0x0F;//< Simple Pairing Randomizer R.
    public static final int BLE_GAP_AD_TYPE_SECURITY_MANAGER_TK_VALUE = 0x10;//< Security Manager TK Value.
    public static final int BLE_GAP_AD_TYPE_SECURITY_MANAGER_OOB_FLAGS = 0x11;//< Security Manager Out Of Band Flags.
    public static final int BLE_GAP_AD_TYPE_SLAVE_CONNECTION_INTERVAL_RANGE = 0x12;//< Slave Connection Interval Range.
    public static final int BLE_GAP_AD_TYPE_SOLICITED_SERVICE_UUIDS_16BIT = 0x14;//< List of 16-bit Service Solicitation UUIDs.
    public static final int BLE_GAP_AD_TYPE_SOLICITED_SERVICE_UUIDS_128BIT = 0x15;//< List of 128-bit Service Solicitation UUIDs.
    public static final int BLE_GAP_AD_TYPE_SERVICE_DATA = 0x16;//< Service Data - 16-bit UUID.
    public static final int BLE_GAP_AD_TYPE_PUBLIC_TARGET_ADDRESS = 0x17;//< Public Target Address.
    public static final int BLE_GAP_AD_TYPE_RANDOM_TARGET_ADDRESS = 0x18;//< Random Target Address.
    public static final int BLE_GAP_AD_TYPE_APPEARANCE = 0x19;//< Appearance.
    public static final int BLE_GAP_AD_TYPE_ADVERTISING_INTERVAL = 0x1A;//< Advertising Interval.
    public static final int BLE_GAP_AD_TYPE_LE_BLUETOOTH_DEVICE_ADDRESS = 0x1B;//< LE Bluetooth Device Address.
    public static final int BLE_GAP_AD_TYPE_LE_ROLE = 0x1C;//< LE Role.
    public static final int BLE_GAP_AD_TYPE_SIMPLE_PAIRING_HASH_C256 = 0x1D;//< Simple Pairing Hash C-256.
    public static final int BLE_GAP_AD_TYPE_SIMPLE_PAIRING_RANDOMIZER_R256 = 0x1E;//< Simple Pairing Randomizer R-256.
    public static final int BLE_GAP_AD_TYPE_SERVICE_DATA_32BIT_UUID = 0x20;//< Service Data - 32-bit UUID.
    public static final int BLE_GAP_AD_TYPE_SERVICE_DATA_128BIT_UUID = 0x21;//< Service Data - 128-bit UUID.
    public static final int BLE_GAP_AD_TYPE_3D_INFORMATION_DATA = 0x3D;//< 3D Information Data.
    public static final int BLE_GAP_AD_TYPE_MANUFACTURER_SPECIFIC_DATA = 0xFF;//< Manufacturer Specific Data.

    public static final Parcelable.Creator<AdRecord> CREATOR = new Parcelable.Creator<AdRecord>() {
        public AdRecord createFromParcel(final Parcel in) {
            return new AdRecord(in);
        }

        public AdRecord[] newArray(final int size) {
            return new AdRecord[size];
        }
    };
    private static final String PARCEL_RECORD_DATA = "record_data";
    private static final String PARCEL_RECORD_TYPE = "record_type";
    private static final String PARCEL_RECORD_LENGTH = "record_length";
    /* Model Object Definition */
    private final int mLength;
    private final int mType;
    private final byte[] mData;

    public AdRecord(final int length, final int type, final byte[] data) {
        mLength = length;
        mType = type;
        mData = data;
    }

    public AdRecord(final Parcel in) {
        final Bundle b = in.readBundle(getClass().getClassLoader());
        mLength = b.getInt(PARCEL_RECORD_LENGTH);
        mType = b.getInt(PARCEL_RECORD_TYPE);
        mData = b.getByteArray(PARCEL_RECORD_DATA);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public byte[] getData() {
        return mData;
    }

    public String getHumanReadableType() {
        return getHumanReadableAdType(mType);
    }

    public int getLength() {
        return mLength;
    }

    public int getType() {
        return mType;
    }

    @Override
    public String toString() {
        return "AdRecord [mLength=" + mLength + ", mType=" + mType + ", mData=" + Arrays.toString(mData) + ", getHumanReadableType()=" +
                getHumanReadableType() + "]";
    }

    @Override
    public void writeToParcel(final Parcel parcel, final int arg1) {
        final Bundle b = new Bundle(getClass().getClassLoader());

        b.putInt(PARCEL_RECORD_LENGTH, mLength);
        b.putInt(PARCEL_RECORD_TYPE, mType);
        b.putByteArray(PARCEL_RECORD_DATA, mData);

        parcel.writeBundle(b);
    }

    private static String getHumanReadableAdType(final int type) {
        switch (type) {
            case BLE_GAP_AD_TYPE_FLAGS:
                return "Flags for discoverAbility.";
            case BLE_GAP_AD_TYPE_16BIT_SERVICE_UUID_MORE_AVAILABLE:
                return "Partial list of 16 bit service UUIDs.";
            case BLE_GAP_AD_TYPE_16BIT_SERVICE_UUID_COMPLETE:
                return "Complete list of 16 bit service UUIDs.";
            case BLE_GAP_AD_TYPE_32BIT_SERVICE_UUID_MORE_AVAILABLE:
                return "Partial list of 32 bit service UUIDs.";
            case BLE_GAP_AD_TYPE_32BIT_SERVICE_UUID_COMPLETE:
                return "Complete list of 32 bit service UUIDs.";
            case BLE_GAP_AD_TYPE_128BIT_SERVICE_UUID_MORE_AVAILABLE:
                return "Partial list of 128 bit service UUIDs.";
            case BLE_GAP_AD_TYPE_128BIT_SERVICE_UUID_COMPLETE:
                return "Complete list of 128 bit service UUIDs.";
            case BLE_GAP_AD_TYPE_SHORT_LOCAL_NAME:
                return "Short local device name.";
            case BLE_GAP_AD_TYPE_COMPLETE_LOCAL_NAME:
                return "Complete local device name.";
            case BLE_GAP_AD_TYPE_TX_POWER_LEVEL:
                return "Transmit power level.";
            case BLE_GAP_AD_TYPE_CLASS_OF_DEVICE:
                return "Class of device.";
            case BLE_GAP_AD_TYPE_SIMPLE_PAIRING_HASH_C:
                return "Simple Pairing Hash C.";
            case BLE_GAP_AD_TYPE_SIMPLE_PAIRING_RANDOMIZER_R:
                return "Simple Pairing Randomizer R.";
            case BLE_GAP_AD_TYPE_SECURITY_MANAGER_TK_VALUE:
                return "Security Manager TK Value.";
            case BLE_GAP_AD_TYPE_SECURITY_MANAGER_OOB_FLAGS:
                return "Security Manager Out Of Band Flags.";
            case BLE_GAP_AD_TYPE_SLAVE_CONNECTION_INTERVAL_RANGE:
                return "Slave Connection Interval Range.";
            case BLE_GAP_AD_TYPE_SOLICITED_SERVICE_UUIDS_16BIT:
                return "List of 16-bit Service Solicitation UUIDs.";
            case BLE_GAP_AD_TYPE_SOLICITED_SERVICE_UUIDS_128BIT:
                return "List of 128-bit Service Solicitation UUIDs.";
            case BLE_GAP_AD_TYPE_SERVICE_DATA:
                return "Service Data - 16-bit UUID.";
            case BLE_GAP_AD_TYPE_PUBLIC_TARGET_ADDRESS:
                return "Public Target Address.";
            case BLE_GAP_AD_TYPE_RANDOM_TARGET_ADDRESS:
                return "Random Target Address.";
            case BLE_GAP_AD_TYPE_APPEARANCE:
                return "Appearance.";
            case BLE_GAP_AD_TYPE_ADVERTISING_INTERVAL:
                return "Advertising Interval.";
            case BLE_GAP_AD_TYPE_LE_BLUETOOTH_DEVICE_ADDRESS:
                return "LE Bluetooth Device Address.";
            case BLE_GAP_AD_TYPE_LE_ROLE:
                return "LE Role.";
            case BLE_GAP_AD_TYPE_SIMPLE_PAIRING_HASH_C256:
                return "Simple Pairing Hash C-256.";
            case BLE_GAP_AD_TYPE_SIMPLE_PAIRING_RANDOMIZER_R256:
                return "Simple Pairing Randomizer R-256.";
            case BLE_GAP_AD_TYPE_SERVICE_DATA_32BIT_UUID:
                return "Service Data - 32-bit UUID.";
            case BLE_GAP_AD_TYPE_SERVICE_DATA_128BIT_UUID:
                return "Service Data - 128-bit UUID.";
            case BLE_GAP_AD_TYPE_3D_INFORMATION_DATA:
                return "3D Information Data.";
            case BLE_GAP_AD_TYPE_MANUFACTURER_SPECIFIC_DATA:
                return "Manufacturer Specific Data.";
            default:
                return "Unknown AdRecord Structure: " + type;
        }
    }
}
