package com.nyankosama.mapred.base;

import java.io.IOException;

/**
 * @created: 2015/3/30
 * @author: nyankosama
 * @description:
 */
public interface OutputCollector<OutKey, OutVal> {
    public void collect(OutKey key, OutVal val) throws IOException;
}
