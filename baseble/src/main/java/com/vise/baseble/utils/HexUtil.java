package com.vise.baseble.utils;

import com.vise.log.ViseLog;

/**
 * @Description: 十六进制转换类
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/8/7 21:57.
 */
public class HexUtil {
    /**
     * 用于建立十六进制字符的输出的小写字符数组
     */
    private static final char[] DIGITS_LOWER = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * 用于建立十六进制字符的输出的大写字符数组
     */
    private static final char[] DIGITS_UPPER = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    /**
     * 将字节数组转换为十六进制字符数组
     *
     * @param data byte[]
     * @return 十六进制char[]
     */
    public static char[] encodeHex(byte[] data) {
        return encodeHex(data, true);
    }

    /**
     * 将字节数组转换为十六进制字符数组
     *
     * @param data        byte[]
     * @param toLowerCase <code>true</code> 传换成小写格式 ， <code>false</code> 传换成大写格式
     * @return 十六进制char[]
     */
    public static char[] encodeHex(byte[] data, boolean toLowerCase) {
        return encodeHex(data, toLowerCase ? DIGITS_LOWER : DIGITS_UPPER);
    }

    /**
     * 将字节数组转换为十六进制字符数组
     *
     * @param data     byte[]
     * @param toDigits 用于控制输出的char[]
     * @return 十六进制char[]
     */
    protected static char[] encodeHex(byte[] data, char[] toDigits) {
        int l = data.length;
        char[] out = new char[l << 1];
        // two characters form the hex value.
        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = toDigits[(0xF0 & data[i]) >>> 4];
            out[j++] = toDigits[0x0F & data[i]];
        }
        return out;
    }

    /**
     * 将字节数组转换为十六进制字符串
     *
     * @param data byte[]
     * @return 十六进制String
     */
    public static String encodeHexStr(byte[] data) {
        return encodeHexStr(data, true);
    }

    /**
     * 将字节数组转换为十六进制字符串
     *
     * @param data        byte[]
     * @param toLowerCase <code>true</code> 传换成小写格式 ， <code>false</code> 传换成大写格式
     * @return 十六进制String
     */
    public static String encodeHexStr(byte[] data, boolean toLowerCase) {
        return encodeHexStr(data, toLowerCase ? DIGITS_LOWER : DIGITS_UPPER);
    }

    /**
     * 将字节数组转换为十六进制字符串
     *
     * @param data     byte[]
     * @param toDigits 用于控制输出的char[]
     * @return 十六进制String
     */
    protected static String encodeHexStr(byte[] data, char[] toDigits) {
        if (data == null) {
            ViseLog.e("this data is null.");
            return "";
        }
        return new String(encodeHex(data, toDigits));
    }

    /**
     * 将十六进制字符串转换为字节数组
     *
     * @param data
     * @return
     */
    public static byte[] decodeHex(String data) {
        if (data == null) {
            ViseLog.e("this data is null.");
            return new byte[0];
        }
        return decodeHex(data.toCharArray());
    }

    /**
     * 将十六进制字符数组转换为字节数组
     *
     * @param data 十六进制char[]
     * @return byte[]
     * @throws RuntimeException 如果源十六进制字符数组是一个奇怪的长度，将抛出运行时异常
     */
    public static byte[] decodeHex(char[] data) {

        int len = data.length;

        if ((len & 0x01) != 0) {
            throw new RuntimeException("Odd number of characters.");
        }

        byte[] out = new byte[len >> 1];

        // two characters form the hex value.
        for (int i = 0, j = 0; j < len; i++) {
            int f = toDigit(data[j], j) << 4;
            j++;
            f = f | toDigit(data[j], j);
            j++;
            out[i] = (byte) (f & 0xFF);
        }

        return out;
    }

    /**
     * 将十六进制字符转换成一个整数
     *
     * @param ch    十六进制char
     * @param index 十六进制字符在字符数组中的位置
     * @return 一个整数
     * @throws RuntimeException 当ch不是一个合法的十六进制字符时，抛出运行时异常
     */
    protected static int toDigit(char ch, int index) {
        int digit = Character.digit(ch, 16);
        if (digit == -1) {
            throw new RuntimeException("Illegal hexadecimal character " + ch + " at index " + index);
        }
        return digit;
    }

    /**
     * 截取字节数组
     *
     * @param src   byte []  数组源  这里填16进制的 数组
     * @param begin 起始位置 源数组的起始位置。0位置有效
     * @param count 截取长度
     * @return
     */
    public static byte[] subBytes(byte[] src, int begin, int count) {
        byte[] bs = new byte[count];
        System.arraycopy(src, begin, bs, 0, count);  // bs 目的数组  0 截取后存放的数值起始位置。0位置有效
        return bs;
    }

    /**
     * int转byte数组
     *
     * @param bb
     * @param x
     * @param index 第几位开始
     * @param flag 标识高低位顺序，高位在前为true，低位在前为false
     */
    public static void intToByte(byte[] bb, int x, int index, boolean flag) {
        if (flag) {
            bb[index + 0] = (byte) (x >> 24);
            bb[index + 1] = (byte) (x >> 16);
            bb[index + 2] = (byte) (x >> 8);
            bb[index + 3] = (byte) (x >> 0);
        } else {
            bb[index + 3] = (byte) (x >> 24);
            bb[index + 2] = (byte) (x >> 16);
            bb[index + 1] = (byte) (x >> 8);
            bb[index + 0] = (byte) (x >> 0);
        }
    }

    /**
     * byte数组转int
     *
     * @param bb
     * @param index 第几位开始
     * @param flag 标识高低位顺序，高位在前为true，低位在前为false
     * @return
     */
    public static int byteToInt(byte[] bb, int index, boolean flag) {
        if (flag) {
            return (int) ((((bb[index + 0] & 0xff) << 24)
                    | ((bb[index + 1] & 0xff) << 16)
                    | ((bb[index + 2] & 0xff) << 8)
                    | ((bb[index + 3] & 0xff) << 0)));
        } else {
            return (int) ((((bb[index + 3] & 0xff) << 24)
                    | ((bb[index + 2] & 0xff) << 16)
                    | ((bb[index + 1] & 0xff) << 8)
                    | ((bb[index + 0] & 0xff) << 0)));
        }
    }


    /**
     * 字节数组逆序
     *
     * @param data
     * @return
     */
    public static byte[] reverse(byte[] data) {
        byte[] reverseData = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            reverseData[i] = data[data.length - 1 - i];
        }
        return reverseData;
    }

    /**
     * 蓝牙传输 16进制 高低位 读数的 转换
     *
     * @param data 截取数据源，字节数组
     * @param index 截取数据开始位置
     * @param count 截取数据长度，只能为2、4、8个字节
     * @param flag 标识高低位顺序，高位在前为true，低位在前为false
     * @return
     */
    public static long byteToLong(byte[] data, int index, int count, boolean flag) {
        long lg = 0;
        if (flag) {
            switch (count) {
                case 2:
                    lg = ((((long) data[index + 0] & 0xff) << 8)
                            | (((long) data[index + 1] & 0xff) << 0));
                    break;

                case 4:
                    lg = ((((long) data[index + 0] & 0xff) << 24)
                            | (((long) data[index + 1] & 0xff) << 16)
                            | (((long) data[index + 2] & 0xff) << 8)
                            | (((long) data[index + 3] & 0xff) << 0));
                    break;

                case 8:
                    lg = ((((long) data[index + 0] & 0xff) << 56)
                            | (((long) data[index + 1] & 0xff) << 48)
                            | (((long) data[index + 2] & 0xff) << 40)
                            | (((long) data[index + 3] & 0xff) << 32)
                            | (((long) data[index + 4] & 0xff) << 24)
                            | (((long) data[index + 5] & 0xff) << 16)
                            | (((long) data[index + 6] & 0xff) << 8)
                            | (((long) data[index + 7] & 0xff) << 0));
                    break;
            }
            return lg;
        } else {
            switch (count) {
                case 2:
                    lg = ((((long) data[index + 1] & 0xff) << 8)
                            | (((long) data[index + 0] & 0xff) << 0));
                    break;
                case 4:
                    lg = ((((long) data[index + 3] & 0xff) << 24)
                            | (((long) data[index + 2] & 0xff) << 16)
                            | (((long) data[index + 1] & 0xff) << 8)
                            | (((long) data[index + 0] & 0xff) << 0));
                    break;
                case 8:
                    lg = ((((long) data[index + 7] & 0xff) << 56)
                            | (((long) data[index + 6] & 0xff) << 48)
                            | (((long) data[index + 5] & 0xff) << 40)
                            | (((long) data[index + 4] & 0xff) << 32)
                            | (((long) data[index + 3] & 0xff) << 24)
                            | (((long) data[index + 2] & 0xff) << 16)
                            | (((long) data[index + 1] & 0xff) << 8)
                            | (((long) data[index + 0] & 0xff) << 0));
                    break;
            }
            return lg;
        }
    }

}
