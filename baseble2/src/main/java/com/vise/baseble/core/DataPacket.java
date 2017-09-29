package com.vise.baseble.core;

/**
 * @Description: 数据包
 * @author: <a href="http://xiaoyaoyou1212.360doc.com">DAWI</a>
 * @date: 2017/9/28 21:11
 */
public class DataPacket {
    private byte[] data;

    public byte[] getData() {
        return data;
    }

    public DataPacket setData(byte[] data) {
        this.data = data;
        return this;
    }
}
