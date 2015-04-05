package com.nyankosama.mapred.worker;


import com.nyankosama.base.ResultSet;

import java.util.List;

/**
 * @created: 2015/3/30
 * @author: nyankosama
 * @description:
 */
public interface Worker {
    public ResultSet<Boolean, Throwable> applyJobs(List<MapredJob> jobs);

    public ResultSet<Boolean, Throwable> ping();

    /**
     * 具体实现：
     * 在Reduce读取文件进行过程中，通过某个flag来标记正在读取的Worker，当异步调用notifyMigration时
     * 通过CAP操作来更新对应worker的flag为disable，这样使得thread-safe
     * worker用一个Queue来存储，标记完成后把newWorker push到队列的最后
     * @param failWorkerID
     * @param newWorkerID
     * @return
     */
    public ResultSet<Boolean, Throwable> notifyMigration(int failWorkerID, int newWorkerID);
}
