package com.vise.baseble.common;

/**
 * @Description: 状态描述
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/8/20 17:06.
 */
public enum State {
    SCAN_PROCESS(0x01),
    SCAN_SUCCESS(0x02),
    SCAN_TIMEOUT(0x03),
    CONNECT_PROCESS(0x04),
    CONNECT_SUCCESS(0x05),
    CONNECT_FAILURE(0x06),
    CONNECT_TIMEOUT(0x07),
    DISCONNECT(0x08);

    private int code;

    State(int code){
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
