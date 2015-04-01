package com.nyankosama.mapred.worker;

import com.nyankosama.base.ResultSet;

import java.util.List;

/**
 * @created: 2015/4/1
 * @author: nyankosama
 * @description:
 */
public class WorkerHandler implements Worker {

    private List<WorkerInfo> workerInfos; // for reduce job
    private int stage; //reduce or map?


    @Override
    public ResultSet<Boolean, Throwable> applyJobs(List<MapredJob> jobs) {
        return null;
    }

    @Override
    public ResultSet<Boolean, Throwable> ping() {
        return null;
    }

    @Override
    public ResultSet<Boolean, Throwable> notifyMigration(int failWorkerID, int newWorkerID) {
        return null;
    }

    private void doMapJob() {
        /**
         * TODO
         * - read lines
         * - do map function
         * - combination part
         * - partition part
         * - write to local filesystem
         */
    }

    private void doReduceJob() {
        /**
         * TODO
         * - read lines from many map workers
         * - sort the key
         * - do reduce function and write to global file system
          */
    }
}
