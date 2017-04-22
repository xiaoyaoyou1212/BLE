package com.vise.baseble.utils;

import android.util.SparseArray;

import com.vise.baseble.model.adrecord.AdRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description: 广播包解析工具类
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/8/7 21:56.
 */
public class AdRecordUtil {
    private AdRecordUtil() {
        // TO AVOID INSTANTIATION
    }

    public static String getRecordDataAsString(final AdRecord nameRecord) {
        if (nameRecord == null) {
            return "";
        }
        return new String(nameRecord.getData());
    }

    public static byte[] getServiceData(final AdRecord serviceData) {
        if (serviceData == null) {
            return null;
        }
        if (serviceData.getType() != AdRecord.BLE_GAP_AD_TYPE_SERVICE_DATA) return null;

        final byte[] raw = serviceData.getData();
        //Chop out the uuid
        return Arrays.copyOfRange(raw, 2, raw.length);
    }

    public static int getServiceDataUuid(final AdRecord serviceData) {
        if (serviceData == null) {
            return -1;
        }
        if (serviceData.getType() != AdRecord.BLE_GAP_AD_TYPE_SERVICE_DATA) return -1;

        final byte[] raw = serviceData.getData();
        //Find UUID data in byte array
        int uuid = (raw[1] & 0xFF) << 8;
        uuid += (raw[0] & 0xFF);

        return uuid;
    }

    /*
     * Read out all the AD structures from the raw scan record
     */
    public static List<AdRecord> parseScanRecordAsList(final byte[] scanRecord) {
        final List<AdRecord> records = new ArrayList<>();

        int index = 0;
        while (index < scanRecord.length) {
            final int length = scanRecord[index++];
            //Done once we run out of records
            if (length == 0) break;

            final int type = ConvertUtil.getIntFromByte(scanRecord[index]);

            //Done if our record isn't a valid type
            if (type == 0) break;

            final byte[] data = Arrays.copyOfRange(scanRecord, index + 1, index + length);

            records.add(new AdRecord(length, type, data));

            //Advance
            index += length;
        }

        return Collections.unmodifiableList(records);
    }

    public static Map<Integer, AdRecord> parseScanRecordAsMap(final byte[] scanRecord) {
        final Map<Integer, AdRecord> records = new HashMap<>();

        int index = 0;
        while (index < scanRecord.length) {
            final int length = scanRecord[index++];
            //Done once we run out of records
            if (length == 0) break;

            final int type = ConvertUtil.getIntFromByte(scanRecord[index]);

            //Done if our record isn't a valid type
            if (type == 0) break;

            final byte[] data = Arrays.copyOfRange(scanRecord, index + 1, index + length);

            records.put(type, new AdRecord(length, type, data));

            //Advance
            index += length;
        }

        return Collections.unmodifiableMap(records);
    }

    public static SparseArray<AdRecord> parseScanRecordAsSparseArray(final byte[] scanRecord) {
        final SparseArray<AdRecord> records = new SparseArray<>();

        int index = 0;
        while (index < scanRecord.length) {
            final int length = scanRecord[index++];
            //Done once we run out of records
            if (length == 0) break;

            final int type = ConvertUtil.getIntFromByte(scanRecord[index]);

            //Done if our record isn't a valid type
            if (type == 0) break;

            final byte[] data = Arrays.copyOfRange(scanRecord, index + 1, index + length);

            records.put(type, new AdRecord(length, type, data));

            //Advance
            index += length;
        }

        return records;
    }
}
