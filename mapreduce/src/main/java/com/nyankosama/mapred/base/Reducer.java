package com.nyankosama.mapred.base;

import java.io.IOException;

/**
 * @created: 2015/3/30
 * @author: nyankosama
 * @description:
 */
public interface Reducer<InKey, InVal, OutKey, OutVal> {
    public void reduce(InKey key, InVal inVal, OutputCollector<OutKey, OutVal> collector) throws IOException;
}
