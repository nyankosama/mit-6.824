package com.nyankosama.mapred.base;

import com.nyankosama.base.ResultSet;

/**
 * @created: 2015/4/1
 * @author: nyankosama
 * @description:
 */
public interface Writer {
    public ResultSet<Boolean, Throwable> writeLine(String line);
}
