package com.vise.baseble.common;

/**
 * @Description: 属性类型
 * @author: <a href="http://xiaoyaoyou1212.360doc.com">DAWI</a>
 * @date: 2017/10/17 20:27
 */
public enum PropertyType {
    PROPERTY_READ(0x01),
    PROPERTY_WRITE(0x02),
    PROPERTY_NOTIFY(0x04),
    PROPERTY_INDICATE(0x08);

    private int propertyValue;

    PropertyType(int propertyValue) {
        this.propertyValue = propertyValue;
    }

    public int getPropertyValue() {
        return propertyValue;
    }
}
