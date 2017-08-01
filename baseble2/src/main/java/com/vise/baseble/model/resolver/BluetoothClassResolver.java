package com.vise.baseble.model.resolver;

import android.bluetooth.BluetoothClass;

/**
 * @Description: 蓝牙设备类别
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/8/7 21:48.
 */
public class BluetoothClassResolver {
    public static String resolveDeviceClass(final int btClass) {
        switch (btClass) {
            case BluetoothClass.Device.AUDIO_VIDEO_CAMCORDER:
                return "A/V, Camcorder";
            case BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO:
                return "A/V, Car Audio";
            case BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE:
                return "A/V, Handsfree";
            case BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES:
                return "A/V, Headphones";
            case BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO:
                return "A/V, HiFi Audio";
            case BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER:
                return "A/V, Loudspeaker";
            case BluetoothClass.Device.AUDIO_VIDEO_MICROPHONE:
                return "A/V, Microphone";
            case BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO:
                return "A/V, Portable Audio";
            case BluetoothClass.Device.AUDIO_VIDEO_SET_TOP_BOX:
                return "A/V, Set Top Box";
            case BluetoothClass.Device.AUDIO_VIDEO_UNCATEGORIZED:
                return "A/V, Uncategorized";
            case BluetoothClass.Device.AUDIO_VIDEO_VCR:
                return "A/V, VCR";
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CAMERA:
                return "A/V, Video Camera";
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CONFERENCING:
                return "A/V, Video Conferencing";
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER:
                return "A/V, Video Display and Loudspeaker";
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY:
                return "A/V, Video Gaming Toy";
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_MONITOR:
                return "A/V, Video Monitor";
            case BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET:
                return "A/V, Video Wearable Headset";
            case BluetoothClass.Device.COMPUTER_DESKTOP:
                return "Computer, Desktop";
            case BluetoothClass.Device.COMPUTER_HANDHELD_PC_PDA:
                return "Computer, Handheld PC/PDA";
            case BluetoothClass.Device.COMPUTER_LAPTOP:
                return "Computer, Laptop";
            case BluetoothClass.Device.COMPUTER_PALM_SIZE_PC_PDA:
                return "Computer, Palm Size PC/PDA";
            case BluetoothClass.Device.COMPUTER_SERVER:
                return "Computer, Server";
            case BluetoothClass.Device.COMPUTER_UNCATEGORIZED:
                return "Computer, Uncategorized";
            case BluetoothClass.Device.COMPUTER_WEARABLE:
                return "Computer, Wearable";
            case BluetoothClass.Device.HEALTH_BLOOD_PRESSURE:
                return "Health, Blood Pressure";
            case BluetoothClass.Device.HEALTH_DATA_DISPLAY:
                return "Health, Data Display";
            case BluetoothClass.Device.HEALTH_GLUCOSE:
                return "Health, Glucose";
            case BluetoothClass.Device.HEALTH_PULSE_OXIMETER:
                return "Health, Pulse Oximeter";
            case BluetoothClass.Device.HEALTH_PULSE_RATE:
                return "Health, Pulse Rate";
            case BluetoothClass.Device.HEALTH_THERMOMETER:
                return "Health, Thermometer";
            case BluetoothClass.Device.HEALTH_UNCATEGORIZED:
                return "Health, Uncategorized";
            case BluetoothClass.Device.HEALTH_WEIGHING:
                return "Health, Weighting";
            case BluetoothClass.Device.PHONE_CELLULAR:
                return "Phone, Cellular";
            case BluetoothClass.Device.PHONE_CORDLESS:
                return "Phone, Cordless";
            case BluetoothClass.Device.PHONE_ISDN:
                return "Phone, ISDN";
            case BluetoothClass.Device.PHONE_MODEM_OR_GATEWAY:
                return "Phone, Modem or Gateway";
            case BluetoothClass.Device.PHONE_SMART:
                return "Phone, Smart";
            case BluetoothClass.Device.PHONE_UNCATEGORIZED:
                return "Phone, Uncategorized";
            case BluetoothClass.Device.TOY_CONTROLLER:
                return "Toy, Controller";
            case BluetoothClass.Device.TOY_DOLL_ACTION_FIGURE:
                return "Toy, Doll/Action Figure";
            case BluetoothClass.Device.TOY_GAME:
                return "Toy, Game";
            case BluetoothClass.Device.TOY_ROBOT:
                return "Toy, Robot";
            case BluetoothClass.Device.TOY_UNCATEGORIZED:
                return "Toy, Uncategorized";
            case BluetoothClass.Device.TOY_VEHICLE:
                return "Toy, Vehicle";
            case BluetoothClass.Device.WEARABLE_GLASSES:
                return "Wearable, Glasses";
            case BluetoothClass.Device.WEARABLE_HELMET:
                return "Wearable, Helmet";
            case BluetoothClass.Device.WEARABLE_JACKET:
                return "Wearable, Jacket";
            case BluetoothClass.Device.WEARABLE_PAGER:
                return "Wearable, Pager";
            case BluetoothClass.Device.WEARABLE_UNCATEGORIZED:
                return "Wearable, Uncategorized";
            case BluetoothClass.Device.WEARABLE_WRIST_WATCH:
                return "Wearable, Wrist Watch";
            default:
                return "Unknown, Unknown (class=" + btClass + ")";
        }
    }

    public static String resolveMajorDeviceClass(final int majorBtClass) {
        switch (majorBtClass) {
            case BluetoothClass.Device.Major.AUDIO_VIDEO:
                return "Audio/ Video";
            case BluetoothClass.Device.Major.COMPUTER:
                return "Computer";
            case BluetoothClass.Device.Major.HEALTH:
                return "Health";
            case BluetoothClass.Device.Major.IMAGING:
                return "Imaging";
            case BluetoothClass.Device.Major.MISC:
                return "Misc";
            case BluetoothClass.Device.Major.NETWORKING:
                return "Networking";
            case BluetoothClass.Device.Major.PERIPHERAL:
                return "Peripheral";
            case BluetoothClass.Device.Major.PHONE:
                return "Phone";
            case BluetoothClass.Device.Major.TOY:
                return "Toy";
            case BluetoothClass.Device.Major.UNCATEGORIZED:
                return "Uncategorized";
            case BluetoothClass.Device.Major.WEARABLE:
                return "Wearable";
            default:
                return "Unknown (" + majorBtClass + ")";
        }
    }
}
