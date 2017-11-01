package com.vise.baseble.core;

import java.util.LinkedHashMap;

/**
 * @Description: Lru算法实现的HashMap
 * @author: <a href="http://xiaoyaoyou1212.360doc.com">DAWI</a>
 * @date: 2017/9/28 19:57
 */
public class LruHashMap<K, V> extends LinkedHashMap<K, V> {
    private final int MAX_SAVE_SIZE;

    public LruHashMap(int saveSize) {
        super((int) Math.ceil(saveSize / 0.75) + 1, 0.75f, true);
        MAX_SAVE_SIZE = saveSize;
    }

    @Override
    protected boolean removeEldestEntry(Entry eldest) {
        if (size() > MAX_SAVE_SIZE && eldest.getValue() instanceof DeviceMirror) {
            ((DeviceMirror) eldest.getValue()).disconnect();
        }
        return size() > MAX_SAVE_SIZE;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Entry<K, V> entry : entrySet()) {
            sb.append(String.format("%s:%s ", entry.getKey(), entry.getValue()));
        }
        return sb.toString();
    }
}
