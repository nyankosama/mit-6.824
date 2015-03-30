package com.nyankosama.mapred.base;

import java.io.IOException;

/**
 * @created: 2015/3/30
 * @author: nyankosama
 * @description:
 */
public interface Mapper<InKey, Inval, OutKey, OutVal> {
    public void map(InKey key, Inval val, OutputCollector<OutKey, OutVal> collector) throws IOException;
}
