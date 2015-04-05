package com.nyankosama.mapred.base.io;

import com.nyankosama.base.ResultSet;
import com.nyankosama.mapred.base.Writer;

/**
 * @created: 2015/4/1
 * @author: nyankosama
 * @description:
 */
public class SocketWriter implements Writer{
    @Override
    public ResultSet<Boolean, Throwable> writeLine(String line) {
        return null;
    }
}
