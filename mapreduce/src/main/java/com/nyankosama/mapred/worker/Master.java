package com.nyankosama.mapred.worker;

import com.nyankosama.base.ResultSet;

import java.util.List;

/**
 * @created: 2015/3/30
 * @author: nyankosama
 * @description:
 */
public interface Master {
    public ResultSet<Boolean, Throwable> replyJobResults(List<MapredJob> jobs);
}
