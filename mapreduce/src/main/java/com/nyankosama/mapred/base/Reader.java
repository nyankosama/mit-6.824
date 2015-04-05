package com.nyankosama.mapred.base;

import com.nyankosama.base.ResultSet;

/**
 * @created: 2015/4/1
 * @author: nyankosama
 * @description:
 */
public interface Reader {
    public ResultSet<String, Throwable> readLine();
}
